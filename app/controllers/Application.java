package controllers;

import play.mvc.*;

import models.*;

@With(LanguageFilter.class)
public class Application extends Controller {

    public static void index() {
        Project project = Project.findById(1L);
        render(project);
    }

}