package controllers;

import controllers.securesocial.SecureSocial;
import models.*;
import play.Play;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Router;
import play.mvc.With;
import securesocial.provider.SocialUser;
import services.MailService;
import services.SecureUserService;
import utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: root
 * Date: 4/15/12
 * Time: 7:02 PM
 * To change this template use File | Settings | File Templates.
 */
@With( SecureSocial.class )
public class Projects extends Controller {

    public static void edit(Long id) {
        Project project = Project.findById(id);
        List<Participation> participations = Participation.find("byProject", project).fetch();
        render(project, participations);
    }
    
    public static void save(Long id, String title, Visibility visibility) {
        Project project = Project.findById(id);
        project.title = title;
        project.visibility = visibility;
        project.save();
        renderText("{\"saved\":\"true\"}");
    }

    public static void add(String title) {
        User user = SecureUserService.getCurrentUser();
        addProject(user, title);
        // return the whole list to refresh UI
        renderText(Utils.toJson(getProjects(user)));
    }

    /**
     * promotes users to admins of a project
     * @param participationIds
     * @param projectId
     */
    public static void promoteToAdmin(Long[] participationIds, Long projectId) {
        Project project = Project.findById(projectId);
        if (project != null) {
            for (Long participationId : participationIds) {
                Participation participation = Participation.findById(participationId);
                if (participation != null && participation.role.equals(Role.USER) && participation.project.equals(project)) {
                    participation.role = Role.ADMIN;
                    participation.save();
                }
            }
            List<Participation> participations = Participation.find("byProject", project).fetch();
            renderTemplate("/Projects/administrators.html", project, participations);
        }
    }

    public static void deleteAdmin(Long participationId) {
        Participation participation = Participation.findById(participationId);
        // TODO check permission
        if (participation != null && participation.role.equals(Role.ADMIN)) {
            participation.role = Role.USER;
            participation.save();
            renderText(participationId);
        } else {
            renderText("");
        }
    }

    public static void deleteMember(Long participationId) {
        Participation participation = Participation.findById(participationId);
        // TODO check permission
        if (participation != null) {
            participation.delete();
            renderText(participationId);
        } else {
            renderText("");
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
                .setParameter("project", project).setParameter("role", Role.ADMIN).getResultList();
        return participations;
    }

    /**
     * Gets all projects this user participates
     * @param user
     * @return
     */
    static List<Project> getProjects(User user) {
        List<Project> projects = JPA.em()
                .createQuery("select proj from Participation p left join p.project proj where p.user=:user order by proj.dateCreated desc")
                .setParameter("user", user).getResultList();
        return projects;
    }

    /**
     * Add a new project to a user
     *
     * @param user
     * @param title
     * @return
     */
    static Project addProject(User user, String title) {

        // create default project if no project is available
        Project project = new Project();
        project.creator = user;
        project.title = title;
        project.save();
        Participation participation = new Participation();
        participation.project = project;
        participation.user = user;
        participation.role = Role.ADMIN;
        participation.save();
        return project;
    }
}
