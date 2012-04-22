package models;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import play.data.validation.MaxSize;
import play.data.validation.MinSize;
import play.data.validation.Required;
import play.db.jpa.Model;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: super
 * Date: 3/17/12
 * Time: 3:14 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name="user")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class User extends Model {

    @Required
    @MaxSize(20)
    @MinSize(2)
    @Column(name = "username", nullable = false)   // unique
    public String username;

    @Required
    @MaxSize(30)
    @MinSize(2)
    @Column(name = "screen_name", nullable = false)
    public String screenName;

    @Version
    @Column(name = "version", nullable = false)
    public long version;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_created", nullable = true)
    public Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_last_login", nullable = true)
    public Date dateLastLogin;

    @Column(name = "role", nullable = true)
    @Enumerated(EnumType.STRING)
    public Role role;

    public User() {
        dateCreated = new Date();
    }
}
