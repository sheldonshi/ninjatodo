package controllers;

import models.Project;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Created by IntelliJ IDEA.
 * User: root
 * Date: 4/15/12
 * Time: 7:02 PM
 * To change this template use File | Settings | File Templates.
 */
@With(LanguageController.class)
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
}
