package controllers;

import play.mvc.*;

import models.*;

public class Application extends Controller {

    public static void index() {
        Project project = Project.findById(1L);
        render(project);
    }

}