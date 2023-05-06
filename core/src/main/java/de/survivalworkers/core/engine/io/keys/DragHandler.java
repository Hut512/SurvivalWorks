package de.survivalworkers.core.engine.io.keys;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used for methods in Classes implementing {@link MouseDragListener} to get called whenever the mouse is dragged<br>
 * The method annotated has deltaX,deltaY,xPos and yPos as parameters which need to be of type float
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DragHandler {
    String value();
}
