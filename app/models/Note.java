package models;

import json.JsonExclude;
import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.Model;

import javax.persistence.*;

/**
 * Created by IntelliJ IDEA.
 * User: sheldon
 * Date: 6/24/12
 * Time: 3:47 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name="note")
public class Note extends Model {

    @Required
    @MaxSize(255)
    @Column(name = "content", nullable = false)
    public String content;

    public boolean checked;

    @ManyToOne
    @JsonExclude
    @JoinColumn(name = "todo_id", nullable = false)
    public ToDo toDo;
}
