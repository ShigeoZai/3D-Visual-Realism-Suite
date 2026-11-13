/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL30.*;

/** Mesh di GPU: VAO smooth (indexed) dan VAO flat (non-indexed). */
public class Mesh {
    private static final int STRIDE = 8 * Float.BYTES;   // pos3 + normal3 + uv2

    private final int vaoSmooth, vboSmooth, ebo, smoothCount;
    private final int vaoFlat, vboFlat, flatCount;

    public Mesh(MeshData data) {
        float[] smooth = data.interleavedSmooth();
        int[] indices = data.indices();
        smoothCount = indices.length;

        vaoSmooth = glGenVertexArrays();
        glBindVertexArray(vaoSmooth);
        vboSmooth = uploadVertices(smooth);
        setupAttributes();
        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer ib = MemoryUtil.memAllocInt(indices.length);
        ib.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);
        MemoryUtil.memFree(ib);

        float[] flat = data.interleavedFlat();
        flatCount = flat.length / 8;
        vaoFlat = glGenVertexArrays();
        glBindVertexArray(vaoFlat);
        vboFlat = uploadVertices(flat);
        setupAttributes();

        glBindVertexArray(0);
    }

    private static int uploadVertices(float[] v) {
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer fb = MemoryUtil.memAllocFloat(v.length);
        fb.put(v).flip();
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
        MemoryUtil.memFree(fb);
        return vbo;
    }

    private static void setupAttributes() {
        glEnableVertexAttribArray(0);   // aPos
        glVertexAttribPointer(0, 3, GL_FLOAT, false, STRIDE, 0L);
        glEnableVertexAttribArray(1);   // aNormal
        glVertexAttribPointer(1, 3, GL_FLOAT, false, STRIDE, 3L * Float.BYTES);
        glEnableVertexAttribArray(2);   // aTexCoords
        glVertexAttribPointer(2, 2, GL_FLOAT, false, STRIDE, 6L * Float.BYTES);
    }

    public void draw(boolean smooth) {
        if (smooth) {
            glBindVertexArray(vaoSmooth);
            glDrawElements(GL_TRIANGLES, smoothCount, GL_UNSIGNED_INT, 0L);
        } else {
            glBindVertexArray(vaoFlat);
            glDrawArrays(GL_TRIANGLES, 0, flatCount);
        }
        glBindVertexArray(0);
    }

    public void delete() {
        glDeleteVertexArrays(vaoSmooth); glDeleteVertexArrays(vaoFlat);
        glDeleteBuffers(vboSmooth); glDeleteBuffers(vboFlat); glDeleteBuffers(ebo);
    }
}