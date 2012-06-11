package controllers.security;

import controllers.securesocial.SecureSocial;
import models.Invitation;
import models.Participation;
import models.Role;
import models.User;
import notifiers.securesocial.Mails;
import play.Logger;
import play.data.validation.*;
import play.i18n.Messages;
import play.libs.Crypto;
import play.mvc.Controller;
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
    private static final String SECURESOCIAL_ACCOUNT_CREATED = "securesocial.accountCreated";
    private static final String SECURESOCIAL_ACTIVATION_TITLE = "securesocial.activationTitle";
    private static final String SECURESOCIAL_SECURE_SOCIAL_NOTICE_PAGE_HTML = "securesocial/SecureSocial/noticePage.html";
    private static final String PASSWORD = "password";
    private static final String DISPLAY_NAME = "displayName";

    public static void signup() {
        render();
    }

    /**
     * checks whether username has not been used
     * @param username
     */
    public static void isUnique(@Required @MinSize(3) String username) {
        if (validation.hasErrors()) {
            renderText("");
        } else {
            if (isUsernameUnique(username)) {
                renderText("0");
            } else {
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
            }
        } catch ( Throwable e ) {
            Logger.error(e, "Error while invoking UserService.save()");
            flash.error(Messages.get(SECURESOCIAL_ERROR_CREATING_ACCOUNT));
            tryAgain(userName, displayName, email, password, invitationUuid);
        }

        // create an activation id
        if (!socialUser.isEmailVerified) {
            final String uuid = UserService.createActivation(socialUser);
            Mails.sendActivationEmail(socialUser, uuid);
            flash.success(Messages.get(SECURESOCIAL_ACCOUNT_CREATED));
            final String title = Messages.get(SECURESOCIAL_ACTIVATION_TITLE, socialUser.displayName);
            render(SECURESOCIAL_SECURE_SOCIAL_NOTICE_PAGE_HTML, title);
        } else {
            // already email verified, should login and go to project directly
            SecureSocial.authenticate(ProviderType.userpass);
        }
    }

    public static void join(@Required String code) {
        Invitation invitation = Invitation.find("byUuid", code).first();
        if (invitation != null) {
            flash.put(SignUpController.EMAIL, invitation.toEmail);
            flash.put(SignUpController.INVITATION_UUID, code);
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
