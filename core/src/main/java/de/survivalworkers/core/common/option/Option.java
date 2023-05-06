package de.survivalworkers.core.common.option;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Option {
    String defaultValue() default "";
}
