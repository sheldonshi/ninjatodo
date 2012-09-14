package models;

import controllers.securesocial.SecureSocial;
import json.JsonExclude;
import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.Model;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: super
 * Date: 3/17/12
 * Time: 3:08 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name="todolist")
public class ToDoList extends Model {
    @Required
    @MaxSize(20)
    @Column(name = "name", nullable = false)
    public String name;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_created", nullable = true)
    public Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_updated", nullable = true)
    public Date lastUpdated;

    @Required
    @ManyToOne
    @JoinColumn(name="creator_id", nullable = false)
    @JsonExclude
    public User creator;

    @Required
    @ManyToOne
    @JoinColumn(name="project_id", nullable = false)
    @JsonExclude
    public Project project;

    @Column(name = "order_index", nullable = false)
    public int orderIndex;

    @Transient
    public Sort sort = Sort.DEFAULT;

    @Transient
    public boolean showCompleted;

    @Transient
    public boolean notesExpanded;

    //@Transient
    //public boolean showMetadata;

    @Transient
    public boolean watchedByMe;
    
    public ToDoList() {
        dateCreated = new Date();
        lastUpdated = dateCreated;
    }

    public void delete(User currentUser) {

        // first delete all tasks within the list
        List<ToDo> todos = ToDo.find("byToDoList", this).fetch();
        for (ToDo todo : todos) {
            todo.delete();
        }
        // then remove watch list, otherwise will have foreign key constraint violation
        List<User> users = User
                .find("select u from User u left join u.watchedToDoLists l where l=?", this)
                .fetch();
        if (users != null) {
            for (User user : users) {
                if (user.equals(currentUser)) {
                    currentUser.watchedToDoLists.remove(this);
                    currentUser.save();
                } else {
                    user.watchedToDoLists.remove(this);
                }
            }
        }
        this.delete();
    }
}
