package at.yawk.cricket.template;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a class to have all its methods and / or fields serialized, ignoring @Parameter annotations.
 *
 * @author yawkat
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ParameterClass {
    boolean fields() default false;

    boolean methods() default false;
}
