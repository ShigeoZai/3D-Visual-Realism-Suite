/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/** Utilitas membaca file dari classpath (src/main/resources). */
public final class ResourceUtil {
    private ResourceUtil() {}

    public static byte[] readBytes(String path) {
        try (InputStream in = ResourceUtil.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IllegalArgumentException("Resource tidak ditemukan: " + path);
            return in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Gagal membaca resource: " + path, e);
        }
    }

    public static String readText(String path) {
        return new String(readBytes(path), StandardCharsets.UTF_8);
    }

    /** Hasil harus dibebaskan dengan MemoryUtil.memFree() oleh pemanggil. */
    public static ByteBuffer readToBuffer(String path) {
        byte[] bytes = readBytes(path);
        ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
        buf.put(bytes).flip();
        return buf;
    }
}