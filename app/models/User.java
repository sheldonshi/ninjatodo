package models;

import json.JsonExclude;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import play.data.validation.Email;
import play.data.validation.MaxSize;
import play.data.validation.MinSize;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.JPABase;
import play.db.jpa.Model;
import play.libs.Codec;
import securesocial.provider.AuthenticationMethod;
import securesocial.provider.ProviderType;
import securesocial.provider.SocialUser;
import securesocial.provider.UserId;
import services.CacheService;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
    @MinSize(3)
    @Column(name = "username", nullable = false, unique = true)
    public String username;

    @Required
    @MaxSize(30)
    @Column(name = "full_name", nullable = false)
    public String fullName;

    @Required
    @Email
    @Column(name = "email", nullable = false)
    @JsonExclude
    public String email;

    @Column(name = "email_verified", nullable = false)
    public boolean isEmailVerified;
    
    @Column(name = "password", nullable = true)
    @JsonExclude
    public String password;

    @Version
    @Column(name = "version", nullable = false)
    @JsonExclude
    public long version;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_created", nullable = true)
    public Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_last_login", nullable = true)
    public Date dateLastLogin;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_notification_check", nullable = true)
    public Date lastNotificationCheck;

    @Column(name = "provider", nullable = true)
    @Enumerated(EnumType.STRING)
    public ProviderType provider;
    
    @Column(name = "access_token", nullable = true)
    public String accessToken;
    
    @Column(name = "avatar", nullable = true)
    public String avatar;

    @Column(name = "verify_code", nullable = true)
    @JsonExclude
    public String verifyCode;

    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    public Set<ToDoList> watchedToDoLists;
    
    public User() {
        dateCreated = new Date();
    }

    /**
     * translation to SocialUser DTO
     * 
     * @return
     */
    public SocialUser unpack() {
        SocialUser socialUser = new SocialUser();
        UserId userId = new UserId();
        userId.id = this.username;
        userId.provider = this.provider;
        socialUser.id = userId;
        socialUser.accessToken = this.accessToken;
        socialUser.authMethod = AuthenticationMethod.USER_PASSWORD;
        socialUser.avatarUrl = this.avatar;
        socialUser.displayName = this.fullName;
        socialUser.email = this.email;
        socialUser.isEmailVerified = this.isEmailVerified;
        socialUser.lastAccess = this.dateLastLogin;
        socialUser.password = this.password;
        return socialUser;
    }

    /**
     * translate socialUser DTO into this
     *
     * @return
     */
    public void pack(SocialUser socialUser) {
        this.username = socialUser.id.id;
        if ("userpass".equals(socialUser.id.provider)) {
            this.password = socialUser.password;
        } else {
            this.password = "";
        }
        this.provider = socialUser.id.provider;
        this.accessToken = socialUser.accessToken;
        this.fullName = socialUser.displayName;
        this.avatar = socialUser.avatarUrl;
        this.email = socialUser.email;
        this.isEmailVerified = socialUser.isEmailVerified;
        // only set verify code when email has not been verified
        if (!socialUser.isEmailVerified) {
            this.verifyCode = Codec.UUID();
        } else {
            this.verifyCode = null;
        }
        this.dateLastLogin = socialUser.lastAccess;
    }
    
    public static User loadBySocialUser(SocialUser socialUser) {
        if (socialUser != null) {
            // cache user id only. if you cache the whole user, there will be hibernate session attach/detach problem
            Long userId = CacheService.getUserId(socialUser.id.id);
            if (userId != null) {
                return User.findById(userId);
            } else {
                try {
                    User user = (User) JPA.em()
                            .createQuery("select u from User u left join fetch u.watchedToDoLists where username=:username")
                            .setParameter("username", socialUser.id.id)
                            .getSingleResult();
                    if (user != null) {
                        CacheService.cacheUserId(user.username, user.id);
                    }
                    return user;
                } catch (Exception e) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    /**
     * store (ie insert) the entity.
     */
    @Override
    public User save() {
        return super.save();
    }
}
