package controllers;

import controllers.securesocial.SecureSocial;
import controllers.security.SignUpController;
import jobs.NotificationJob;
import models.*;
import notifiers.Mails;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.libs.Codec;
import play.mvc.*;
import services.SecureUserService;
import utils.Utils;

import java.util.ArrayList;
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
    private static final String INVITED_CODE = "code";
    private static final String INVITATIONS_JOIN = "security.SignUpController.join";

    @Before
    static void checkAccess() throws Throwable {
        Projects.checkOwnership();
    }
    /**
     * gets all pending acceptance invitations for a project
     * @param projectId
     */
    public static void index(Long projectId) {
        Project proj = Project.findById(projectId);
        renderText(Utils.toJson(getInvitations(proj)));
    }

    /**
     * Invites someone to join a project by email
     * @param emails
     * @param projectId
     */
    public static void invite(@Required String emails, @Required Long projectId, Role role) {
        String[] emailArray = emails.split(",");
        User user = SecureUserService.getCurrentUser();
        Project project = Project.findById(projectId);
        // send email to all recipients
        List<String> sendList = new ArrayList<String>();
        for (String email : emailArray) {
            email = email.trim(); // remove white spaces

            String uuid = Codec.UUID();
            Map<String, Object> args = new HashMap<String, Object>();
            args.put(INVITED_CODE, uuid);
            String activationUrl = Router.getFullUrl(INVITATIONS_JOIN, args);

            // check whether this email is already a registered user
            User existingUser = User.find("email=?", email).first();
            if (existingUser != null) {
                // send notification
                NotificationJob.queueNotification(Notification.createOnInvite(existingUser, user, project, activationUrl));
            }
            // then check whether email is valid
            if (validation.email(email).error == null &&
                    Mails.sendInvitationEmail(email, project, user, role, uuid, activationUrl)) {
                sendList.add(email);
            }
        }
        renderText(Utils.toJson(getInvitations(project)));
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
     * delete an invitation (revoke). Can only delete an invitation with a project != null
     * @param id
     */
    public static void delete(@Required Long id, @Required Long projectId) {
        Invitation invitation = Invitation.findById(id);
        if (invitation.project != null && invitation.project.id == projectId) {
            invitation.delete();
            Map returnMap = new HashMap();
            returnMap.put("id", id);
            renderJSON(returnMap);
        } else {
            // do nothing
        }
    }
}
