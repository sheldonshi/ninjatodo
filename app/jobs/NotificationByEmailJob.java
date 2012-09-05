package jobs;

import models.Notification;
import notifiers.Mails;
import play.jobs.Job;

import java.util.List;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: sheldon
 * Date: 9/4/12
 * Time: 3:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class NotificationByEmailJob extends Job {
    private static Vector<Notification> notifications = new Vector<Notification>();

    /**
     * Send notifications in the queue by email one by one
     */
    public void doJob() {
        for (int i = 0; i < notifications.size(); i++) {
            // send email
            Notification notification = notifications.get(i);
            Mails.sendNotificationEmail(notification.recipient, notification.message, notification.gotoUrl, notification.projectTitle);
        }
        notifications.clear();
    }

    /**
     * add notification to the queue
     *
     * @param moreNotifications
     */
    public static void queueNotification(List<Notification> moreNotifications) {
        // gets all the subscribers, except the user who initiate the notification action
        if (moreNotifications != null && !moreNotifications.isEmpty()) {
            notifications.addAll(moreNotifications);
            new NotificationByEmailJob().now();
        }
    }
}
