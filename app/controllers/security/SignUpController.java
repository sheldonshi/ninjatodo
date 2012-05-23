package controllers.security;

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

    private static final String USER_NAME = "userName";
    private static final String SECURESOCIAL_USER_NAME_TAKEN = "securesocial.userNameTaken";
    private static final String SECURESOCIAL_ERROR_CREATING_ACCOUNT = "securesocial.errorCreatingAccount";
    private static final String SECURESOCIAL_ACCOUNT_CREATED = "securesocial.accountCreated";
    private static final String SECURESOCIAL_ACTIVATION_TITLE = "securesocial.activationTitle";
    private static final String SECURESOCIAL_SECURE_SOCIAL_NOTICE_PAGE_HTML = "securesocial/SecureSocial/noticePage.html";
    private static final String DISPLAY_NAME = "displayName";
    private static final String EMAIL = "email";
    private static final String PASSWORD = "password";
    private static final String SECURESOCIAL_INVALID_LINK = "securesocial.invalidLink";
    private static final String SECURESOCIAL_ACTIVATION_SUCCESS = "securesocial.activationSuccess";
    private static final String SECURESOCIAL_SECURE_SOCIAL_LOGIN = "securesocial.SecureSocial.login";
    private static final String SECURESOCIAL_ACTIVATE_TITLE = "securesocial.activateTitle";

    public static void signup() {
        render();
    }
    
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

    /**
     * Creates an account
     *
     * @param userName      The username
     * @param displayName   The user's full name
     * @param email         The email
     * @param password      The password
     */
    public static void createAccount(@Required @MinSize(3) String userName,
                                     @Required String displayName,
                                     @Required @Email String email,
                                     @Match(value="^.*(?=.{6,})(?=.*\\d)(?=.*[a-zA-Z]).*$", message="signup_passwordPattern") String password) {
        if ( validation.hasErrors() ) {
            tryAgain(userName, displayName, email, password);
        }
        UserId id = new UserId();
        id.id = userName;
        id.provider = ProviderType.userpass;

        if (!isUsernameUnique(userName)) {
            validation.addError(USER_NAME, Messages.get(SECURESOCIAL_USER_NAME_TAKEN));
            tryAgain(userName, displayName, email, password);
        }
        SocialUser user = new SocialUser();
        user.id = id;
        user.displayName = displayName;
        user.email = email;
        user.password = Crypto.passwordHash(password);
        // the user will remain inactive until the email verification is done.
        user.isEmailVerified = false;
        user.authMethod = AuthenticationMethod.USER_PASSWORD;

        try {
            UserService.save(user);
        } catch ( Throwable e ) {
            Logger.error(e, "Error while invoking UserService.save()");
            flash.error(Messages.get(SECURESOCIAL_ERROR_CREATING_ACCOUNT));
            tryAgain(userName, displayName, email, password);
        }

        // create an activation id
        final String uuid = UserService.createActivation(user);
        Mails.sendActivationEmail(user, uuid);
        flash.success(Messages.get(SECURESOCIAL_ACCOUNT_CREATED));
        final String title = Messages.get(SECURESOCIAL_ACTIVATION_TITLE, user.displayName);
        render(SECURESOCIAL_SECURE_SOCIAL_NOTICE_PAGE_HTML, title);
    }

    private static void tryAgain(String username, String displayName, String email, String password) {
        flash.put(USER_NAME, username);
        flash.put(DISPLAY_NAME, displayName);
        flash.put(EMAIL, email);
        flash.put(PASSWORD, password);
        validation.keep();
        signup();
    }
    
}
