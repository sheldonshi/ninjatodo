package services;

import models.Invitation;
import models.Project;
import models.User;
import play.Play;
import play.db.jpa.JPA;
import play.exceptions.MailException;
import play.i18n.Messages;
import play.libs.Codec;
import play.mvc.Mailer;
import play.mvc.Router;
import securesocial.provider.SocialUser;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: sheldon
 * Date: 5/24/12
 * Time: 6:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class MailService extends Mailer {
    private static final String SECURESOCIAL_MAILER_FROM = "securesocial.mailer.from";
    private static final String SECURESOCIAL_USERNAME_PASSWORD_CONTROLLER_ACTIVATE = "securesocial.UsernamePasswordController.activate";
    private static final String UUID = "uuid";
    private static final String PROJECT = "project";
    private static final String USER = "user";

    public static List<String> sendInvitationEmail(String[] emails, Project project, User fromUser) {
        List<String> sendList = new ArrayList<String>();
        
        //setSubject(Messages.get("mail_invitation_subject"));
        //setFrom(Play.configuration.getProperty(SECURESOCIAL_MAILER_FROM));
        for (String email : emails) {
            Date cutoffDate = new Date(System.currentTimeMillis() - 86400000L);
            email = email.trim(); // remove white spaces
            List<Invitation> invitations = JPA.em().createNamedQuery("Invitation.findExistingRecent")
                    .setParameter("project", project)
                    .setParameter("fromUser", fromUser)
                    .setParameter("cutoffDate", cutoffDate)
                    .setParameter("toEmail", email)
                    .getResultList();
            if (invitations.isEmpty()) {
                // only send email if has not send the same email recently
                try {

                    //addRecipient(email);
                    String uuid = Codec.UUID();

                    Map<String, Object> args = new HashMap<String, Object>();
                    args.put(UUID, uuid);
                    args.put(USER, fromUser);
                    args.put(PROJECT, project);
                    String activationUrl = Router.getFullUrl(SECURESOCIAL_USERNAME_PASSWORD_CONTROLLER_ACTIVATE, args);
                    //send("template name", fromUser, activationUrl);

                    Invitation invitation = new Invitation();
                    invitation.fromUser = fromUser;
                    invitation.project = project;
                    invitation.toEmail = email;
                    invitation.uuid = uuid;
                    invitation.save();
                    sendList.add(email);
                } catch (MailException me) {
                    // do nothing
                }
            }
        }
        return sendList;
    }
}
