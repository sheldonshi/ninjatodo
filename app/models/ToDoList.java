package models;

import json.JsonExclude;
import play.data.validation.MaxSize;
import play.data.validation.MinSize;
import play.data.validation.Required;
import play.db.jpa.Model;

import javax.persistence.*;
import java.util.Date;

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

    @Column(name = "sort_order", nullable = true)
    @Enumerated(EnumType.STRING)
    public SortOrder sortOrder;

    @Column(name = "show_completed")
    public boolean showCompleted;

    @Column(name = "notes_expanded")
    public boolean notesExpanded;
    
    public ToDoList() {
        dateCreated = new Date();
        lastUpdated = dateCreated;
    }
}
