package json;

import java.lang.annotation.*;

/**
 * annotation to exclude from json conversion. for example,
 * this is used to avoid circular reference, or lighten up json payload
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface JsonExclude {
}
