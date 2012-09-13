package controllers;

import au.com.bytecode.opencsv.CSVWriter;
import controllers.securesocial.SecureSocial;
import models.*;
import play.db.jpa.JPA;
import play.i18n.Messages;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Scope;
import play.mvc.With;
import services.CacheService;
import services.SecureUserService;
import utils.Utils;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: root
 * Date: 4/15/12
 * Time: 7:02 PM
 * To change this template use File | Settings | File Templates.
 */
@With( SecureSocial.class )
public class Projects extends Controller {

    /**
     * this checks whether a user has ownership role to a project
     *
     * @throws Throwable
     */
    @Before(only={"save"})
    static void checkOwnerAccess() throws Throwable {
        checkOwnership();
    }

    /**
     * this checks whether a user has membership to a project
     *
     * @throws Throwable
     */
    @Before(unless={"save"})
    static void checkAccess() throws Throwable {
        checkMembership();
    }

    /**
     * prepares for the edit setting screen
     * @param projectId
     */
    public static void edit(Long projectId) {
        Project project = Project.findById(projectId);
        List<Participation> participations = Participation.find("byProject", project).fetch();
        List<Invitation> invitations = Invitation.find("byProject", project).fetch();
        render(project, participations, invitations);
    }

    /**
     * save the edited settings
     *
     * @param projectId
     * @param title
     */
    public static void save(Long projectId, String title) {
        Project project = Project.findById(projectId);
        project.title = title;
        project.save();
        renderText("{\"saved\":\"true\"}");
    }

    public static void add(String title) {
        User user = SecureUserService.getCurrentUser();
        addProject(user, title);
        // return the whole list to refresh UI
        renderText(Utils.toJson(getParticipations(user)));
    }

    /**
     * promotes a user in a project
     * @param participationId
     * @param projectId
     */
    public static void promoteMember(Long participationId, Long projectId) {
        Project project = Project.findById(projectId);
        if (project != null) {
            Participation participation = Participation.findById(participationId);
            if (participation != null) {
                if (participation.role.equals(Role.READ) && participation.project.equals(project)) {
                    participation.role = Role.WRITE;
                }
                if (participation.role.equals(Role.WRITE) && participation.project.equals(project)) {
                    participation.role = Role.OWN;
                }
                participation.save();
                // save to cache
                CacheService.cacheRole(participation);
            }
            renderText("");
        }
    }

    /**
     * demotes a user in a project
     * @param participationId
     * @param projectId
     */
    public static void demoteMember(Long participationId, Long projectId) {
        Project project = Project.findById(projectId);
        if (project != null) {
            Participation participation = Participation.findById(participationId);
            if (participation != null) {
                if (participation.role.equals(Role.WRITE) && participation.project.equals(project)) {
                    participation.role = Role.READ;
                }
                if (participation.role.equals(Role.OWN) && participation.project.equals(project)) {
                    if (Participation.count("project=? and role=?", project, Role.OWN) > 1) {
                        participation.role = Role.WRITE; // only allow demoting an owner when there are more than one owner
                    }
                }
                participation.save();
                // save to cache
                CacheService.cacheRole(participation);
            }
            renderText("");
        }
    }

    /**
     * remove a user from a project
     * @param participationId
     * @param projectId
     */
    public static void deleteMember(Long participationId, Long projectId) {
        Participation participation = Participation.findById(participationId);
        // TODO check permission
        if (participation != null &&
                participation.project.id == projectId) {
            participation.delete();
            // save to cache
            CacheService.clearRole(participation);
            renderText(participationId);
        }
    }

    /**
     * export a project in csv
     */
    public static void exportInCSV(Long projectId) {
        response.setHeader("Content-Type", "text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"Exported_Project.csv\"");

        File csvFile = new File("tmp/project_" + projectId + ".csv");
        if (csvFile.exists()) {
            csvFile.delete();
        }
        FileWriter fw = null;
        try {
            fw = new FileWriter(csvFile);
            CSVWriter writer = new CSVWriter(fw);
            // now load the whole project
            Project project = Project.findById(projectId);

            String projectColumn = Messages.get("export_project");
            DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);

            // write project title
            writer.writeNext(new String[]{projectColumn, project.title});
            writer.writeNext(new String[]{""});

            List<String> headers = new ArrayList<String>();
            headers.add(Messages.get("export_header_list"));
            headers.add(Messages.get("export_header_task"));
            headers.add(Messages.get("export_header_priority"));
            headers.add(Messages.get("export_header_due"));
            headers.add(Messages.get("export_header_completed"));
            headers.add(Messages.get("export_header_notes"));
            writer.writeNext(headers.toArray(new String[headers.size()]));
            
            // write each list
            List<ToDoList> toDoLists = ToDoList.find("byProject", project).fetch();
            for (ToDoList toDoList : toDoLists) {
                List<ToDo> toDos = ToDo.find("toDoList=? order by completed, dateDue desc", toDoList).fetch();
                for (ToDo toDo : toDos) {
                    List<String> columns = new ArrayList<String>();
                    columns.add(toDoList.name);
                    columns.add(toDo.title);
                    columns.add(String.valueOf(toDo.priority));
                    columns.add(toDo.dateDue != null ? dateFormat.format(toDo.dateDue) : "");
                    columns.add(toDo.completed ? "true" : "false");
                    List<Note> notes = Note.find("byToDo", toDo).fetch();
                    String noteString = "";
                    for (Note note : notes) {
                        noteString += note + "\n";
                    }
                    columns.add(noteString);

                    writer.writeNext(columns.toArray(new String[columns.size()]));
                }
            }
            writer.flush();
            renderBinary(csvFile);
        } catch (IOException ioe) {
            error();
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    
                }
            }
        }
    }

    /**
     * Gets all projects this user participates
     * @param project
     * @return
     */
    static List<Participation> getAdministrators(Project project) {
        List<Participation> participations = JPA.em()
                .createQuery("select p from Participation p where p.project=:project and p.role=:role order by p.dateCreated")
                .setParameter("project", project).setParameter("role", Role.WRITE).getResultList();
        return participations;
    }

    /**
     * Gets all projects this user participates
     * @param user
     * @return
     */
    static List<Participation> getParticipations(User user) {
        List<Participation> participations = JPA.em()
                .createQuery("select p from Participation p left join p.project proj where p.user=:user order by proj.id desc")
                .setParameter("user", user).getResultList();
        return participations;
    }

    /**
     * Add a new project to a user
     *
     * @param user
     * @param title
     * @return
     */
    static Participation addProject(User user, String title) {

        // create default project if no project is available
        Project project = new Project();
        project.creator = user;
        project.title = title;
        project.save();
        Participation participation = new Participation();
        participation.project = project;
        participation.user = user;
        participation.role = Role.OWN;
        participation.save();
        return participation;
    }

    /**
     * Check whether the project has the user as a member (can write or own)
     */
    static void checkMembership() {
        if (canWrite(getProject())) {
            return; // member of a non-public project
        } else {
            throw new RuntimeException();   // no permission
        }
    }

    /**
     * this checks whether a user has ownership role to a project
     *
     * @throws Throwable
     */
    @Before(only={"save"})
    static void checkOwnership() {
        User user = User.loadBySocialUser(SecureSocial.getCurrentUser());
        String id = Scope.Params.current().get("projectId");
        Project project = Project.findById(Long.valueOf(id));
        Role currentRole = CacheService.getPole(user, project);
        if (currentRole == null) {
            Participation participation = Participation.find("project=? and user=?", project, user).first();
            if (participation != null) {
                CacheService.cacheRole(participation);
                currentRole = participation.role;
            }
        }
        if (!Role.OWN.equals(currentRole)) {
            forbidden();
        }
    }

    /**
     * Check whether the project is visible to the user
     */
    static void checkVisibility() {
        Project project = getProject();
        // check the visibility of the project
        if (project.visibility.equals(Visibility.PUBLIC)) {
            return; // public project
        } else if (project.visibility.equals(Visibility.LINK) && false) {
            // TODO deal with a private link
        } else if (canRead(project)) {
            return; // member of a non-public project
        } else {
            throw new RuntimeException();   // no permission
        }
    }

    /**
     * gets the Project from projectId or list id
     * @return
     */
    private static Project getProject() {
        String projectId = Scope.Params.current().get("projectId");
        String listId = Scope.Params.current().get("list");
        String toDoId = Scope.Params.current().get("taskId");
        // get the project first. if not available, get list. if not available get task
        Project project = null;
        if (projectId != null) {
            project = Project.findById(Long.valueOf(projectId));
        } else if (listId != null) {
            ToDoList toDoList = ToDoList.findById(Long.valueOf(listId));
            if (toDoList != null) {
                project = toDoList.project;
            }
        } else if (toDoId != null) {
            ToDo toDo = ToDo.findById(Long.valueOf(toDoId));
            if (toDo != null) {
                project = toDo.toDoList.project;
            }
        }
        return project;
    }

    /**
     * Checks if a user is a member (at least can write) of the project
     * @param project
     * @return
     */
    private static boolean canWrite(Project project) {
        Role currentRole = getCurrentRole(project);
        if (Role.OWN.equals(currentRole) || Role.WRITE.equals(currentRole)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if a user is a member (at least can read) of the project
     * @param project
     * @return
     */
    private static boolean canRead(Project project) {
        Role currentRole = getCurrentRole(project);
        if (currentRole != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * get role of the current user in this project
     *
     * @param project
     * @return
     */
    private static Role getCurrentRole(Project project) {
        User user = User.loadBySocialUser(SecureSocial.getCurrentUser());
        Role currentRole = CacheService.getPole(user, project);
        if (currentRole == null) {
            Participation participation = Participation.find("project=? and user=?", project, user).first();
            if (participation != null) {
                CacheService.cacheRole(participation);
                currentRole = participation.role;
            }
        }
        return currentRole;
    }
}
