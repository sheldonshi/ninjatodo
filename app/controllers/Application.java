package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

@With(LanguageController.class)
public class Application extends Controller {

    public static void index() {
        Project project = Project.findById(1L);
        render(project);
    }

}