package models;

import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: super
 * Date: 3/17/12
 * Time: 3:09 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name="project")
public class Project extends Model {
    @Required
    @MaxSize(100)
    @Column(name = "title", nullable = false)
    public String title;

    @Required
    @ManyToOne
    @JoinColumn(name="creator_id", nullable = false)
    public User creator;

    @Required
    @ManyToOne
    @JoinColumn(name="administrator_id", nullable = false)
    public User administrator;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_created", nullable = true)
    public Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_updated", nullable = true)
    public Date lastUpdated;

    public Project() {
        dateCreated = new Date();
        lastUpdated = dateCreated;
    }
}
