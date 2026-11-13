/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.grafkom.viewing3d;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/** Membuat brick.png dan floor.png bila belum ada. Bisa diganti dengan gambar sendiri. */
public class TextureGenerator {

    public static void generateIfMissing() {
        if (TextureGenerator.class.getClassLoader().getResource("textures/brick.png") != null
                && TextureGenerator.class.getClassLoader().getResource("textures/floor.png") != null) {
            return;
        }
        System.setProperty("java.awt.headless", "true");
        try {
            BufferedImage brick = makeBrick();
            BufferedImage floor = makeFloor();
            // Simpan ke folder sumber (agar ikut tersimpan di proyek) dan ke target/classes (agar langsung terbaca)
            for (String dir : new String[]{"src/main/resources/textures", "target/classes/textures"}) {
                File d = new File(dir);
                d.mkdirs();
                ImageIO.write(brick, "png", new File(d, "brick.png"));
                ImageIO.write(floor, "png", new File(d, "floor.png"));
            }
            System.out.println("Tekstur dibuat: brick.png dan floor.png");
        } catch (IOException e) {
            throw new RuntimeException("Gagal membuat tekstur", e);
        }
    }

    private static BufferedImage makeBrick() {
        int size = 512, bh = 64, bw = 128;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(190, 185, 175));                 // warna semen
        g.fillRect(0, 0, size, size);
        Random rnd = new Random(7);
        for (int r = 0; r < size / bh; r++) {
            int off = (r % 2 == 0) ? 0 : bw / 2;
            for (int c = -1; c <= size / bw; c++) {
                g.setColor(new Color(150 + rnd.nextInt(36), 65 + rnd.nextInt(21), 45 + rnd.nextInt(16)));
                g.fillRect(c * bw + off + 4, r * bh + 4, bw - 8, bh - 8);
            }
        }
        g.setColor(new Color(220, 40, 40));  g.fillRect(0, 0, 40, 40);            // kiri-atas = merah
        g.setColor(new Color(40, 60, 220));  g.fillRect(0, size - 40, 40, 40);    // kiri-bawah = biru
        g.dispose();
        return img;
    }

    private static BufferedImage makeFloor() {
        int size = 512, n = 8, s = size / n;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                g.setColor(((i + j) % 2 == 0) ? new Color(225, 225, 225) : new Color(95, 110, 130));
                g.fillRect(i * s, j * s, s, s);
            }
        }
        g.dispose();
        return img;
    }
}