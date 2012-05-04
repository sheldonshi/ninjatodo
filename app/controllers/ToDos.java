package controllers;

import models.*;
import org.apache.commons.lang.StringUtils;
import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.With;
import utils.JSConstant;
import utils.Utils;

import javax.persistence.NoResultException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: root
 * Date: 4/15/12
 * Time: 5:07 PM
 * To change this template use File | Settings | File Templates.
 */
@With(LanguageFilter.class)
public class ToDos extends Controller {

    public static void newTask(Long list, String title) {
        ToDo toDo = new ToDo();
        toDo.title = title;
        toDo.toDoList = ToDoList.findById(list);
        try {
            Integer lastOrderIndex = (Integer) JPA.em()
                    .createQuery("select orderIndex from ToDo order by orderIndex desc")
                    .setMaxResults(1)
                    .getSingleResult();
            toDo.orderIndex = lastOrderIndex + 1;
        } catch (NoResultException nre) {
            // orderIndex should remain the default 0
        }
        toDo.save();
        renderText(Utils.toJson(toDo));
    }
    
    /**
     * clone a toDo. The new toDo will always be incompleted, has its own 
     * created date and last updated date
     */
    public static void cloneTask(Long id) {
        ToDo toDo = ToDo.findById(id);
        ToDo newToDo = null;
        if (toDo != null) {
            newToDo = new ToDo();
            newToDo.title = toDo.title;
            newToDo.dateDue = toDo.dateDue;
            newToDo.toDoList = toDo.toDoList;
            newToDo.note = toDo.note;
            newToDo.priority = toDo.priority;
            if (toDo.tags != null) {
                newToDo.tags = new ArrayList<Tag>();
                for (Tag tag : toDo.tags) {
                    newToDo.tags.add(tag);
                }
            }
            newToDo.completed = false;
            newToDo.orderIndex = toDo.orderIndex;
            newToDo.save();
        }
        renderText(Utils.toJson(newToDo));
    }

    /**
     *
     if(params.search && params.search != '') q += '&s='+encodeURIComponent(params.search);
     if(params.tag && params.tag != '') q += '&t='+encodeURIComponent(params.tag);
     if(params.setCompl && params.setCompl != 0) q += '&setCompl=1';
     q += '&rnd='+Math.random();


     $.getJSON(this.mtt.mttUrl+'loadTasks&list='+params.list+'&completed=' + params.completed + '&changeShowCompleted='+params.changeShowCompleted+'&sort='+params.sort+q, callback);
     */
    public static void loadTasks(Long list, Boolean showCompleted, Boolean changeShowCompleted, String t) {
        if (Boolean.TRUE.equals(changeShowCompleted)) {
            // showCompleted has been changed; update. TODO check permission
            ToDoList toDoList = ToDoList.findById(list);
            toDoList.showCompleted = showCompleted;
            toDoList.save();
        }
        // store
        String sort = params.get("sort") != null && !params.get("sort").equals(JSConstant.STRING_UNDEFINED) ?
                params.get("sort") : null;

        String querySortStr = " order by " + (sort != null ? sort : SortOrder.DEFAULT.getSqlSort());
        List<ToDo> tasks = new ArrayList<ToDo>();
        if (!StringUtils.isEmpty(params.get("s"))) {
            String queryLikeStr = "%"+ params.get("s") + "%";
            tasks = ToDo.find("toDoList.id=? and (title like ? or note like ?)" + (showCompleted ? "" : " and completed=false") + querySortStr, list, queryLikeStr, queryLikeStr).fetch();
        } else if (list != -1) {
            tasks = ToDo.find("toDoList.id=? " + (showCompleted ? "" : " and completed=false") + querySortStr, list).fetch();
        } else {
            tasks = ToDo.find(querySortStr).fetch();
        }
        // filter by tag now
        if (StringUtils.isNotEmpty(t)) {
            String[] tags = t.split(",");
            for (int i=0; i<tasks.size(); i++) {
                // loop through all tags. filtered result must have all tags
                for (String tag : tags) {
                    if (!tasks.get(i).hasTag(tag)) {
                        tasks.remove(i);
                        i--;
                        break;
                    }
                }
            }
        }
        renderText(Utils.toJson(tasks));
    }

    public static void completeTask(Long id, boolean completed) {
        ToDo toDo = ToDo.findById(id);
        toDo.completed = completed;
        toDo.save();
        renderText(Utils.toJson(toDo));
    }

    /**
     *
     * @param list the primary key of ToDoList
     */
    public static void saveFullTask(Long list, Long id) {
        ToDo toDo = null;
        if (id == null) {
            toDo = new ToDo();
        } else {
            toDo = ToDo.findById(id);
            list = toDo.toDoList.id;
        }
        toDo.title = params.get("title");
        toDo.priority = Integer.parseInt(params.get("prio"));
        toDo.note = params.get("note");
        toDo.toDoList = ToDoList.findById(list);
        if (params.get("duedate") != null && params.get("duedate").length() > 0) {
            try {
                toDo.dateDue = (new SimpleDateFormat("MM/dd/yy")).parse(params.get("duedate"));
            } catch (ParseException pe) {
                // TODO error handling
            }
        }
        // TODO deal with tags
        if (toDo.tags != null) {
            toDo.tags.clear();
        } else {
            toDo.tags = new ArrayList<Tag>();
        }
        if (params.get("tags") != null) {
            String[] tags = params.get("tags").split(",");
            for (String t : tags) {
                // first find if tag exists, if not, save it
                t = t.trim();
                // remove empty tag
                if (t.length() > 0) {
                    Tag existing = Tag.find("text=?", t).first();
                    if (existing == null) {
                        Tag tag = new Tag();
                        tag.text = t;
                        tag.project = Project.findById(1L);
                        tag.save();
                        existing = tag;
                    }
                    toDo.tags.add(existing);
                }
            }
        }
        toDo.save();

        renderText(Utils.toJson(toDo));
    }

    /**
     */
    public static void tagCloud(Long list) {
        List<Tag> tags = JPA.em()
                .createQuery("select t from ToDo todo right join todo.tags t where todo.toDoList.id=" + list)
                .getResultList();

        renderText(Utils.toJson(tags));
    }

    /**
     * edit the note of a task
     */
    public static void editNote(Long id, String note) {
        ToDo toDo = ToDo.findById(id);
        if (toDo != null) {
            toDo.note = note;
            toDo.save();
        }

        renderText(Utils.toJson(toDo));
    }

    /**
     * delete a task completely
     */
    public static void deleteTask(Long id) {
        ToDo toDo = ToDo.findById(id);
        if (toDo != null) {
            toDo.delete();
        }

        renderText(Utils.toJson(1));
    }

    /**
     * delete a task completely
     */
    public static void setPriority(Long id, int prio) {
        ToDo toDo = ToDo.findById(id);
        if (toDo != null) {
            toDo.priority = prio;
            toDo.save();
        }

        renderText(Utils.toJson(toDo));
    }

    /**
     * move a task from the current list with id = from to a list with id = to
     * @param id
     * @param from
     * @param to
     */
    public static void moveTask(Long id, Long from, Long to) {
        ToDo toDo = ToDo.findById(id);
        ToDoList newToDoList = ToDoList.findById(to);
        if (toDo != null && toDo.toDoList.id == from && newToDoList != null) {
            toDo.toDoList = newToDoList;
            toDo.save();
        }

        renderText(Utils.toJson(toDo));
    }

    /**
     * changes order of a task
     */
    public static void changeOrder(Long id, Long[] back, Long[] forward) {
        if (back != null && back.length > 0) {
            ToDo currentToDo = ToDo.findById(id);
            ToDo pushedToDo = ToDo.findById(back[0]);
            if (currentToDo != null && pushedToDo != null) {
                currentToDo.orderIndex = pushedToDo.orderIndex;
            }
            currentToDo.save();
            JPA.em().createQuery("update ToDo t set t.orderIndex = t.orderIndex + 1 where t.id in (" + StringUtils.join(back, ",") + ")")
                    .executeUpdate();
        } else if (forward != null && forward.length > 0) {
            ToDo currentToDo = ToDo.findById(id);
            ToDo pushedToDo = ToDo.findById(forward[forward.length - 1]);
            if (currentToDo != null && pushedToDo != null) {
                currentToDo.orderIndex = pushedToDo.orderIndex;
            }
            currentToDo.save();
            JPA.em().createQuery("update ToDo t set t.orderIndex = t.orderIndex - 1 where t.id in (" + StringUtils.join(forward, ",") + ")")
                    .executeUpdate();
        }

        renderText("");
    }
}
