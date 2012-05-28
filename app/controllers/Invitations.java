package controllers;

import com.sun.corba.se.impl.logging.UtilSystemException;
import controllers.securesocial.SecureSocial;
import models.Invitation;
import models.Project;
import models.User;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.mvc.Controller;
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
 * User: sheldon
 * Date: 5/25/12
 * Time: 3:19 PM
 * To change this template use File | Settings | File Templates.
 */
@With(SecureSocial.class)
public class Invitations extends Controller {

    /**
     * gets all pending acceptance invitations for a project
     * @param project
     */
    public static void index(Long project) {
        Project proj = Project.findById(project);
        renderText(Utils.toJson(getInvitations(proj)));
    }

    /**
     * Invites someone to join a project by email
     * @param emails
     * @param project
     */
    public static void invite(@Required String emails, @Required Long project) {
        String[] emailArray = emails.split(",");
        User user = SecureUserService.getCurrentUser();
        Project proj = Project.findById(project);
        MailService.sendInvitationEmail(emailArray, proj, user);
        renderText(Utils.toJson(getInvitations(proj)));
    }

    /**
     * gets all invitations pending acceptance for a project
     * @param proj
     * @return
     */
    private static List<Invitation> getInvitations(Project proj) {
        return JPA.em().createNamedQuery("Invitation.findExistingAll")
                .setParameter("project", proj)
                .getResultList();
    }

    /**
     * delete an invitation (revoke)
     * @param id
     */
    public static void delete(@Required Long id) {
        Invitation invitation = Invitation.findById(id);
        invitation.delete();
        Map returnMap = new HashMap();
        returnMap.put("id", id);
        renderJSON(returnMap);
    }
}
