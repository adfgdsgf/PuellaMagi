// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/hud/HUD布局数据.java

package com.v2t.puellamagi.client.gui.hud;

import com.v2t.puellamagi.client.客户端状态管理;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.function.BiFunction;

/**
 * HUD布局数据
 *
 * 通用的HUD布局存储类，包含位置、方向、缩放
 * 支持延迟计算的默认位置（解决需要屏幕尺寸的问题）
 */
public class HUD布局数据 {

    private final String hudId;
    private final boolean 支持方向;
    private final boolean 支持缩放;
    private final float 最小缩放, 最大缩放;

    // 默认位置计算器：(屏幕宽, 屏幕高) -> [x, y]
    private final BiFunction<Integer, Integer, int[]> 默认位置计算器;

    // 当前值
    private int x = -1, y = -1;
    private I可编辑HUD.HUD方向 方向 = I可编辑HUD.HUD方向.横向;
    private float 缩放 = 1.0f;

    // 编辑模式备份
    private int 备份X, 备份Y;
    private I可编辑HUD.HUD方向 备份方向;
    private float 备份缩放;

    // 是否已从存储加载
    private boolean 已加载 = false;

    // ==================== 构造器 ====================

    /**
     * 固定默认位置（兼容旧用法）
     */
    public HUD布局数据(String hudId, int defaultX, int defaultY,boolean supportDirection, boolean supportScale,
                       float minScale, float maxScale) {
        this(hudId, (sw, sh) -> new int[]{defaultX, defaultY},supportDirection, supportScale, minScale, maxScale);
    }

    /**
     * 动态计算默认位置（如居中）
     */
    public HUD布局数据(String hudId, BiFunction<Integer, Integer, int[]> defaultPosCalculator,
                       boolean supportDirection, boolean supportScale,
                       float minScale, float maxScale) {
        this.hudId = hudId;
        this.默认位置计算器 = defaultPosCalculator;
        this.支持方向 = supportDirection;
        this.支持缩放 = supportScale;
        this.最小缩放 = minScale;
        this.最大缩放 = maxScale;
    }

    // ==================== 延迟加载 ====================

    /**
     * 确保数据已加载
     */
    private void 确保已加载() {
        if (!已加载) {
            从存储加载();
            已加载 = true;
        }
    }

    // ==================== 位置 ====================

    public int 获取X() {
        确保已加载();
        return x;
    }

    public int 获取Y() {
        确保已加载();
        return y;
    }

    public void 设置位置(int x, int y) {
        确保已加载();
        this.x = x;
        this.y = y;
    }

    /**
     * 获取默认位置（需要屏幕尺寸时动态计算）
     */
    public int[] 获取默认位置() {
        Minecraft mc = Minecraft.getInstance();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        return 默认位置计算器.apply(screenW, screenH);
    }

    /**
     * 限制位置在屏幕内
     */
    public int[] 限制在屏幕内(int posX, int posY, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int clampedX = Math.max(0, Math.min(posX, screenW - width));
        int clampedY = Math.max(0, Math.min(posY, screenH - height));
        return new int[]{clampedX, clampedY};
    }

    // ==================== 方向 ====================

    public boolean 支持方向() {
        return 支持方向;
    }

    public I可编辑HUD.HUD方向 获取方向() {
        确保已加载();
        return 方向;
    }

    public void 设置方向(I可编辑HUD.HUD方向 dir) {
        if (支持方向) {
            确保已加载();
            this.方向 = dir;
        }
    }

    // ==================== 缩放 ====================

    public boolean 支持缩放() {
        return 支持缩放;
    }

    public float 获取缩放() {
        确保已加载();
        return 缩放;
    }

    public void 设置缩放(float scale) {
        if (支持缩放) {
            确保已加载();
            this.缩放 = Mth.clamp(scale, 最小缩放, 最大缩放);
        }
    }

    public float 获取最小缩放() {
        return 最小缩放;
    }

    public float 获取最大缩放() {
        return 最大缩放;
    }

    // ==================== 编辑模式 ====================

    public void 开始编辑() {
        确保已加载();
        备份X = x;
        备份Y = y;
        备份方向 = 方向;
        备份缩放 = 缩放;
    }

    public void 取消编辑() {
        x = 备份X;
        y = 备份Y;
        方向 = 备份方向;
        缩放 = 备份缩放;
    }

    public void 保存编辑() {
        保存到存储();
    }

    public void 重置为默认() {
        int[] defaultPos = 获取默认位置();
        x = defaultPos[0];
        y = defaultPos[1];
        方向 = I可编辑HUD.HUD方向.横向;
        缩放 = 1.0f;
    }

    // ==================== 持久化 ====================

    private void 从存储加载() {
        int[] pos = 客户端状态管理.获取HUD位置(hudId);
        if (pos != null) {
            x = pos[0];
            y = pos[1];
        } else {
            // 首次使用，计算默认位置
            int[] defaultPos = 获取默认位置();
            x = defaultPos[0];
            y = defaultPos[1];
        }

        if (支持方向) {
            String dirStr = 客户端状态管理.获取字符串("hud." + hudId + ".direction", "横向");
            方向 = "纵向".equals(dirStr) ? I可编辑HUD.HUD方向.纵向 : I可编辑HUD.HUD方向.横向;
        }

        if (支持缩放) {
            缩放 = 客户端状态管理.获取浮点("hud." + hudId + ".scale", 1.0f);
            缩放 = Mth.clamp(缩放, 最小缩放, 最大缩放);
        }
    }

    private void 保存到存储() {
        客户端状态管理.设置HUD位置(hudId, x, y);

        if (支持方向) {
            客户端状态管理.设置字符串("hud." + hudId + ".direction",
                    方向== I可编辑HUD.HUD方向.纵向 ? "纵向" : "横向");
        }

        if (支持缩放) {
            客户端状态管理.设置浮点("hud." + hudId + ".scale", 缩放);
        }
    }
}
