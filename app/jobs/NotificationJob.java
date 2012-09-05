package jobs;

import models.*;
import play.db.jpa.JPA;
import play.db.jpa.NoTransaction;
import play.jobs.Every;
import play.jobs.Job;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Vector;

/**
 * This Job is always invoked by .now().
 */
public class NotificationJob extends Job {
    private static Vector<Notification> notifications = new Vector<Notification>();

    /**
     * Insert notifications in the queue into database
     */
    public void doJob() {
        Vector<Notification> notificationsByEmail = new Vector<Notification>();
        Vector<Notification> notificationsByPush = new Vector<Notification>();
        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = notifications.get(i);
            notification.save();
            // check for needs to send email or push notification
            if (notification.recipient.watchByEmail) {
                notificationsByEmail.add(notification);
            }
            if (notification.recipient.watchByPush && notification.recipient.apnToken != null) {
                notificationsByPush.add(notification);
            }
        }
        if (!notificationsByEmail.isEmpty()) {
            NotificationByEmailJob.queueNotification(notificationsByEmail);
        }
        if (!notificationsByPush.isEmpty()) {
            NotificationByPushJob.queueNotification(notificationsByPush);
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
            new NotificationJob().now();
        }
    }
}
