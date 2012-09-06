package models;

/**
 * Created by IntelliJ IDEA.
 * User: sheldon
 * Date: 7/6/12
 * Time: 10:45 AM
 * To change this template use File | Settings | File Templates.
 */
public enum NotificationType {
    // JOIN_BY_INVITE: notify when someone joins from my invitation
    // INVITE_IN_PROJECT: notify when someone invites another user to join projects
    ASSIGN_TASK, REMOVE_TASK, UPDATE_TASK, COMPLETE_TASK, CHANGE_PRIORITY, ADD_LIST, DELETE_LIST, RENAME_LIST, CLEAR_LIST, JOIN_BY_INVITE, INVITE_IN_PROJECT
}
