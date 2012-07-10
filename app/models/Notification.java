package models;

import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.Model;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: sheldon
 * Date: 7/6/12
 * Time: 10:36 AM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name="notification")
public class Notification extends Model {

    @Required
    @MaxSize(400)
    @Column(name = "message", nullable = false)
    public String message;

    @Required
    @ManyToOne
    @JoinColumn(name="recipient_id", nullable = false)
    public User recipient;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_created", nullable = true)
    public Date dateCreated;

    @Column(name = "goto_url", nullable = true)
    public String gotoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    public NotificationType notificationType = null;
    
    public Notification() {
        dateCreated = new Date();
    }
}
