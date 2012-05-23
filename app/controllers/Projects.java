package controllers;

import controllers.securesocial.SecureSocial;
import models.Participation;
import models.Project;
import models.Role;
import models.User;
import play.db.jpa.JPA;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;
import securesocial.provider.SocialUser;
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

    public static void edit(Long id) {
        Project project = Project.findById(id);
        render(project);
    }
    
    public static void save(Long id, String title) {
        Project project = Project.findById(id);
        project.title = title;
        project.save();
        renderText("{\"saved\":\"true\"}");
    }

    public static void add(String title) {
        SocialUser socialUser = SecureSocial.getCurrentUser();
        User user = User.find("byUsername", socialUser.id.id).first();
        addProject(user, title);
        // return the whole list to refresh UI
        renderText(Utils.toJson(getProjects(user)));
    }
    
    static List<Project> getProjects(User user) {
        List<Project> projects = JPA.em()
                .createQuery("select proj from Participation p left join p.project proj where p.user=:user order by proj.lastUpdated desc")
                .setParameter("user", user).getResultList();
        return projects;
    }

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
