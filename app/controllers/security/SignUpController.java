package controllers.security;

import controllers.securesocial.SecureSocial;
import jobs.NotificationJob;
import models.*;
import notifiers.securesocial.Mails;
import play.Logger;
import play.data.validation.*;
import play.i18n.Messages;
import play.libs.Crypto;
import play.mvc.Controller;
import play.mvc.Router;
import securesocial.provider.*;

/**
 * Created by IntelliJ IDEA.
 * User: sheldon
 * Date: 5/17/12
 * Time: 4:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class SignUpController extends Controller {
    public static final String EMAIL = "email";
    public static final String INVITATION_UUID = "invitationUuid";
    public static final String EMAIL_ALREADY_IN_USE = "signup_emailAlreadyInUse";

    private static final String USER_NAME = "userName";
    private static final String SECURESOCIAL_USER_NAME_TAKEN = "securesocial.userNameTaken";
    private static final String SECURESOCIAL_ERROR_CREATING_ACCOUNT = "securesocial.errorCreatingAccount";
    private static final String SECURESOCIAL_ACCOUNT_CREATED = "signup_accountCreated";
    private static final String SECURESOCIAL_ACTIVATION_TITLE = "securesocial.activationTitle";
    private static final String SECURESOCIAL_SECURE_SOCIAL_NOTICE_PAGE_HTML = "security/SignUpController/noticePage.html";
    private static final String PASSWORD = "password";
    private static final String DISPLAY_NAME = "displayName";
    private static final String ORIGINAL_URL = "originalUrl";

    public static void signup() {
        render();
    }

    /**
     * checks whether username has not been used
     * @param username
     */
    public static void isUnique(@Required @MinSize(3) @MaxSize(20) String username) {
        if (validation.hasErrors()) {
            renderText("");
        } else {
            if (username.indexOf("@") != -1 || username.indexOf(",") != -1 || username.indexOf("\\uFF03") != -1 || username.indexOf(" ") != -1) {
                renderText("1");
            } else if (isUsernameUnique(username)) {
                renderText("0");
            } else  {
                renderText("1");
            }
        }
        
    }

    private static boolean isUsernameUnique(String username) {
        return User.find("byUsername", username).first() == null;
    }

    private static boolean isEmailUnique(String email) {
        return User.find("byEmail", email).first() == null;
    }

    /**
     * Creates an account. An account can be created by (1) self-sign up, email must be verified
     * (2) invited signup. email doesn't need to be verified if it is the same as the email address 
     * that the invitation was sent to.
     *
     * @param userName      The username
     * @param displayName   The user's full name
     * @param email         The email
     * @param password      The password      
     * @param invitationUuid          Optional uuid that comes from an invited link
     */
    public static void createAccount(@Required @MinSize(3) String userName,
                                     @Required String displayName,
                                     @Required @Email String email,
                                     @Match(value="^.*(?=.{6,})(?=.*\\d)(?=.*[a-zA-Z]).*$", message="signup_passwordPattern") String password,
                                     String invitationUuid) {
        if ( validation.hasErrors() ) {
            tryAgain(userName, displayName, email, password, invitationUuid);
        }
        UserId id = new UserId();
        id.id = userName;
        id.provider = ProviderType.userpass;

        if (!isUsernameUnique(userName)) {
            validation.addError(USER_NAME, Messages.get(SECURESOCIAL_USER_NAME_TAKEN));
            tryAgain(userName, displayName, email, password, invitationUuid);
        }
        if (!isEmailUnique(email)) {
            validation.addError(EMAIL, Messages.get(EMAIL_ALREADY_IN_USE));
            tryAgain(userName, displayName, email, password, invitationUuid);
        }
        SocialUser socialUser = new SocialUser();
        socialUser.id = id;
        socialUser.displayName = displayName;
        socialUser.email = email;
        socialUser.password = Crypto.passwordHash(password);
        // the user will remain inactive until the email verification is done.
        socialUser.isEmailVerified = false;
        socialUser.authMethod = AuthenticationMethod.USER_PASSWORD;
        // check whether this is a sign up by invite
        Invitation invitation = null;
        if (invitationUuid != null) {
            invitation = Invitation.find("byUuid", invitationUuid).first();
            // compare email address to see whether email verification is required
            if (invitation != null && invitation.toEmail.equalsIgnoreCase(socialUser.email)) {
                socialUser.isEmailVerified = true;
            }
        }

        try {
            UserService.save(socialUser);
            if (invitation != null) {
                // assign the user to the project member if the signup is from a project invite
                if (invitation.project != null && invitation.role != null) {
                    User user = User.loadBySocialUser(socialUser);
                    if (user != null) {
                        Participation participation = new Participation();
                        participation.project = invitation.project;
                        participation.user = user;
                        participation.role = invitation.role;
                        participation.save();
                    }
                }
                // remove invitation because it has been used and cannot be used again
                invitation.delete();
                // send notification
                User user = User.loadBySocialUser(SecureSocial.getCurrentUser());
                NotificationJob.queueNotification(Notification
                        .createOnJoinByInvite(user, invitation.fromUser, invitation.project));

            }
        } catch ( Throwable e ) {
            Logger.error(e, "Error while invoking UserService.save()");
            flash.error(Messages.get(SECURESOCIAL_ERROR_CREATING_ACCOUNT));
            tryAgain(userName, displayName, email, password, invitationUuid);
        }

        // create an activation id
        if (!socialUser.isEmailVerified) {
            final String uuid = UserService.createActivation(socialUser);
            sendActivationEmail(socialUser, invitationUuid, password);
            final String title = "";//Messages.get(SECURESOCIAL_ACTIVATION_TITLE, socialUser.displayName);
            render(SECURESOCIAL_SECURE_SOCIAL_NOTICE_PAGE_HTML, title);
        } else {
            // already email verified, should login and go to project directly
            SecureSocial.authenticate(ProviderType.userpass);
        }
    }

    /**
     * resend activation email
     */
    public static void resendActivation() {
        if (flash.get(USER_NAME) != null) {
            UserId socialUserId = new UserId();
            socialUserId.id = flash.get(USER_NAME);
            socialUserId.provider = ProviderType.userpass;
            User user = User.loadBySocialUserId(socialUserId);
            if (user != null) {
                sendActivationEmail(user.unpack(), flash.get(INVITATION_UUID), flash.get(PASSWORD));
                final String title = "";//Messages.get(SECURESOCIAL_ACTIVATION_TITLE, user.fullName);
                render(SECURESOCIAL_SECURE_SOCIAL_NOTICE_PAGE_HTML, title);
            }
        }
        signup();
    }

    /**
     * sends an activation email, and store key form values in flash, so that user can sign
     * up using a different email without re-enter other info
     * @param socialUser
     * @param invitationUuid
     */
    private static void sendActivationEmail(SocialUser socialUser, String invitationUuid, String password) {
        Mails.sendActivationEmail(socialUser, UserService.createActivation(socialUser));
        flash.success(Messages.get(SECURESOCIAL_ACCOUNT_CREATED, socialUser.email));
        // for signing up using a different email
        flash.put(USER_NAME, socialUser.id.id);
        flash.put(DISPLAY_NAME, socialUser.displayName);
        flash.put(INVITATION_UUID, invitationUuid);
        flash.put(PASSWORD, password);
        validation.keep();
    }

    /**
     * single click facebook authenticated join by invite
     *
     * @param code
     */
    public static void joinByFacebook(@Required String code) {
        flash.put(ORIGINAL_URL, "/auth/joinByInvite?code=" + code);
        redirect("/auth/facebook");
    }

    public static void join(@Required String code) {
        Invitation invitation = Invitation.find("byUuid", code).first();
        User existingUser = null;
        if (invitation != null) {
            flash.remove(ORIGINAL_URL);  // if it comes from another redirect, remove further
            if (SecureSocial.getCurrentUser() != null) {
                existingUser = User.loadBySocialUser(SecureSocial.getCurrentUser());
                if (invitation.project != null) {
                    // check existing first
                    Participation participation = Participation.find("project=? and user=?",
                            invitation.project, existingUser).first();
                    if (participation == null) {
                        participation = new Participation();
                        participation.project = invitation.project;
                        participation.role = invitation.role;
                        participation.user = existingUser;
                        participation.save();
                    }
                    invitation.delete();
                    redirect("/w/" + invitation.project.id);
                } else {
                    redirect("/");
                }
            } else {
                flash.put(SignUpController.EMAIL, invitation.toEmail);
                flash.put(SignUpController.INVITATION_UUID, code);
            }
        }
        signup();
    }

    private static void tryAgain(String username, String displayName, String email, 
                                 String password, String invitationUuid) {
        flash.put(USER_NAME, username);
        flash.put(DISPLAY_NAME, displayName);
        flash.put(EMAIL, email);
        flash.put(PASSWORD, password);
        flash.put(INVITATION_UUID, invitationUuid);
        validation.keep();
        signup();
    }
    
}
