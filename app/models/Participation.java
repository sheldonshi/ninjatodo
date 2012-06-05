package models;

import json.JsonExclude;
import play.data.validation.Required;
import play.db.jpa.Model;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: sheldon
 * Date: 5/19/12
 * Time: 2:41 PM
 * To change this template use File | Settings | File Templates.
 */

@Entity
@Table(name="participation")
public class Participation extends Model {
    @Required
    @ManyToOne
    @JoinColumn(name="project_id", nullable = false)
    public Project project;

    @Required
    @ManyToOne
    @JoinColumn(name="user_id", nullable = false)
    @JsonExclude
    public User user;

    @Required
    @Column(name="role", nullable = false)
    @Enumerated(EnumType.STRING)
    public Role role;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_created", nullable = true)
    public Date dateCreated;

    public Participation() {
        dateCreated = new Date();
    }
}
