package de.survivalworks.core.client.engine.vk.scene;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Camera {
    private Vector3f direction;
    private Vector3f position;
    private Vector3f right;
    private Vector2f rotation;
    private Vector3f up;
    private Matrix4f viewMatrix;

    public Camera(){
        direction = new Vector3f();
        position = new Vector3f();
        right = new Vector3f();
        rotation = new Vector2f();
        up = new Vector3f();
        viewMatrix = new Matrix4f();
    }

    public void addRotation(float x,float y){
        rotation.add(x,y);
        recalculate();
    }



    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    public void moveBack(float i){
        viewMatrix.positiveZ(direction).negate().mul(i);
        position.sub(direction);
        recalculate();
    }

    public void moveDown(float i) {
        viewMatrix.positiveY(up).mul(i);
        position.sub(up);
        recalculate();
    }

    public void moveForward(float i){
        viewMatrix.positiveZ(direction).negate().mul(i);
        position.add(direction);
        recalculate();
    }

    public void moveLeft(float i){
        viewMatrix.positiveX(right).mul(i);
        position.sub(right);
        recalculate();
    }

    public void moveRight(float i){
        viewMatrix.positiveX(right).mul(i);
        position.add(right);
        recalculate();
    }

    public void moveUp(float i){
        viewMatrix.positiveY(up).mul(i);
        position.add(up);
        recalculate();
    }

    private void recalculate() {
        viewMatrix.identity().rotateX(rotation.x).rotateY(rotation.y).translate(-position.x, -position.y, -position.z);
    }

    public void setPos(float x,float y,float z){
        position.set(x,y,z);
        recalculate();
    }

    public void setRotation(float x,float y){
        rotation.set(x,y);
        recalculate();
    }
}
