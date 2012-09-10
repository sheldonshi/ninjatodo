package controllers;

import controllers.securesocial.SecureSocial;
import models.Notification;
import models.User;
import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.With;
import utils.Utils;

import java.util.*;

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
        List<Notification> notifications = Notification.find("recipient=? order by id desc", user)
                .fetch();
        if (!notifications.isEmpty()) {
            // set the last checked notification id for this user, for future reference of new notification alert
            user.lastNotificationId = notifications.get(0).id;
        }
        user.save();
        renderText(Utils.toJson(notifications, true));
    }
    /**
     * get all notification count since last viewed. this returns the count and the last notification id in this count.
     * so that the UI can monitor newer notifications based ont he last notification id.
     */
    public static void checkNewCount() {
        User user = User.loadBySocialUser(SecureSocial.getCurrentUser());
        List<Notification> notifications =  Notification.find("recipient=? and id >? order by id desc",
                user, (user.lastNotificationId != null ? user.lastNotificationId : 0)).fetch();
        Long newNotificationId = user.lastNotificationId;
        if (!notifications.isEmpty()) {
            // set the last checked notification id for this user, for future reference of new notification alert
            newNotificationId = notifications.get(0).id;
        }
        Map jsonMap = new HashMap();
        jsonMap.put("count", notifications.size());
        jsonMap.put("notificationId", newNotificationId);
        renderText(Utils.toJson(jsonMap));
    }
    
    /**
     * get all notification count for a particular project since last refresh of the project.
     * this is a comet connection
     */
    public static void checkNewCountForProject(Long projectId, Long notificationId) {
        User user = User.loadBySocialUser(SecureSocial.getCurrentUser());
        List<Notification> notifications = new ArrayList<Notification>();
        int countForProject = 0;
        Long newNotificationId = notificationId;
        while (notifications.isEmpty()) {
            notifications = Notification.find("recipient=? and id >? order by id",
                user, notificationId).fetch();
            if (!notifications.isEmpty()) {
                break;
            }
            await(3000);  // wait for 3000 millis, suspend the request (for comet connection)
        } 
        // check which of the notifications are for this project
        for (Notification notification : notifications) {
            if (notification.projectId != null && notification.projectId.equals(projectId) && notification.id > notificationId) {
                countForProject ++;
            }
            newNotificationId = notification.id;
        }
        Map jsonMap = new HashMap();
        jsonMap.put("count", notifications.size());
        jsonMap.put("countForProject", countForProject);
        jsonMap.put("notificationId", newNotificationId);
        renderText(Utils.toJson(jsonMap));
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
