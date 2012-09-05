package jobs;

import models.Notification;
import play.jobs.Job;

import java.util.List;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: sheldon
 * Date: 9/4/12
 * Time: 3:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class NotificationByPushJob extends Job {
    private static Vector<Notification> notifications = new Vector<Notification>();

    /**
     * Sends notifications in the queue by push notification
     */
    public void doJob() {
        for (int i = 0; i < notifications.size(); i++) {
            // send push notification
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
