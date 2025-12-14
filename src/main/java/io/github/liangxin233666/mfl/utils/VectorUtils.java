package io.github.liangxin233666.mfl.utils;

import java.util.List;

public class VectorUtils {
    // 维度常量，与Gemini模型保持一致
    private static final int DIMENSIONS = 768;

    /**
     * 计算一组向量的平均值（重心）
     */
    public static float[] calculateCentroid(List<float[]> vectors) {
        if (vectors == null || vectors.isEmpty()) {
            return new float[DIMENSIONS]; // 空则返回0，没办法
        }

        float[] result = new float[DIMENSIONS];
        int count = vectors.size();

        // 1. 累加
        for (float[] vector : vectors) {
            // 防御性检查维度，万一混进了其他模型的向量
            if (vector.length != DIMENSIONS) continue;
            for (int i = 0; i < DIMENSIONS; i++) {
                result[i] += vector[i];
            }
        }

        // 2. 求平均
        for (int i = 0; i < DIMENSIONS; i++) {
            result[i] = result[i] / count;
        }

        return result;
    }
}