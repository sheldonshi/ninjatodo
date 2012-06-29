package controllers;

import controllers.securesocial.SecureSocial;
import models.Note;
import models.ToDo;
import models.User;
import org.apache.commons.lang.StringUtils;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: sheldon
 * Date: 6/24/12
 * Time: 4:15 PM
 * To change this template use File | Settings | File Templates.
 */

@With( SecureSocial.class )
public class MigrationController extends Controller {
    @Before
    public static void checkIsAdmin() {
        User user = User.loadBySocialUser(SecureSocial.getCurrentUser());
        if (user.username.equals("tuxzilla")) {
            return;
        } else {
            forbidden();
        }
    }

    /**
     * this is only run once
     */
    /*
    public static void migrateSingleNoteToMultipleNotes() {
        List<ToDo> todos = ToDo.find("note is not null").fetch();
        for (ToDo todo : todos) {
            if (StringUtils.isNotEmpty(todo.note)) {
                String[] notes = todo.note.split("\n");
                int count = 0;
                for (String noteString : notes) {
                    if (StringUtils.isNotEmpty(noteString.trim())) {
                        Note note = new Note();
                        if (noteString.length() > 255) {
                            noteString = noteString.substring(0, 255);
                        }
                        note.content = noteString;
                        note.toDo = todo;
                        note.save();
                        count ++;
                    }
                }
                todo.noteCount = count;
                todo.save();
            }
        }
        renderText("ok");
    } */
}
