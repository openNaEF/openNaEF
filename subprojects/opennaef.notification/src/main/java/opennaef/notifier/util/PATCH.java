package opennaef.notifier.util;

import javax.ws.rs.HttpMethod;
import java.lang.annotation.*;

/**
 * Indicates that the annotated method responds to HTTP PATCH requests.
 *
 * @see HttpMethod
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("PATCH")
@Documented
public @interface PATCH {
}
