/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Kamera orbit sederhana: drag mouse = putar, scroll = zoom. */
public class Camera {
    private float yaw = 35f, pitch = 28f, distance = 10f;
    private final Vector3f target = new Vector3f(0f, 0.8f, 0f);

    public void rotate(float dxDeg, float dyDeg) {
        yaw += dxDeg;
        pitch = Math.max(-5f, Math.min(88f, pitch + dyDeg));
    }

    public void zoom(float delta) {
        distance = Math.max(3f, Math.min(30f, distance - delta));
    }

    public Vector3f position() {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new Vector3f(
                target.x + distance * (float) (Math.cos(p) * Math.sin(y)),
                target.y + distance * (float) Math.sin(p),
                target.z + distance * (float) (Math.cos(p) * Math.cos(y)));
    }

    public Matrix4f viewMatrix() {
        return new Matrix4f().lookAt(position(), target, new Vector3f(0, 1, 0));
    }
}
