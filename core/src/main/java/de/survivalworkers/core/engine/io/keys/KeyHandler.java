package de.survivalworkers.core.engine.io.keys;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used for methods in Classes implementing {@link KeyListener} to get called whenever the key is pressed or released<br>
 * A method using this annotation needs to have exactly one Parameter of type boolean which is true when the key is pressed and false when the key is released
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface KeyHandler {
    String value();
}
