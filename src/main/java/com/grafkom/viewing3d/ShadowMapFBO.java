/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL30.*;   // GL30 mewarisi GL11 & GL13 (GL_CLAMP_TO_BORDER)

/** Praktikum 4: Framebuffer Object berisi Depth Texture sebagai Shadow Map. */
public class ShadowMapFBO {
    private int fboID;
    private int depthTextureID;
    private final int SHADOW_WIDTH = 2048, SHADOW_HEIGHT = 2048;

    public ShadowMapFBO() {
        fboID = glGenFramebuffers();
        depthTextureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTextureID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, SHADOW_WIDTH, SHADOW_HEIGHT, 0,
                GL_DEPTH_COMPONENT, GL_FLOAT, (java.nio.ByteBuffer) null);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        float[] borderColor = { 1.0f, 1.0f, 1.0f, 1.0f };   // di luar shadow map = terang
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);

        glBindFramebuffer(GL_FRAMEBUFFER, fboID);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTextureID, 0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Shadow FBO tidak lengkap!");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void bindForWriting() {
        glViewport(0, 0, SHADOW_WIDTH, SHADOW_HEIGHT);
        glBindFramebuffer(GL_FRAMEBUFFER, fboID);
        glClear(GL_DEPTH_BUFFER_BIT);
    }

    public void unbind(int screenWidth, int screenHeight) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, screenWidth, screenHeight);
    }

    /** Mengikat depth map ke texture unit tertentu untuk dibaca di Fragment Shader. */
    public void bindForReading(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, depthTextureID);
    }

    public int getDepthTextureID() { return depthTextureID; }
    public int getWidth()  { return SHADOW_WIDTH; }
    public int getHeight() { return SHADOW_HEIGHT; }

    public void delete() {
        glDeleteFramebuffers(fboID);
        glDeleteTextures(depthTextureID);
    }
}
