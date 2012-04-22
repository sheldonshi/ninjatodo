package utils;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: sheldon
 * Date: 4/3/12
 * Time: 2:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class DateJsonUtils {
    public static String toJson(Object o) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        return gsonBuilder.setDateFormat("MM/dd/yy").create().toJson(o);
    }
}
