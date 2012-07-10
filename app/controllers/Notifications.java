package controllers;

import controllers.securesocial.SecureSocial;
import models.Notification;
import models.User;
import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.With;
import utils.Utils;

import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: sheldon
 * Date: 7/8/12
 * Time: 9:01 PM
 * To change this template use File | Settings | File Templates.
 */
@With( SecureSocial.class )
public class Notifications extends Controller {
    /**
     * get all notifications
     */
    public static void check() {
        User user = User.loadBySocialUser(SecureSocial.getCurrentUser());
        List<Notification> notifications = Notification.find("recipient=?", user)
                .fetch();
        user.lastNotificationCheck = new Date();
        user.save();
        renderText(Utils.toJson(notifications));
    }
    /**
     * get all notification count since last viewed
     */
    public static void checkNewCount() {
        User user = User.loadBySocialUser(SecureSocial.getCurrentUser());
        int count = (int) Notification.count("recipient=? and dateCreated>?",
                user, (user.lastNotificationCheck != null ? user.lastNotificationCheck : new Date(0)));
        renderText(Utils.toJson(count));
    }

    /**
     * clear all my notifications
     */
    public static void clear() {
        User user = User.loadBySocialUser(SecureSocial.getCurrentUser());
        JPA.em().createQuery("delete from Notification n where n.recipient=:user")
                .setParameter("user", user)
                .executeUpdate();
        renderText(Utils.toJson(0));
    }
}
