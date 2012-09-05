package notifiers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import json.JsonExclude;
import models.Invitation;
import models.Project;
import models.Role;
import models.User;
import org.apache.commons.lang.StringUtils;
import play.Play;
import play.data.validation.Email;
import play.data.validation.Required;
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
public class Mails extends Mailer {
    private static final String SECURESOCIAL_MAILER_FROM = "securesocial.mailer.from";

    /**
     * Send invitation by email. Project could be null.
     * 
     * @param email
     * @param project - nullable
     * @param fromUser
     * @param role
     * @param uuid
     * @param activationUrl
     * @return
     */
    public static boolean sendInvitationEmail(String email, Project project, User fromUser, 
                                              Role role, String uuid, String activationUrl) {

        setSubject(Messages.get("mail_invitation_subject", fromUser.fullName, project.title));
        setFrom(Play.configuration.getProperty(SECURESOCIAL_MAILER_FROM));

        Date cutoffDate = new Date(System.currentTimeMillis() - 86400000L);
        List<Invitation> invitations = JPA.em().createNamedQuery("Invitation.findExistingRecent")
                .setParameter("project", project)
                .setParameter("fromUser", fromUser)
                .setParameter("cutoffDate", cutoffDate)
                .setParameter("toEmail", email)
                .getResultList();
        if (invitations.isEmpty()) {
            // only send email if has not send the same email recently
            try {

                addRecipient(email);

                send(fromUser, project, activationUrl);

                Invitation invitation = new Invitation();
                invitation.fromUser = fromUser;
                invitation.project = project;
                invitation.toEmail = email;
                invitation.uuid = uuid;
                invitation.role = role;
                invitation.save();
                return true;
            } catch (MailException me) {
                // do nothing
            }

        }
        return false;
    }

    /**
     * Send notification by email
     * 
     * @param recipient
     * @param message
     * @param projectTitle
     * @return
     */
    public static boolean sendNotificationEmail(User recipient, String message, String projectUrl, String projectTitle) {
        if (recipient == null || StringUtils.isEmpty(message) || StringUtils.isEmpty(projectTitle)) {
            return false;
        }
        setSubject(Messages.get("mail_notification_subject", recipient.fullName, projectTitle));
        setFrom(Play.configuration.getProperty(SECURESOCIAL_MAILER_FROM));
        
        if (StringUtils.isNotEmpty(recipient.email)) {
            try {

                addRecipient(recipient.email);
                JsonObject msgJson = new JsonParser().parse(message).getAsJsonObject();
                send(msgJson, projectUrl);

                return true;
            } catch (MailException me) {
                // do nothing
                me.printStackTrace();
            }

        }
        return false;
    }
}
