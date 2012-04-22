package controllers;

import models.*;
import org.apache.commons.lang.StringUtils;
import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.With;
import utils.DateJsonUtils;
import utils.JSConstant;
import utils.Utils;

import javax.persistence.NoResultException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: super
 * Date: 3/18/12
 * Time: 3:45 PM
 * To change this template use File | Settings | File Templates.
 */
@With(LanguageController.class)
public class ToDoLists extends Controller {
    /**
     * required json data structure on the UI {'list':[], 'total':..}
     */
    public static void loadLists() {
        List toDoLists = ToDoList.find("order by orderIndex").fetch();
        renderJSON(Utils.toJson(toDoLists));
    }

    /**
     * required json data structure on the UI {'list':[], 'total':..}
     */
    public static void addList(String name) {
        ToDoList toDoList = new ToDoList();
        toDoList.name = name;
        toDoList.project = Project.all().first();
        toDoList.save();
        try {
            Integer lastOrderIndex = (Integer) JPA.em()
                    .createQuery("select orderIndex from ToDoList order by orderIndex desc")
                    .setMaxResults(1)
                    .getSingleResult();
            toDoList.orderIndex = lastOrderIndex + 1;
        } catch (NoResultException nre) {
            // orderIndex should remain the default 0
        }
        renderText(Utils.toJson(toDoList));
    }

    /**
     * rename a list
     */
    public static void renameList(Long list, String name) {
        ToDoList toDoList = ToDoList.findById(list);
        if (toDoList != null) {
            toDoList.name = name;
            toDoList.save();
        }
        
        renderText(Utils.toJson(toDoList));
    }

    /**
     * clear (delete) a list. only if all tasks are completed
     */
    public static void clearCompletedInList(Long list) {
        int count = 0;

        ToDoList toDoList = ToDoList.findById(list);
        if (toDoList != null) {
            // delete in one shot                                                                                     
//            count = JPA.em().createQuery("delete from ToDo t where t.dateCompleted is not null and t.toDoList.id=" + list).executeUpdate();
//            count = JPA.em().createQuery("delete from ToDo t where t.dateCompleted is not null and t.toDoList.id=" + list).executeUpdate();
            List<ToDo> toDos = ToDo.find("toDoList=? and dateCompleted is not null", toDoList).fetch();
            for (ToDo toDo : toDos) {
                toDo.delete();
                count++;
            }
        }

        renderText(Utils.toJson(count));
    }


    /**
     * changes order of a list. has to use native query because case when then won't work otherwise
     */
    public static void changeListOrder(Long[] order) {
        int count = 0;
        if (order != null && order.length > 0) {
            String updateQuery = "update ToDoList t set t.orderIndex=CASE\n";
            for (int i=0; i<order.length; i++) {
                Long id = order[i];
                updateQuery += " WHEN t.id=" + id + " THEN " + i + "\n";
            }
            updateQuery +=  " END where t.id in (" + StringUtils.join(order, ",") + ")";
            count = JPA.em().createQuery(updateQuery).executeUpdate();
        }

        renderText(Utils.toJson(count));
    }

    /**
     * delete a list
     */
    public static void deleteList(Long list) {
        ToDoList toDoList = ToDoList.findById(list);
        if (toDoList != null) {
            toDoList.delete();
        }

        renderText(Utils.toJson(toDoList));
    }

    /**
     * sets new sort order of a list
     */
    public static void setListSortOrder(Long list, String sort) {
        ToDoList toDoList = ToDoList.findById(list);
        SortOrder sortOrder = SortOrder.valueOf(sort);
        if (toDoList != null) {
            toDoList.sortOrder = sortOrder;
            toDoList.save();
        }

        renderText(Utils.toJson(toDoList));
    }

}
