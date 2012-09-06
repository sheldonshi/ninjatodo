package controllers;

import controllers.securesocial.SecureSocial;
import models.User;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Created by IntelliJ IDEA.
 * User: sheldon
 * Date: 9/5/12
 * Time: 6:27 PM
 * To change this template use File | Settings | File Templates.
 */
@With(SecureSocial.class)
public class Users extends Controller {

    /**
     * prepares for the edit profile screen
     */
    public static void edit() {
        User user = User.loadBySocialUser(SecureSocial.getCurrentUser());
        render(user);
    }

    /**
     * prepares for the edit profile screen
     */
    public static void save() {
        User user = User.loadBySocialUser(SecureSocial.getCurrentUser());
        renderText("{\"saved\":\"true\"}");
    }

}
