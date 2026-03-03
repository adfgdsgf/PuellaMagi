package com.v2t.puellamagi.util;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * 世界坐标 →屏幕坐标 投影工具
 *
 * 投影原理：
 * 1. Camera方向向量将世界坐标转到摄像机空间（每帧实时，保证正确）
 * 2. 缓存的projection矩阵提供FOV参数（从RenderLevelStageEvent获取）
 * 3. 两者结合计算屏幕坐标
 *
 * 不依赖PoseStack（在不同渲染阶段可能被修改），避免"黏屏"问题
 *
 * 复用场景：队友头像HUD、技能瞄准系统、标记系统等
 */
public final class 投影工具 {

    private 投影工具() {}

    //==================== 矩阵缓存 ====================

    /** 缓存的投影矩阵（仅用于提取FOV参数） */
    private static Matrix4f 缓存投影 = new Matrix4f();
    private static boolean 矩阵有效 = false;

    /**
     * 在世界渲染阶段调用，缓存投影矩阵
     * 由RenderLevelStageEvent(AFTER_SKY) 触发
     *
     * 只缓存投影矩阵，view变换由Camera方向向量实时计算
     *
     * @param projection 投影矩阵（包含正确的动态FOV）
     */
    public static void 更新投影矩阵(Matrix4f projection) {
        缓存投影 = new Matrix4f(projection);
        矩阵有效 = true;
    }

    /**
     * 投影矩阵是否可用（至少渲染过一帧）
     */
    public static boolean 矩阵是否有效() {
        return 矩阵有效;
    }

    // ==================== 投影计算 ====================

    /**
     * 投影结果
     *
     * @param screenX     屏幕X坐标
     * @param screenY     屏幕Y坐标
     * @param 在视野内    是否在摄像机前方且在屏幕范围内
     * @param 在摄像机前方 是否在摄像机前方
     */
    public record 投影结果(float screenX, float screenY, boolean 在视野内, boolean 在摄像机前方) {}

    /**
     * 将世界坐标投影到屏幕坐标
     *
     * 使用Camera方向向量做view变换（实时准确），
     * 从缓存的projection矩阵提取FOV参数做透视投影
     *
     * @param worldX       世界坐标X
     * @param worldY       世界坐标Y
     * @param worldZ       世界坐标Z
     * @param screenWidth  屏幕宽度（GUI缩放后）
     * @param screenHeight 屏幕高度（GUI缩放后）
     * @return 投影结果，null表示无法计算
     */
    @Nullable
    public static 投影结果 投影到屏幕(double worldX, double worldY, double worldZ,int screenWidth, int screenHeight) {
        if (!矩阵有效) return null;

        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null) return null;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        // 相对摄像机的偏移
        double relX = worldX - cameraPos.x;
        double relY = worldY - cameraPos.y;
        double relZ = worldZ - cameraPos.z;

        // 摄像机坐标系基向量
        Vector3f forward = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();

        // 投影到摄像机坐标系
        // d = 沿前方向的距离（正值= 在摄像机前方）
        double d = relX * forward.x() + relY * forward.y() + relZ * forward.z();
        // r = 沿右方向的距离（right = -left）
        double r = -(relX * left.x() + relY * left.y() + relZ * left.z());
        // u = 沿上方向的距离
        double u = relX * up.x() + relY * up.y() + relZ * up.z();

        boolean 在前方 = d > 0;

        // 避免除零
        if (Math.abs(d) < 0.001) d = 0.001;

        // 从projection矩阵提取FOV参数
        // m00 = 1/(aspect * tan(fov/2))
        // m11 = 1/tan(fov/2)
        float p00 = 缓存投影.m00();
        float p11 = 缓存投影.m11();

        // 计算NDC坐标
        float ndcX = (float) (r / d * p00);
        float ndcY = (float) (u / d * p11);

        // NDC → 屏幕坐标
        float screenX = (ndcX + 1.0f) / 2.0f * screenWidth;
        float screenY = (1.0f - ndcY) / 2.0f * screenHeight;

        // 判断是否在屏幕范围内
        boolean 在范围内 = 在前方&& screenX >= 0 && screenX <= screenWidth
                && screenY >= 0 && screenY <= screenHeight;

        return new 投影结果(screenX, screenY, 在范围内, 在前方);
    }

    // ==================== 边缘钉住 ====================

    /**
     * 将屏幕外的点钉到屏幕边缘
     *
     * @param screenX      原始屏幕X（可能超出范围）
     * @param screenY      原始屏幕Y（可能超出范围）
     * @param 在摄像机前方 是否在前方
     * @param screenWidth  屏幕宽度
     * @param screenHeight 屏幕高度
     * @param 边距 边缘留白像素
     * @return [clampedX, clampedY]
     */
    public static float[] 钉到屏幕边缘(float screenX, float screenY,boolean 在摄像机前方,
                                       int screenWidth, int screenHeight,
                                       int 边距) {
        float centerX = screenWidth / 2.0f;
        float centerY = screenHeight / 2.0f;

        float dirX = screenX - centerX;
        float dirY = screenY - centerY;

        // 在摄像机后方时翻转方向
        if (!在摄像机前方) {
            dirX = -dirX;
            dirY = -dirY;
        }

        // 避免零向量
        if (Math.abs(dirX) < 0.01f && Math.abs(dirY) < 0.01f) {
            dirX = 0;
            dirY = -1;
        }

        // 计算到边缘的缩放比例
        float minX = 边距;
        float maxX = screenWidth - 边距;
        float minY = 边距;
        float maxY = screenHeight - 边距;

        float scale = Float.MAX_VALUE;

        if (dirX != 0) {
            float sx = dirX > 0 ? (maxX - centerX) / dirX : (minX - centerX) / dirX;
            scale = Math.min(scale, sx);
        }
        if (dirY != 0) {
            float sy = dirY > 0 ? (maxY - centerY) / dirY : (minY - centerY) / dirY;
            scale = Math.min(scale, sy);
        }

        scale = Math.max(0, scale);

        return new float[]{
                centerX + dirX * scale,
                centerY + dirY * scale
        };
    }
}
