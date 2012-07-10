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
        for (int i = 0; i < notifications.size(); i++) {
            notifications.get(i).save();
        }
        notifications.clear();
    }

    /**
     * add notification to the queue
     *
     * @param notificationType
     * @param toDoList
     * @param user
     * @param message
     */
    public static void queueNotification(NotificationType notificationType,
                                         ToDoList toDoList, User user, String message) {
        // gets all the subscribers, except the user who initiate the notification action
        List<User> users = null;
        if (NotificationType.ADD_LIST.equals(notificationType)) {
            users = JPA.em()
                    .createQuery("select p.user from Participation p where p.project=:project and p.user<>:user")
                    .setParameter("project", toDoList.project)
                    .setParameter("user", user)
                    .getResultList();
        } else {
            users = JPA.em()
                    .createQuery("select u from User u left join u.watchedToDoLists l where l=:toDoList and u<>:user")
                    .setParameter("toDoList", toDoList)
                    .setParameter("user", user)
                    .getResultList();
        }
        if (users != null && users.size() > 0) {
            for (User recipient : users) {
                Notification notification = new Notification();
                notification.message = message;
                notification.notificationType = notificationType;
                notification.recipient = recipient;
                notification.gotoUrl = "/l/" + toDoList.id;
                notifications.add(notification);
            }
        }
        new NotificationJob().now();
    }
}
