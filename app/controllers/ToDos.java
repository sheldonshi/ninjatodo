package controllers;

import models.*;
import org.apache.commons.lang.StringUtils;
import play.db.jpa.JPA;
import play.mvc.Before;
import play.mvc.Controller;
import utils.JSConstant;
import utils.Utils;

import javax.persistence.NoResultException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: root
 * Date: 4/15/12
 * Time: 5:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class ToDos extends Controller {
    public static final Long ALL_LIST_ID = -1L;
    // this is based on twitter-text-java
    public static final Pattern VALID_HASHTAG = Pattern.compile("(^|[^&0-9\\uff10-\\uff19_a-z\\u00c0-\\u00d6\\u00d8-\\u00f6\\u00f8-\\u00ff\\u0100-\\u024f\\u0253\\u0254\\u0256\\u0257\\u0259\\u025b\\u0263\\u0268\\u026f\\u0272\\u0289\\u028b\\u02bb\\u0300-\\u036f\\u1e00-\\u1eff\\u0400-\\u04ff\\u0500-\\u0527\\u2de0-\\u2dff\\ua640-\\ua69f\\u0591-\\u05bf\\u05c1-\\u05c2\\u05c4-\\u05c5\\u05c7\\u05d0-\\u05ea\\u05f0-\\u05f4\\ufb1d-\\ufb28\\ufb2a-\\ufb36\\ufb38-\\ufb3c\\ufb3e\\ufb40-\\ufb41\\ufb43-\\ufb44\\ufb46-\\ufb4f\\u0610-\\u061a\\u0620-\\u065f\\u066e-\\u06d3\\u06d5-\\u06dc\\u06de-\\u06e8\\u06ea-\\u06ef\\u06fa-\\u06fc\\u06ff\\u0750-\\u077f\\u08a0\\u08a2-\\u08ac\\u08e4-\\u08fe\\ufb50-\\ufbb1\\ufbd3-\\ufd3d\\ufd50-\\ufd8f\\ufd92-\\ufdc7\\ufdf0-\\ufdfb\\ufe70-\\ufe74\\ufe76-\\ufefc\\u200c\\u0e01-\\u0e3a\\u0e40-\\u0e4e\\u1100-\\u11ff\\u3130-\\u3185\\uA960-\\uA97F\\uAC00-\\uD7AF\\uD7B0-\\uD7FF\\p{InHiragana}\\p{InKatakana}\\p{InCJKUnifiedIdeographs}\\u3003\\u3005\\u303b\\uff21-\\uff3a\\uff41-\\uff5a\\uff66-\\uff9f\\uffa1-\\uffdc])(#|＃)([0-9\\uff10-\\uff19_a-z\\u00c0-\\u00d6\\u00d8-\\u00f6\\u00f8-\\u00ff\\u0100-\\u024f\\u0253\\u0254\\u0256\\u0257\\u0259\\u025b\\u0263\\u0268\\u026f\\u0272\\u0289\\u028b\\u02bb\\u0300-\\u036f\\u1e00-\\u1eff\\u0400-\\u04ff\\u0500-\\u0527\\u2de0-\\u2dff\\ua640-\\ua69f\\u0591-\\u05bf\\u05c1-\\u05c2\\u05c4-\\u05c5\\u05c7\\u05d0-\\u05ea\\u05f0-\\u05f4\\ufb1d-\\ufb28\\ufb2a-\\ufb36\\ufb38-\\ufb3c\\ufb3e\\ufb40-\\ufb41\\ufb43-\\ufb44\\ufb46-\\ufb4f\\u0610-\\u061a\\u0620-\\u065f\\u066e-\\u06d3\\u06d5-\\u06dc\\u06de-\\u06e8\\u06ea-\\u06ef\\u06fa-\\u06fc\\u06ff\\u0750-\\u077f\\u08a0\\u08a2-\\u08ac\\u08e4-\\u08fe\\ufb50-\\ufbb1\\ufbd3-\\ufd3d\\ufd50-\\ufd8f\\ufd92-\\ufdc7\\ufdf0-\\ufdfb\\ufe70-\\ufe74\\ufe76-\\ufefc\\u200c\\u0e01-\\u0e3a\\u0e40-\\u0e4e\\u1100-\\u11ff\\u3130-\\u3185\\uA960-\\uA97F\\uAC00-\\uD7AF\\uD7B0-\\uD7FF\\p{InHiragana}\\p{InKatakana}\\p{InCJKUnifiedIdeographs}\\u3003\\u3005\\u303b\\uff21-\\uff3a\\uff41-\\uff5a\\uff66-\\uff9f\\uffa1-\\uffdc]*[a-z\\u00c0-\\u00d6\\u00d8-\\u00f6\\u00f8-\\u00ff\\u0100-\\u024f\\u0253\\u0254\\u0256\\u0257\\u0259\\u025b\\u0263\\u0268\\u026f\\u0272\\u0289\\u028b\\u02bb\\u0300-\\u036f\\u1e00-\\u1eff\\u0400-\\u04ff\\u0500-\\u0527\\u2de0-\\u2dff\\ua640-\\ua69f\\u0591-\\u05bf\\u05c1-\\u05c2\\u05c4-\\u05c5\\u05c7\\u05d0-\\u05ea\\u05f0-\\u05f4\\ufb1d-\\ufb28\\ufb2a-\\ufb36\\ufb38-\\ufb3c\\ufb3e\\ufb40-\\ufb41\\ufb43-\\ufb44\\ufb46-\\ufb4f\\u0610-\\u061a\\u0620-\\u065f\\u066e-\\u06d3\\u06d5-\\u06dc\\u06de-\\u06e8\\u06ea-\\u06ef\\u06fa-\\u06fc\\u06ff\\u0750-\\u077f\\u08a0\\u08a2-\\u08ac\\u08e4-\\u08fe\\ufb50-\\ufbb1\\ufbd3-\\ufd3d\\ufd50-\\ufd8f\\ufd92-\\ufdc7\\ufdf0-\\ufdfb\\ufe70-\\ufe74\\ufe76-\\ufefc\\u200c\\u0e01-\\u0e3a\\u0e40-\\u0e4e\\u1100-\\u11ff\\u3130-\\u3185\\uA960-\\uA97F\\uAC00-\\uD7AF\\uD7B0-\\uD7FF\\p{InHiragana}\\p{InKatakana}\\p{InCJKUnifiedIdeographs}\\u3003\\u3005\\u303b\\uff21-\\uff3a\\uff41-\\uff5a\\uff66-\\uff9f\\uffa1-\\uffdc][0-9\\uff10-\\uff19_a-z\\u00c0-\\u00d6\\u00d8-\\u00f6\\u00f8-\\u00ff\\u0100-\\u024f\\u0253\\u0254\\u0256\\u0257\\u0259\\u025b\\u0263\\u0268\\u026f\\u0272\\u0289\\u028b\\u02bb\\u0300-\\u036f\\u1e00-\\u1eff\\u0400-\\u04ff\\u0500-\\u0527\\u2de0-\\u2dff\\ua640-\\ua69f\\u0591-\\u05bf\\u05c1-\\u05c2\\u05c4-\\u05c5\\u05c7\\u05d0-\\u05ea\\u05f0-\\u05f4\\ufb1d-\\ufb28\\ufb2a-\\ufb36\\ufb38-\\ufb3c\\ufb3e\\ufb40-\\ufb41\\ufb43-\\ufb44\\ufb46-\\ufb4f\\u0610-\\u061a\\u0620-\\u065f\\u066e-\\u06d3\\u06d5-\\u06dc\\u06de-\\u06e8\\u06ea-\\u06ef\\u06fa-\\u06fc\\u06ff\\u0750-\\u077f\\u08a0\\u08a2-\\u08ac\\u08e4-\\u08fe\\ufb50-\\ufbb1\\ufbd3-\\ufd3d\\ufd50-\\ufd8f\\ufd92-\\ufdc7\\ufdf0-\\ufdfb\\ufe70-\\ufe74\\ufe76-\\ufefc\\u200c\\u0e01-\\u0e3a\\u0e40-\\u0e4e\\u1100-\\u11ff\\u3130-\\u3185\\uA960-\\uA97F\\uAC00-\\uD7AF\\uD7B0-\\uD7FF\\p{InHiragana}\\p{InKatakana}\\p{InCJKUnifiedIdeographs}\\u3003\\u3005\\u303b\\uff21-\\uff3a\\uff41-\\uff5a\\uff66-\\uff9f\\uffa1-\\uffdc]*)");

    /**
     * Check whether the project is visible to the user
     */
    @Before(only = {"loadTasks"})
    static void checkReadAccess() {
        Projects.checkVisibility();
    }

    /**
     * Check whether the project has the user as a member
     */
    @Before(unless = {"loadTasks"})
    static void checkWriteAccess() {
        Projects.checkMembership();
    }

    public static void newTask(Long list, String title) {
        ToDo toDo = new ToDo();
        toDo.title = title;
        toDo.toDoList = ToDoList.findById(list);
        toDo.orderIndex = getNextOrderIndex(toDo.toDoList);
        // convert hashtags in title into tags, and remove them from title
        List<String> hashtags = parseHashtags(toDo);
        if (!hashtags.isEmpty()) {
            addTagToToDo(StringUtils.join(hashtags, ","), toDo);
        }
        toDo.save();
        renderText(Utils.toJson(toDo));
    }

    /**
     * get the next order index of the list for a new task
     * @param toDoList
     * @return
     */
    private static int getNextOrderIndex(ToDoList toDoList) {
            try {
                Integer firstOrderIndex = (Integer) JPA.em()
                        .createQuery("select orderIndex from ToDo where toDoList =:toDoList order by orderIndex")
                        .setParameter("toDoList", toDoList)
                        .setMaxResults(1)
                        .getSingleResult();
                return firstOrderIndex - 1;
            } catch (NoResultException nre) {
                // orderIndex should remain the default 0
                return 0;
            }
    }

    /**
     * clone a toDo. The new toDo will always be incompleted, has its own 
     * created date and last updated date
     */
    public static void cloneTask(Long taskId) {
        ToDo toDo = ToDo.findById(taskId);
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
     * Adds comma separated tags to existing tags of a to do task, does not check for duplicity
     * @param taskId
     * @param tags
     */
    public static void addTag(Long taskId, String tags) {
        ToDo toDo = ToDo.findById(taskId);
        if (toDo != null) {
            addTagToToDo(tags, toDo);
            toDo.save();
        }
        renderText(Utils.toJson(toDo));
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
            if (toDoList != null) {
                toDoList.showCompleted = showCompleted;
                toDoList.save();
            }
        }
        // treat undefined sort parameter as null
        String sort = JSConstant.STRING_UNDEFINED.equals(params.get("sort")) ? null : params.get("sort");

        String querySortStr = " order by " + (sort != null ? Sort.valueOf(sort).getSqlSort() : Sort.DEFAULT.getSqlSort());
        // if a valid list id is passed, filter by list, otherwise, filter by project
        String projectFilterStr = "toDoList.project.id=" + params.get("projectId");
        String listFilterStr = "toDoList.id=" + list;
        List<ToDo> tasks = new ArrayList<ToDo>();
        if (StringUtils.isNotEmpty(params.get("s"))) {
            String queryLikeStr = "%"+ params.get("s") + "%";
            String queryStr = (list == ALL_LIST_ID ? projectFilterStr : listFilterStr) +
                    " and (title like ? or note like ?)" + (showCompleted ? "" : " and completed=false");
            tasks = ToDo.find(queryStr + querySortStr, queryLikeStr, queryLikeStr).fetch();
        } else {
            String queryStr = (list == ALL_LIST_ID ? projectFilterStr : listFilterStr) +
                    (showCompleted ? "" : " and completed=false");
            tasks = ToDo.find(queryStr + querySortStr).fetch();
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

    public static void completeTask(Long taskId, boolean completed) {
        ToDo toDo = ToDo.findById(taskId);
        toDo.completed = completed;
        toDo.save();
        renderText(Utils.toJson(toDo));
    }

    /**
     *
     * @param list the primary key of ToDoList
     */
    public static void saveFullTask(Long list, Long taskId) {
        ToDo toDo = null;
        if (taskId == null) {
            toDo = new ToDo();
        } else {
            toDo = ToDo.findById(taskId);
            list = toDo.toDoList.id;
        }
        toDo.title = params.get("title");
        toDo.priority = Integer.parseInt(params.get("prio"));
        toDo.note = params.get("note");
        toDo.toDoList = ToDoList.findById(list);
        toDo.orderIndex = getNextOrderIndex(toDo.toDoList);
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
        addTagToToDo(params.get("tags"), toDo);
        toDo.save();

        renderText(Utils.toJson(toDo));
    }

    /**
     * parse out hashtags into an array and remove hashtags from task title
     * @param toDo
     * @return
     */
    private static List<String> parseHashtags(ToDo toDo) {

        List<String> hashtags = new ArrayList<String>();
        Matcher matcher = VALID_HASHTAG.matcher(toDo.title);
        boolean foundHashtags = false;
        while (matcher.find()) {
            foundHashtags = true;
            hashtags.add(toDo.title.substring(matcher.start() + 2, matcher.end()));
        }
        if (foundHashtags) {
            toDo.title = matcher.replaceAll("");
        }
        return hashtags;
    }

    /**
     * convenient method to add comma separated tags to todo
     * @param tagsString
     * @param toDo
     */
    private static void addTagToToDo(String tagsString, ToDo toDo) {
        if (StringUtils.isEmpty(tagsString)) {
            return;
        }
        String[] tags = tagsString.split(",");
        for (String t : tags) {
            // first find if tag exists, if not, save it
            t = t.trim();
            // remove empty tag
            if (t.length() > 0) {
                Tag existing = Tag.find("text=? and project=?", t, toDo.toDoList.project).first();
                if (existing == null) {
                    // save this tag if it has not been saved before
                    Tag tag = new Tag();
                    tag.text = t;
                    tag.project = toDo.toDoList.project;
                    tag.save();
                    existing = tag;
                }
                if (toDo.tags == null) {
                    toDo.tags = new ArrayList<Tag>();
                }
                toDo.tags.add(existing);
            }
        }
    }

    /**
     * edit the note of a task
     */
    public static void editNote(Long taskId, String note) {
        ToDo toDo = ToDo.findById(taskId);
        if (toDo != null) {
            toDo.note = note;
            toDo.save();
        }

        renderText(Utils.toJson(toDo));
    }

    /**
     * delete a task completely
     */
    public static void deleteTask(Long taskId) {
        ToDo toDo = ToDo.findById(taskId);
        if (toDo != null) {
            toDo.delete();
        }

        renderText(Utils.toJson(1));
    }

    /**
     * delete a task completely
     */
    public static void setPriority(Long taskId, int prio) {
        ToDo toDo = ToDo.findById(taskId);
        if (toDo != null) {
            toDo.priority = prio;
            toDo.save();
        }

        renderText(Utils.toJson(toDo));
    }

    /**
     * move a task from the current list with id = from to a list with id = to
     * @param taskId
     * @param from
     * @param to
     */
    public static void moveTask(Long taskId, Long from, Long to) {
        ToDo toDo = ToDo.findById(taskId);
        ToDoList newToDoList = ToDoList.findById(to);
        if (toDo != null && toDo.toDoList.id == from && newToDoList != null) {
            toDo.toDoList = newToDoList;
            toDo.orderIndex = getNextOrderIndex(toDo.toDoList);
            toDo.save();
        }

        renderText(Utils.toJson(toDo));
    }

    /**
     * changes order of a task. Arguments back and forward are mutually exclusive.
     * @param taskId - the id of the single task being dragged and moved
     * @param back - the list of ids of tasks that are being pushed back
     * @param forward - the list of ids of tasks that are being pushed forward
     */
    public static void changeOrder(Long taskId, Long[] back, Long[] forward) {
        if (back != null && back.length > 0) {
            // push the single task being dragged to the first of the tasks that are being moved back
            ToDo currentToDo = ToDo.findById(taskId);
            ToDo pushedToDo = ToDo.findById(back[0]);
            if (currentToDo != null && pushedToDo != null) {
                currentToDo.orderIndex = pushedToDo.orderIndex;
            }
            currentToDo.save();
            JPA.em().createQuery("update ToDo t set t.orderIndex = t.orderIndex + 1 where t.id in (" + StringUtils.join(back, ",") + ")")
                    .executeUpdate();
        } else if (forward != null && forward.length > 0) {
            // push the single task being dragged to the last of tasks that are being moved forward
            ToDo currentToDo = ToDo.findById(taskId);
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
