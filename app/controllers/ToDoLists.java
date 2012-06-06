package controllers;

import controllers.securesocial.SecureSocial;
import models.*;
import org.apache.commons.lang.StringUtils;
import play.db.jpa.JPA;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Scope;
import utils.Utils;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: super
 * Date: 3/18/12
 * Time: 3:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class ToDoLists extends Controller {

    /**
     * Check whether the project is visible to the user
     */
    @Before(only = {"loadLists"})
     static void checkReadAccess() {
        Projects.checkVisibility();
    }

    /**
     * Check whether the project has the user as a member
     */
    @Before(unless = {"loadLists"})
    static void checkWriteAccess() {
        Projects.checkMembership();
    }
    
    /**
     * required json data structure on the UI {'list':[], 'total':..}
     * <p/>
     * this requires visibility to the project
     */
    public static void loadLists(Long projectId) {
        Project proj = Project.findById(projectId);
        List toDoLists = proj != null ? ToDoList.find("project=? order by orderIndex", proj).fetch() : null;
        renderJSON(Utils.toJson(toDoLists));
    }

    /**
     * required json data structure on the UI {'list':[], 'total':..}
     * <p/>
     * This requires to be project member
     */
    public static void addList(String name, Long projectId) {
        ToDoList toDoList = new ToDoList();
        toDoList.name = name;
        toDoList.project = Project.findById(projectId);
        toDoList.creator = User.loadBySocialUser(SecureSocial.getCurrentUser());
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
     * requires to be the owner of list
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
     * requires to be the owner of list
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
     * changes order of a list. has to use native query because case when then won't work otherwise.
     * <p/>
     * should be user specific
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
     * <p/>
     * must be the owner of the list or admin of project
     */
    public static void deleteList(Long list) {
        ToDoList toDoList = ToDoList.findById(list);
        if (toDoList != null) {
            // first delete all tasks within the list
            List<ToDo> todos = ToDo.find("byToDoList", toDoList).fetch();
            for (ToDo todo : todos) {
                todo.delete();
            }
            toDoList.delete();
        }

        renderText(Utils.toJson(toDoList));
    }

    /**
     * sets new sort order of a list
     * <p/>
     * should be user specific
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

    /**
     * toggles notesExpanded boolean property of a list
     * <p/>
     * should be user specific
     */
    public static void toggleNotesExpanded(Long list) {
        ToDoList toDoList = ToDoList.findById(list);
        if (toDoList != null) {
            toDoList.notesExpanded = !toDoList.notesExpanded;
            toDoList.save();
        }

        renderText(Utils.toJson(toDoList));
    }

    /**
     * get all tags contained in a to do list. If an id of -1 is passed, it means all lists in the project
     * TODO if list=-1, we should have project id and get tags under that project id
     *
     * <p/>
     * check visibility
     */
    public static void tagCloud(Long list, Long projectId) {
        
        List<Tag> tags = null;
        if (list > 0) {
            tags = JPA.em()
                .createQuery("select t from ToDo todo right join todo.tags t where todo.toDoList.id=" + list)
                .getResultList();
        } else if (list == -1) {
            Project proj = Project.findById(projectId);
            if (proj != null) {
                Query query = JPA.em()
                        .createQuery("select t from Tag t where project=:project");
                query.setParameter("project", proj);
                tags = query.getResultList();
            }
        }

        renderText(Utils.toJson(tags));
    }
}
