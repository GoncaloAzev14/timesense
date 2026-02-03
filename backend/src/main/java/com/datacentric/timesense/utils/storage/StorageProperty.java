package com.datacentric.timesense.utils.storage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to identify constructor parameters that should be read from
 * application properties
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface StorageProperty {
    /**
     * Property name in application properties
     */
    String value();

    /**
     * Default value for a missing property
     */
    String defaultValue() default "";
}
