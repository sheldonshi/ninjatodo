package models;

import json.JsonExclude;
import play.data.validation.Required;
import play.db.jpa.Model;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: sheldon
 * Date: 5/25/12
 * Time: 11:36 AM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name="invitation")
@NamedQueries({
        @NamedQuery(name="Invitation.findExistingRecent",
                query="SELECT i FROM Invitation i where i.project=:project and i.fromUser=:fromUser and i.dateCreated>:cutoffDate and i.toEmail=:toEmail"),
        @NamedQuery(name="Invitation.findExistingAll",
                query="SELECT distinct i FROM Invitation i where i.project=:project order by i.dateCreated desc")
})
public class Invitation extends Model {

    @ManyToOne
    @JoinColumn(name="project_id", nullable = true)
    @JsonExclude
    public Project project;

    @Required
    @ManyToOne
    @JoinColumn(name="from_user_id", nullable = false)
    @JsonExclude
    public User fromUser;

    @Required
    @Column(name="to_email", nullable = false)
    public String toEmail;

    @Required
    @Column(name="uuid", nullable = false)
    @JsonExclude
    public String uuid;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_created", nullable = true)
    public Date dateCreated;

    @Column(name="role", nullable = true)
    @Enumerated(EnumType.STRING)
    public Role role;

    public Invitation() {
        dateCreated = new Date();
    }
}
