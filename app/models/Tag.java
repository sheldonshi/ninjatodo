package models;

import json.JsonExclude;
import play.data.validation.Required;
import play.db.jpa.Model;

import javax.persistence.*;

/**
 * Created by IntelliJ IDEA.
 * User: super
 * Date: 3/17/12
 * Time: 3:09 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name="tag")
public class Tag extends Model {
    @Required
    public String text;

    // weight of occurrence of the tag
    public int weight = 0;
    
    public String toString() {
        return text;
    }
}
