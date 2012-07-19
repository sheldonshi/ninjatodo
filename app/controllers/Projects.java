package controllers;

import controllers.securesocial.SecureSocial;
import models.*;
import play.db.jpa.JPA;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Scope;
import play.mvc.With;
import services.SecureUserService;
import utils.Utils;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: root
 * Date: 4/15/12
 * Time: 7:02 PM
 * To change this template use File | Settings | File Templates.
 */
@With( SecureSocial.class )
public class Projects extends Controller {

    /**
     * this checks whether a user has ownership role to a project
     *
     * @throws Throwable
     */
    @Before(unless={"add"})
    static void checkAccess() throws Throwable {
        checkOwnership();
    }

    /**
     * prepares for the edit setting screen
     * @param projectId
     */
    public static void edit(Long projectId) {
        Project project = Project.findById(projectId);
        List<Participation> participations = Participation.find("byProject", project).fetch();
        List<Invitation> invitations = Invitation.find("byProject", project).fetch();
        render(project, participations, invitations);
    }

    /**
     * save the edited settings
     *
     * @param projectId
     * @param title
     */
    public static void save(Long projectId, String title) {
        Project project = Project.findById(projectId);
        project.title = title;
        project.save();
        renderText("{\"saved\":\"true\"}");
    }

    public static void add(String title) {
        User user = SecureUserService.getCurrentUser();
        addProject(user, title);
        // return the whole list to refresh UI
        renderText(Utils.toJson(getParticipations(user)));
    }

    /**
     * promotes a user in a project
     * @param participationId
     * @param projectId
     */
    public static void promoteMember(Long participationId, Long projectId) {
        Project project = Project.findById(projectId);
        if (project != null) {
            Participation participation = Participation.findById(participationId);
            if (participation != null) {
                if (participation.role.equals(Role.READ) && participation.project.equals(project)) {
                    participation.role = Role.WRITE;
                }
                if (participation.role.equals(Role.WRITE) && participation.project.equals(project)) {
                    participation.role = Role.OWN;
                }
                participation.save();
            }
            renderText("");
        }
    }

    /**
     * demotes a user in a project
     * @param participationId
     * @param projectId
     */
    public static void demoteMember(Long participationId, Long projectId) {
        Project project = Project.findById(projectId);
        if (project != null) {
            Participation participation = Participation.findById(participationId);
            if (participation != null) {
                if (participation.role.equals(Role.WRITE) && participation.project.equals(project)) {
                    participation.role = Role.READ;
                }
                if (participation.role.equals(Role.OWN) && participation.project.equals(project)) {
                    if (Participation.count("project=? and role=?", project, Role.OWN) > 1) {
                        participation.role = Role.WRITE; // only allow demoting an owner when there are more than one owner
                    }
                }
                participation.save();
            }
            renderText("");
        }
    }

    /**
     * remove a user from a project
     * @param participationId
     * @param projectId
     */
    public static void deleteMember(Long participationId, Long projectId) {
        Participation participation = Participation.findById(participationId);
        // TODO check permission
        if (participation != null &&
                participation.project.id == projectId) {
            participation.delete();
            renderText(participationId);
        }
    }

    /**
     * Gets all projects this user participates
     * @param project
     * @return
     */
    static List<Participation> getAdministrators(Project project) {
        List<Participation> participations = JPA.em()
                .createQuery("select p from Participation p where p.project=:project and p.role=:role order by p.dateCreated")
                .setParameter("project", project).setParameter("role", Role.WRITE).getResultList();
        return participations;
    }

    /**
     * Gets all projects this user participates
     * @param user
     * @return
     */
    static List<Participation> getParticipations(User user) {
        List<Participation> participations = JPA.em()
                .createQuery("select p from Participation p left join p.project proj where p.user=:user order by proj.id desc")
                .setParameter("user", user).getResultList();
        return participations;
    }

    /**
     * Add a new project to a user
     *
     * @param user
     * @param title
     * @return
     */
    static Participation addProject(User user, String title) {

        // create default project if no project is available
        Project project = new Project();
        project.creator = user;
        project.title = title;
        project.save();
        Participation participation = new Participation();
        participation.project = project;
        participation.user = user;
        participation.role = Role.OWN;
        participation.save();
        return participation;
    }

    /**
     * Check whether the project has the user as a member
     */
    static void checkMembership() {
        if (isMember(getProject())) {
            return; // member of a non-public project
        } else {
            throw new RuntimeException();   // no permission
        }
    }

    /**
     * checks whether this user is an admin of the project
     * @throws Throwable
     */
    static void checkOwnership() {
        User user = User.loadBySocialUser(SecureSocial.getCurrentUser());
        String id = Scope.Params.current().get("projectId");
        Project project = Project.findById(Long.valueOf(id));
        if (Participation.find("project=? and user=? and role=?", project, user, Role.OWN).first() == null) {
            throw new RuntimeException();
        }
    }

    /**
     * Check whether the project is visible to the user
     */
    static void checkVisibility() {
        Project project = getProject();
        // check the visibility of the project
        if (project.visibility.equals(Visibility.PUBLIC)) {
            return; // public project
        } else if (project.visibility.equals(Visibility.LINK) && false) {
            // TODO deal with a private link
        } else if (isMember(project)) {
            return; // member of a non-public project
        } else {
            throw new RuntimeException();   // no permission
        }
    }

    /**
     * gets the Project from projectId or list id
     * @return
     */
    private static Project getProject() {
        String projectId = Scope.Params.current().get("projectId");
        String listId = Scope.Params.current().get("list");
        String toDoId = Scope.Params.current().get("taskId");
        // get the project first. if not available, get list. if not available get task
        Project project = null;
        if (projectId != null) {
            project = Project.findById(Long.valueOf(projectId));
        } else if (listId != null) {
            ToDoList toDoList = ToDoList.findById(Long.valueOf(listId));
            if (toDoList != null) {
                project = toDoList.project;
            }
        } else if (toDoId != null) {
            ToDo toDo = ToDo.findById(Long.valueOf(toDoId));
            if (toDo != null) {
                project = toDo.toDoList.project;
            }
        }
        return project;
    }

    private static boolean isMember(Project project) {
        User user = User.loadBySocialUser(SecureSocial.getCurrentUser());
        return Participation.find("project=? and user=?", project, user).first() != null;
    }
}
