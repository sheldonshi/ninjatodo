package models;

import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: sheldon
 * Date: 7/6/12
 * Time: 10:36 AM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name="notification")
public class Notification extends Model {

    @Required
    @MaxSize(400)
    @Column(name = "message", nullable = false)
    public String message;

    @Required
    @ManyToOne
    @JoinColumn(name="recipient_id", nullable = false)
    public User recipient;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_created", nullable = true)
    public Date dateCreated;

    @Column(name = "goto_url", nullable = true)
    public String gotoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    public NotificationType notificationType = null;
    
    public Notification() {
        dateCreated = new Date();
    }

    /**
     * create notifications on list change
     * @param notificationType
     * @param toDoList
     * @param initiator
     * @param oldListNameOnRename
     * @return
     */
    public static List<Notification> createOnListAction(NotificationType notificationType,
                                                  ToDoList toDoList, User initiator, String oldListNameOnRename) {

        // gets all the subscribers, except the user who initiate the notification action
        List<User> users = null;
        if (NotificationType.ADD_LIST.equals(notificationType)) {
            users = JPA.em()
                    .createQuery("select p.user from Participation p where p.project=:project and p.user<>:user")
                    .setParameter("project", toDoList.project)
                    .setParameter("user", initiator)
                    .getResultList();
        } else {
            users = JPA.em()
                    .createQuery("select u from User u left join u.watchedToDoLists l where l=:toDoList and u<>:user")
                    .setParameter("toDoList", toDoList)
                    .setParameter("user", initiator)
                    .getResultList();
        }
        if (users != null && users.size() > 0) {
            String message = null;
            switch (notificationType) {
                case ADD_LIST:
                    message = initiator.fullName + " created a new list \"" + toDoList.name + "\".";
                    break;
                case DELETE_LIST:
                    message = initiator.fullName + " deleted list \"" + toDoList.name + "\".";
                    break;
                case RENAME_LIST:
                    message = initiator.fullName + " renamed list \"" + oldListNameOnRename + "\" to \"" + toDoList.name + "\".";
                    break;
                case CLEAR_LIST:
                    message = initiator.fullName + " cleared all completed tasks in list \"" + toDoList.name + "\".";
                    break;
                default:
                    throw new RuntimeException("notification type " + notificationType + " is not supported");
            }
            return createNotifications(notificationType, users, message);
        } else {
            return null;
        }
    }
    /**
     * create notifications on task change
     * @param notificationType
     * @param toDo
     * @param initiator
     * @param oldToDoList
     * @return
     */
    public static List<Notification> createOnTaskAction(NotificationType notificationType,
                                                        ToDo toDo, User initiator, ToDoList oldToDoList) {

        // gets all the subscribers, except the user who initiate the notification action
        List<User> users = JPA.em()
                    .createQuery("select u from User u left join u.watchedToDoLists l where l=:toDoList and u<>:user")
                    .setParameter("toDoList", toDo.toDoList)
                    .setParameter("user", initiator)
                    .getResultList();
        
        if (users != null && users.size() > 0) {
            String message = null;
            switch (notificationType) {
                case ASSIGN_TASK:
                    message = initiator.fullName + " added a new task \"" + toDo.title + "\" to list \"" + toDo.toDoList.name + "\"";
                    break;
                case REMOVE_TASK:
                    message = initiator.fullName + " removed task \"" + toDo.title + "\" from list \"" + oldToDoList.name + "\"";
                    break;
                case UPDATE_TASK:
                    message = initiator.fullName + " updated task \"" + toDo.title + "\" in list \"" + toDo.toDoList.name + "\".";
                    break;
                case COMPLETE_TASK:
                    message = initiator.fullName + " mark task \"" + toDo.title + "\" completed.";
                    break;
                case CHANGE_PRIORITY:
                    message = initiator.fullName + " changed the priority of task \"" + toDo.title + "\".";
                    break;
            }
            return createNotifications(notificationType, users, message);
        } else {
            return null;
        }
    }

    /**
     * creates notification on join by invite
     * @param invitee
     * @param inviter
     * @return
     */
    public static List<Notification> createOnJoinByInvite(User invitee, User inviter) {
        String message = invitee.fullName + " accepted your invitation to join.";
        List<User> recipients = new ArrayList<User>();
        recipients.add(inviter);
        return createNotifications(NotificationType.JOIN_BY_INVITE, recipients, message);
    }

    /**
     * creates one type of notifications for a list of recipients
     * @param notificationType
     * @param recipients
     * @param message
     * @return
     */
    private static List<Notification> createNotifications(NotificationType notificationType, List<User> recipients, String message) {
        List<Notification> notifications = new ArrayList<Notification> ();
        for (User recipient : recipients) {
            Notification notification = new Notification();
            notification.message = message;
            notification.notificationType = notificationType;
            notification.recipient = recipient;
            notifications.add(notification);
        }
        return notifications;
    }
}
