/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.stb.STBImageWrite.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * 3D Visual Realism Suite (Minggu 11).
 *
 * Kontrol:
 *   B  Phong <-> Blinn-Phong        F  Flat <-> Smooth shading
 *   Z  Z-Buffer ON/OFF              C  Back-Face Culling ON/OFF
 *   T  Tekstur ON/OFF               N  Filtering Nearest <-> Bilinear+Mipmap
 *   S  Shadow Mapping ON/OFF        [ ]  kurangi / tambah Shadow Bias
 *   M  Ganti model (Sphere/Torus/House)
 *   W  Wireframe                    SPACE  jeda/jalankan animasi cahaya
 *   V  VSync ON/OFF (untuk uji FPS) P  Simpan screenshot ke folder screenshots/
 *   Mouse kiri (drag) = orbit kamera, scroll = zoom, ESC = keluar
 */
public class App {
    private record ModelEntry(String name, Mesh mesh, float yOffset) {}

    private long window;
    private int fbW = 1280, fbH = 720;

    private ShaderProgram phong, depthShader;
    private ShadowMapFBO shadowFBO;
    private Texture2D brickTex, floorTex;
    private Mesh ground;
    private final List<ModelEntry> models = new ArrayList<>();
    private int currentModel = 0;
    private final Camera camera = new Camera();

    // ---- State fitur ----
    private boolean blinnPhong = true;    // Key B  (Indikator 1)
    private boolean smoothShading = true; // Key F  (Indikator 2)
    private boolean zBuffer = true;       // Key Z  (Indikator 3)
    private boolean backFaceCull = true;  // Key C  (Indikator 3)
    private boolean useTexture = true;    // Key T  (Indikator 4)
    private boolean mipmapped = true;     // Key N  (Indikator 4)
    private boolean shadows = true;       // Key S  (Indikator 5)
    private float biasMax = 0.005f;       // [ ]    (Indikator 5)

    private boolean animateLight = true, wireframe = false, vsync = true, screenshotRequested = false;
    private float lightAngle = 0.6f;
    private final Vector3f lightPos = new Vector3f();

    // Input mouse
    private boolean dragging = false;
    private double lastX, lastY;

    // FPS
    private double fpsTimer = 0;
    private int fpsFrames = 0;
    private double fps = 0;

    public void run() {
        init();
        loop();
        cleanup();
    }

    // ------------------------------------------------------------------ init
    private void init() {
        TextureGenerator.generateIfMissing();

        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal inisialisasi GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_DEPTH_BITS, 24);   // Z-Buffer 24-bit (kurangi Z-fighting)

        window = glfwCreateWindow(fbW, fbH, "3D Visual Realism Suite", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Gagal membuat window GLFW");

        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glfwSwapInterval(1);

        int[] w = new int[1], h = new int[1];
        glfwGetFramebufferSize(window, w, h);
        fbW = w[0];
        fbH = h[0];

        glfwSetFramebufferSizeCallback(window, (win, nw, nh) -> { fbW = Math.max(1, nw); fbH = Math.max(1, nh); });
        glfwSetKeyCallback(window, (win, key, sc, action, mods) -> { if (action != GLFW_RELEASE) onKey(key, action); });
        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) dragging = (action == GLFW_PRESS);
        });
        glfwSetCursorPosCallback(window, (win, x, y) -> {
            if (dragging) camera.rotate((float) (-(x - lastX) * 0.4), (float) ((y - lastY) * 0.4));
            lastX = x;
            lastY = y;
        });
        glfwSetScrollCallback(window, (win, dx, dy) -> camera.zoom((float) dy));

        // ---- Shader, Texture, FBO ----
        phong = new ShaderProgram("shaders/phong.vert", "shaders/phong.frag");
        depthShader = new ShaderProgram("shaders/depth.vert", "shaders/depth.frag");
        brickTex = new Texture2D("textures/brick.png");
        floorTex = new Texture2D("textures/floor.png");
        shadowFBO = new ShadowMapFBO();

        phong.use();
        phong.setInt("textureSampler", 0);   // texture unit 0
        phong.setInt("shadowMap", 1);        // texture unit 1

        // ---- Mesh 3D (punya Vertex Normal + UV) ----
        models.add(new ModelEntry("Sphere", new Mesh(MeshData.sphere(1.0f, 48, 64)), 1.0f));
        models.add(new ModelEntry("Torus", new Mesh(MeshData.torus(1.2f, 0.45f, 64, 32)), 0.45f));
        models.add(new ModelEntry("House", new Mesh(MeshData.house()), 0.0f));
        ground = new Mesh(MeshData.plane(10f, 10f));

        printHelp();
    }

    private void printHelp() {
        System.out.println("""
                === 3D Visual Realism Suite ===
                B: Phong/Blinn-Phong | F: Flat/Smooth | Z: Z-Buffer | C: Back-Face Culling
                T: Texture | N: Nearest/Bilinear+Mipmap | S: Shadow | [ ]: Shadow Bias
                M: Model | W: Wireframe | SPACE: Light anim | V: VSync | P: Screenshot | ESC: Exit""");
    }

    // ----------------------------------------------------------------- input
    private void onKey(int key, int action) {
        boolean press = (action == GLFW_PRESS);
        switch (key) {
            case GLFW_KEY_ESCAPE -> { if (press) glfwSetWindowShouldClose(window, true); }
            case GLFW_KEY_B -> { if (press) blinnPhong = !blinnPhong; }
            case GLFW_KEY_F -> { if (press) smoothShading = !smoothShading; }
            case GLFW_KEY_Z -> { if (press) zBuffer = !zBuffer; }
            case GLFW_KEY_C -> { if (press) backFaceCull = !backFaceCull; }
            case GLFW_KEY_T -> { if (press) useTexture = !useTexture; }
            case GLFW_KEY_N -> {
                if (press) {
                    mipmapped = !mipmapped;
                    brickTex.setFilterMode(mipmapped);
                    floorTex.setFilterMode(mipmapped);
                }
            }
            case GLFW_KEY_S -> { if (press) shadows = !shadows; }
            case GLFW_KEY_LEFT_BRACKET -> biasMax = Math.max(0.00005f, biasMax / 1.25f);
            case GLFW_KEY_RIGHT_BRACKET -> biasMax = Math.min(0.2f, biasMax * 1.25f);
            case GLFW_KEY_M -> { if (press) currentModel = (currentModel + 1) % models.size(); }
            case GLFW_KEY_W -> { if (press) wireframe = !wireframe; }
            case GLFW_KEY_SPACE -> { if (press) animateLight = !animateLight; }
            case GLFW_KEY_V -> {
                if (press) {
                    vsync = !vsync;
                    glfwSwapInterval(vsync ? 1 : 0);
                }
            }
            case GLFW_KEY_P -> { if (press) screenshotRequested = true; }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ loop
    private void loop() {
        double last = glfwGetTime();
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - last);
            last = now;

            if (animateLight) lightAngle += dt * 0.6f;
            lightPos.set((float) Math.cos(lightAngle) * 6f, 7f, (float) Math.sin(lightAngle) * 6f);

            render();

            if (screenshotRequested) {
                saveScreenshot();
                screenshotRequested = false;
            }
            glfwSwapBuffers(window);
            glfwPollEvents();
            updateFps(now);
        }
    }

    // ---------------------------------------------------------------- render
    private void render() {
        ModelEntry model = models.get(currentModel);
        Matrix4f modelMat = new Matrix4f().translate(0f, model.yOffset(), 0f);
        Matrix4f groundMat = new Matrix4f();

        // Matriks ruang cahaya: ortografik dari lightPos menuju pusat scene
        Matrix4f lightProj = new Matrix4f().ortho(-12f, 12f, -12f, 12f, 1f, 30f);
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        Matrix4f lightSpace = new Matrix4f(lightProj).mul(lightView);

        // ===== PASS 1: Depth Pass dari sudut pandang cahaya =====
        if (shadows) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);   // shadow map selalu solid
            HSRManager.setupDepthTest(true);             // depth test wajib aktif pada pass ini
            HSRManager.setupBackFaceCulling(backFaceCull);
            depthShader.use();
            depthShader.setMat4("lightSpaceMatrix", lightSpace);
            shadowFBO.bindForWriting();
            depthShader.setMat4("model", modelMat);
            model.mesh().draw(true);                     // hanya objek yang melempar bayangan
            shadowFBO.unbind(fbW, fbH);                  // lantai hanya menerima bayangan
        }

        // ===== PASS 2: Lighting Pass dari sudut pandang kamera =====
        glClearColor(0.53f, 0.68f, 0.85f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        HSRManager.setupDepthTest(zBuffer);              // Key Z
        HSRManager.setupBackFaceCulling(backFaceCull);   // Key C
        glPolygonMode(GL_FRONT_AND_BACK, wireframe ? GL_LINE : GL_FILL);

        Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(45.0), (float) fbW / fbH, 0.5f, 100f);
        Matrix4f view = camera.viewMatrix();

        phong.use();
        phong.setMat4("projection", proj);
        phong.setMat4("view", view);
        phong.setMat4("lightSpaceMatrix", lightSpace);
        phong.setVec3("lightPos", lightPos);
        phong.setVec3("viewPos", camera.position());
        phong.setVec3("lightColor", 1f, 1f, 1f);
        phong.setBool("useBlinnPhong", blinnPhong);
        phong.setBool("useShadow", shadows);
        phong.setFloat("biasMax", biasMax);
        phong.setFloat("biasMin", biasMax * 0.1f);
        shadowFBO.bindForReading(1);

        // Lantai
        phong.setMat4("model", groundMat);
        phong.setBool("useTexture", useTexture);
        phong.setVec3("objectColor", 0.6f, 0.62f, 0.65f);
        phong.setFloat("specularStrength", 0.15f);
        floorTex.bind(0);
        ground.draw(true);

        // Objek utama
        phong.setMat4("model", modelMat);
        phong.setBool("useTexture", useTexture);
        phong.setVec3("objectColor", 0.85f, 0.35f, 0.25f);
        phong.setFloat("specularStrength", 0.8f);
        brickTex.bind(0);
        model.mesh().draw(smoothShading);                // Key F

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    }

    // ------------------------------------------------------------ util / FPS
    private void updateFps(double now) {
        fpsFrames++;
        if (now - fpsTimer >= 0.5) {
            fps = fpsFrames / (now - fpsTimer);
            fpsFrames = 0;
            fpsTimer = now;
            glfwSetWindowTitle(window, String.format(
                    "3D Visual Realism | FPS %.0f | %s | %s | %s | Z:%s Cull:%s | Tex:%s | Shadow:%s bias=%.4f | VSync:%s",
                    fps, models.get(currentModel).name(),
                    blinnPhong ? "Blinn-Phong" : "Phong",
                    smoothShading ? "Smooth" : "Flat",
                    zBuffer ? "ON" : "OFF", backFaceCull ? "ON" : "OFF",
                    !useTexture ? "OFF" : (mipmapped ? "Bilinear+Mip" : "Nearest"),
                    shadows ? "ON" : "OFF", biasMax, vsync ? "ON" : "OFF"));
        }
    }

    private void saveScreenshot() {
        try {
            Files.createDirectories(Path.of("screenshots"));
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String tag = String.join("_",
                    models.get(currentModel).name().toLowerCase(),
                    blinnPhong ? "blinn" : "phong",
                    smoothShading ? "smooth" : "flat",
                    "z" + (zBuffer ? 1 : 0), "cull" + (backFaceCull ? 1 : 0),
                    !useTexture ? "notex" : (mipmapped ? "mip" : "nearest"),
                    "shadow" + (shadows ? 1 : 0));
            String file = "screenshots/shot_" + stamp + "_" + tag + ".png";

            ByteBuffer buf = MemoryUtil.memAlloc(fbW * fbH * 3);
            glPixelStorei(GL_PACK_ALIGNMENT, 1);
            glReadBuffer(GL_BACK);
            glReadPixels(0, 0, fbW, fbH, GL_RGB, GL_UNSIGNED_BYTE, buf);
            stbi_flip_vertically_on_write(true);
            boolean ok = stbi_write_png(file, fbW, fbH, 3, buf, fbW * 3);
            MemoryUtil.memFree(buf);
            System.out.println(ok ? "Screenshot disimpan: " + file : "Gagal menyimpan screenshot");
        } catch (IOException e) {
            System.err.println("Gagal membuat folder screenshots: " + e.getMessage());
        }
    }

    private void cleanup() {
        for (ModelEntry m : models) m.mesh().delete();
        ground.delete();
        brickTex.delete();
        floorTex.delete();
        shadowFBO.delete();
        phong.delete();
        depthShader.delete();
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        var cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }
}