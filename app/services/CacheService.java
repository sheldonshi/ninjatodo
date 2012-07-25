package services;

import models.Participation;
import models.Project;
import models.Role;
import models.User;

/**
 * Created by IntelliJ IDEA.
 * User: sheldon
 * Date: 7/20/12
 * Time: 3:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class CacheService {
    private static final String CACHE_KEY_USERNAME = "username ";
    private static final String CACHE_KEY_PARTICIPATION_DELIMITER = "|";
    
    public static Long getUserId(String username) {
        return play.cache.Cache.get(CACHE_KEY_USERNAME + username, Long.class);
    }

    public static void cacheUserId(String username, Long userId) {
        play.cache.Cache.set(CACHE_KEY_USERNAME + username, userId);
    }
    
    public static Role getPole(User user, Project project) {
        return play.cache.Cache.get(user.id + CACHE_KEY_PARTICIPATION_DELIMITER + project.id, 
                Role.class);
    }

    public static void cacheRole(Participation p) {
        play.cache.Cache.set(p.user.id + CACHE_KEY_PARTICIPATION_DELIMITER + p.project.id, p.role, "5mn");
    }

    public static void clearRole(Participation p) {
        play.cache.Cache.delete(p.user.id + CACHE_KEY_PARTICIPATION_DELIMITER + p.project.id);
    }
}
