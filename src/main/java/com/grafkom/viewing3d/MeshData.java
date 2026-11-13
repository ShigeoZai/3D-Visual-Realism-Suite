/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.grafkom.viewing3d;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Data mesh di sisi CPU: posisi, koordinat UV, dan indeks segitiga.
 *   - Smooth : vertex normal averaging  N_v = normalize( sum N_face_i )
 *   - Flat   : satu normal per face (vertex diduplikasi per segitiga)
 */
public class MeshData {
    private final List<Float> pos = new ArrayList<>();
    private final List<Float> uv  = new ArrayList<>();
    private final List<Integer> idx = new ArrayList<>();

    public int addVertex(float x, float y, float z, float u, float v) {
        pos.add(x); pos.add(y); pos.add(z);
        uv.add(u);  uv.add(v);
        return pos.size() / 3 - 1;
    }

    public void addTriangle(int a, int b, int c) { idx.add(a); idx.add(b); idx.add(c); }

    public void addQuad(float[] p0, float[] p1, float[] p2, float[] p3, float uMax, float vMax) {
        int a = addVertex(p0[0], p0[1], p0[2], 0, 0);
        int b = addVertex(p1[0], p1[1], p1[2], uMax, 0);
        int c = addVertex(p2[0], p2[1], p2[2], uMax, vMax);
        int d = addVertex(p3[0], p3[1], p3[2], 0, vMax);
        addTriangle(a, b, c);
        addTriangle(a, c, d);
    }

    public int vertexCount()   { return pos.size() / 3; }
    public int triangleCount() { return idx.size() / 3; }

    private Vector3f p(int i) { return new Vector3f(pos.get(3 * i), pos.get(3 * i + 1), pos.get(3 * i + 2)); }

    /** Memastikan setiap segitiga berwinding CCW bila dilihat dari luar (agar Back-Face Culling benar). */
    public void orient(UnaryOperator<Vector3f> outward) {
        for (int t = 0; t < idx.size(); t += 3) {
            Vector3f a = p(idx.get(t)), b = p(idx.get(t + 1)), c = p(idx.get(t + 2));
            Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
            Vector3f centroid = new Vector3f(a).add(b).add(c).div(3f);
            if (n.dot(outward.apply(centroid)) < 0f) {
                int tmp = idx.get(t + 1);
                idx.set(t + 1, idx.get(t + 2));
                idx.set(t + 2, tmp);
            }
        }
    }

    /** Vertex Normal Averaging. Vertex pada posisi sama (mis. seam UV) digabung agar tidak ada retakan. */
    public float[] computeSmoothNormals() {
        int n = vertexCount();
        Map<String, Vector3f> acc = new HashMap<>();
        String[] keys = new String[n];
        for (int i = 0; i < n; i++) {
            Vector3f q = p(i);
            keys[i] = Math.round(q.x * 1e4f) + "," + Math.round(q.y * 1e4f) + "," + Math.round(q.z * 1e4f);
            acc.putIfAbsent(keys[i], new Vector3f());
        }
        for (int t = 0; t < idx.size(); t += 3) {
            int i0 = idx.get(t), i1 = idx.get(t + 1), i2 = idx.get(t + 2);
            Vector3f a = p(i0), b = p(i1), c = p(i2);
            Vector3f fn = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
            acc.get(keys[i0]).add(fn);
            acc.get(keys[i1]).add(fn);
            acc.get(keys[i2]).add(fn);
        }
        float[] normals = new float[n * 3];
        for (int i = 0; i < n; i++) {
            Vector3f s = new Vector3f(acc.get(keys[i]));
            if (s.lengthSquared() > 1e-12f) s.normalize(); else s.set(0, 1, 0);
            normals[3 * i] = s.x; normals[3 * i + 1] = s.y; normals[3 * i + 2] = s.z;
        }
        return normals;
    }

    /** Interleaved [pos3, normal3, uv2] per vertex untuk Smooth Shading (indexed). */
    public float[] interleavedSmooth() {
        float[] nrm = computeSmoothNormals();
        int n = vertexCount();
        float[] out = new float[n * 8];
        for (int i = 0; i < n; i++) {
            out[8 * i]     = pos.get(3 * i);
            out[8 * i + 1] = pos.get(3 * i + 1);
            out[8 * i + 2] = pos.get(3 * i + 2);
            out[8 * i + 3] = nrm[3 * i];
            out[8 * i + 4] = nrm[3 * i + 1];
            out[8 * i + 5] = nrm[3 * i + 2];
            out[8 * i + 6] = uv.get(2 * i);
            out[8 * i + 7] = uv.get(2 * i + 1);
        }
        return out;
    }

    /** Interleaved untuk Flat Shading: 3 vertex unik per segitiga dengan normal face yang sama. */
    public float[] interleavedFlat() {
        int tris = triangleCount();
        float[] out = new float[tris * 3 * 8];
        int o = 0;
        for (int t = 0; t < idx.size(); t += 3) {
            int[] id = { idx.get(t), idx.get(t + 1), idx.get(t + 2) };
            Vector3f a = p(id[0]), b = p(id[1]), c = p(id[2]);
            Vector3f fn = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
            if (fn.lengthSquared() > 1e-12f) fn.normalize(); else fn.set(0, 1, 0);
            for (int k = 0; k < 3; k++) {
                Vector3f q = p(id[k]);
                out[o++] = q.x; out[o++] = q.y; out[o++] = q.z;
                out[o++] = fn.x; out[o++] = fn.y; out[o++] = fn.z;
                out[o++] = uv.get(2 * id[k]); out[o++] = uv.get(2 * id[k] + 1);
            }
        }
        return out;
    }

    public int[] indices() { return idx.stream().mapToInt(Integer::intValue).toArray(); }

    // ====================== Generator Model ======================

    /** UV Sphere, pusat di (0,0,0). */
    public static MeshData sphere(float r, int stacks, int sectors) {
        MeshData m = new MeshData();
        for (int i = 0; i <= stacks; i++) {
            double phi = Math.PI / 2 - i * Math.PI / stacks;
            for (int j = 0; j <= sectors; j++) {
                double th = j * 2 * Math.PI / sectors;
                float x = (float) (r * Math.cos(phi) * Math.cos(th));
                float y = (float) (r * Math.sin(phi));
                float z = (float) (r * Math.cos(phi) * Math.sin(th));
                m.addVertex(x, y, z, 2f * j / sectors, 1f - (float) i / stacks);
            }
        }
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < sectors; j++) {
                int k1 = i * (sectors + 1) + j, k2 = k1 + sectors + 1;
                if (i != 0)          m.addTriangle(k1, k1 + 1, k2);
                if (i != stacks - 1) m.addTriangle(k1 + 1, k2 + 1, k2);
            }
        }
        m.orient(c -> c);
        return m;
    }

    /** Torus berbaring pada bidang XZ, pusat di (0,0,0). */
    public static MeshData torus(float R, float r, int rings, int sides) {
        MeshData m = new MeshData();
        for (int i = 0; i <= rings; i++) {
            double th = i * 2 * Math.PI / rings;
            for (int j = 0; j <= sides; j++) {
                double ph = j * 2 * Math.PI / sides;
                float x = (float) ((R + r * Math.cos(ph)) * Math.cos(th));
                float y = (float) (r * Math.sin(ph));
                float z = (float) ((R + r * Math.cos(ph)) * Math.sin(th));
                m.addVertex(x, y, z, 3f * i / rings, (float) j / sides);
            }
        }
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < sides; j++) {
                int a = i * (sides + 1) + j, b = (i + 1) * (sides + 1) + j;
                int c = a + 1, d = b + 1;
                m.addTriangle(a, c, b);
                m.addTriangle(b, c, d);
            }
        }
        final float RR = R;
        m.orient(p -> {
            Vector3f ring = new Vector3f(p.x, 0, p.z);
            if (ring.lengthSquared() > 1e-9f) ring.normalize(RR);
            return new Vector3f(p).sub(ring);
        });
        return m;
    }

    /** Rumah 3D: badan kotak + atap pelana. Alas pada y = 0. */
    public static MeshData house() {
        MeshData m = new MeshData();
        float w = 1.0f, d = 0.8f, h = 1.2f, ridge = 2.0f;
        m.addQuad(new float[]{-w, 0,  d}, new float[]{ w, 0,  d}, new float[]{ w, h,  d}, new float[]{-w, h,  d}, 2f, 1f);
        m.addQuad(new float[]{ w, 0, -d}, new float[]{-w, 0, -d}, new float[]{-w, h, -d}, new float[]{ w, h, -d}, 2f, 1f);
        m.addQuad(new float[]{-w, 0, -d}, new float[]{-w, 0,  d}, new float[]{-w, h,  d}, new float[]{-w, h, -d}, 1.6f, 1f);
        m.addQuad(new float[]{ w, 0,  d}, new float[]{ w, 0, -d}, new float[]{ w, h, -d}, new float[]{ w, h,  d}, 1.6f, 1f);
        m.addQuad(new float[]{-w, 0, -d}, new float[]{ w, 0, -d}, new float[]{ w, 0,  d}, new float[]{-w, 0,  d}, 2f, 1.6f);
        m.addQuad(new float[]{-w, h,  d}, new float[]{ 0, ridge,  d}, new float[]{ 0, ridge, -d}, new float[]{-w, h, -d}, 1f, 1.6f);
        m.addQuad(new float[]{ 0, ridge,  d}, new float[]{ w, h,  d}, new float[]{ w, h, -d}, new float[]{ 0, ridge, -d}, 1f, 1.6f);
        int a = m.addVertex(-w, h, d, 0f, 0f), b = m.addVertex(w, h, d, 2f, 0f), c = m.addVertex(0, ridge, d, 1f, 0.8f);
        m.addTriangle(a, b, c);
        int e = m.addVertex(w, h, -d, 0f, 0f), f = m.addVertex(-w, h, -d, 2f, 0f), g = m.addVertex(0, ridge, -d, 1f, 0.8f);
        m.addTriangle(e, f, g);
        final Vector3f center = new Vector3f(0, 0.9f, 0);
        m.orient(p -> new Vector3f(p).sub(center));
        return m;
    }

    /** Bidang lantai ukuran 2*half, UV diulang 'repeat' kali. Normal menghadap +Y. */
    public static MeshData plane(float half, float repeat) {
        MeshData m = new MeshData();
        m.addQuad(new float[]{-half, 0, -half}, new float[]{ half, 0, -half},
                  new float[]{ half, 0,  half}, new float[]{-half, 0,  half}, repeat, repeat);
        m.orient(p -> new Vector3f(0, 1, 0));
        return m;
    }
}