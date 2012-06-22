package controllers;

import controllers.securesocial.SecureSocial;
import play.cache.Cache;
import play.db.jpa.JPA;
import play.i18n.Messages;
import play.libs.Images;
import play.mvc.*;

import models.*;
import securesocial.provider.SocialUser;

import java.util.List;

@With( SecureSocial.class )
public class Application extends Controller {

    public static void index() {
        SocialUser socialUser = SecureSocial.getCurrentUser();
        User user = User.find("byUsername", socialUser.id.id).first();
        List<Participation> participations = Projects.getParticipations(user);
        if (participations.isEmpty()) {
            participations.add(Projects.addProject(user, Messages.get("myFirstProject")));
        }
        // now pick the selected participation
        Participation selectedParticipation = participations.get(0);
        ToDoList selectedToDoList = null;
        boolean authorized = false;
        if (params.get("projectId") != null) {
            for (Participation part : participations) {
                if (part.project.id.toString().equals(params.get("projectId"))) {
                    selectedParticipation = part;
                    authorized = true;
                    break;
                }
            }
        } else if (params.get("list") != null) {
            ToDoList toDoList = ToDoList.findById(Long.valueOf(params.get("list")));
            if (toDoList != null) {
                Project project = toDoList.project;
                for (Participation part : participations) {
                    if (part.project.equals(project)) {
                        selectedParticipation = part;
                        selectedToDoList = toDoList;
                        authorized = true;
                        break;
                    }
                }
            }
        } else {
            authorized = true;
        }
        if (authorized) {
            render(participations, selectedParticipation, selectedToDoList);
        } else {
            forbidden();
        }
    }

    /**
     * http://www.playframework.org/documentation/1.2.4/guide5
     *
     * @param id
     */
    public static void captcha(String id) {
        Images.Captcha captcha = Images.captcha();
        String code = captcha.getText("#E4EAFD");
        Cache.set(id, code, "5mn");
        renderBinary(captcha);
    }

}