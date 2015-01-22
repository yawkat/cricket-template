/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.cricket.template;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field or method that will be used for filling in a template.
 *
 * @author yawkat
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface Parameter {
    /**
     * The variable this field or method should represent.
     */
    String value() default "";

    /**
     * Whether to do a "flat" map, meaning that, during serialization, this objects' members will be included
     * current-level:
     *
     * <code>
     * class A {
     *     @Parameter(flat=true)
     *     B child;
     * }
     *
     * class B {
     *     @Parameter String m1;
     *     @Parameter String m2;
     * }
     * </code>
     *
     * Objects of type A will lead to:
     *
     * <code>
     * {
     *     "m1": // m1,
     *     "m2": // m2,
     * }
     * </code>
     *
     * while without this property they would serialize to:
     *
     * <code>
     * {
     *     "child": {
     *         "m1": // m1,
     *         "m2": // m2,
     *     }
     * }
     * </code>
     */
    boolean flat() default false;
}
