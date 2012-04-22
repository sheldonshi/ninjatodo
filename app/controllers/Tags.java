package controllers;

import models.Tag;
import org.apache.commons.lang.StringUtils;
import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.With;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: root
 * Date: 4/17/12
 * Time: 8:52 AM
 * To change this template use File | Settings | File Templates.
 */
@With(LanguageController.class)
public class Tags extends Controller {
    /**
     * return up to limit # of suggested tags that contains q
     *
     * @param project
     * @param q
     * @param limit
     */
    public static void suggest(Long project, String q, Integer limit) {
        if (limit == null) {
            limit = 8;
        } 
        String returnString = "";
        if (StringUtils.isNotEmpty(q)) {
            List<Tag> tags = Tag.find("project.id=? and name like '%" + q + "%'", project).fetch(limit);
            for (Tag tag : tags) {
                returnString += tag.text + "|" + tag.id + "\n";
            }
        }
        renderText(returnString);
    }
}
