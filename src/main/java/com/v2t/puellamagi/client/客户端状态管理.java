// 文件路径: src/main/java/com/v2t/puellamagi/client/客户端状态管理.java

package com.v2t.puellamagi.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.v2t.puellamagi.PuellaMagi;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 客户端UI状态持久化管理
 *
 * 职责：只管理需要持久化的UI偏好设置
 * - 技能栏折叠状态
 * - 最后使用的预设索引
 * - 编辑器标签页位置
 * - 编辑面板位置
 * - 各HUD位置
 *
 * 存储位置：config/puellamagi/client_state.json
 *
 * 注意：运行时状态（如蓄力进度）由蓄力状态管理处理
 */
public final class 客户端状态管理 {
    private 客户端状态管理() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path 配置目录 = FMLPaths.CONFIGDIR.get().resolve("puellamagi");
    private static final Path 配置文件 = 配置目录.resolve("client_state.json");

    private static JsonObject 状态数据 = new JsonObject();
    private static boolean 已加载 = false;

    // ==================== 初始化 ====================

    /**
     * 从文件加载状态（游戏启动时调用）
     */
    public static void 初始化() {
        if (已加载) return;

        try {
            if (!Files.exists(配置目录)) {
                Files.createDirectories(配置目录);
            }

            if (Files.exists(配置文件)) {
                String json = Files.readString(配置文件);
                状态数据 = GSON.fromJson(json, JsonObject.class);
                if (状态数据 == null) {
                    状态数据 = new JsonObject();
                }
                PuellaMagi.LOGGER.debug("客户端状态加载成功");
            } else {
                状态数据 = new JsonObject();
                PuellaMagi.LOGGER.debug("客户端状态文件不存在，使用默认值");
            }
        } catch (Exception e) {
            PuellaMagi.LOGGER.error("加载客户端状态失败", e);
            状态数据 = new JsonObject();}

        已加载 = true;
    }

    /**
     * 保存状态到文件
     */
    private static void 保存到文件() {
        try {
            if (!Files.exists(配置目录)) {
                Files.createDirectories(配置目录);
            }
            String json = GSON.toJson(状态数据);
            Files.writeString(配置文件, json);
        } catch (IOException e) {
            PuellaMagi.LOGGER.error("保存客户端状态失败", e);
        }
    }

    // ==================== 通用读写方法 ====================

    public static boolean 获取布尔(String key, boolean defaultValue) {
        确保已加载();
        if (状态数据.has(key) && 状态数据.get(key).isJsonPrimitive()) {
            return 状态数据.get(key).getAsBoolean();
        }
        return defaultValue;
    }

    public static void 设置布尔(String key, boolean value) {
        确保已加载();
        状态数据.addProperty(key, value);
        保存到文件();
    }

    public static int 获取整数(String key, int defaultValue) {
        确保已加载();
        if (状态数据.has(key) && 状态数据.get(key).isJsonPrimitive()) {
            return 状态数据.get(key).getAsInt();
        }
        return defaultValue;
    }

    public static void 设置整数(String key, int value) {
        确保已加载();
        状态数据.addProperty(key, value);
        保存到文件();
    }

    public static float 获取浮点(String key, float defaultValue) {
        确保已加载();
        if (状态数据.has(key) && 状态数据.get(key).isJsonPrimitive()) {
            return 状态数据.get(key).getAsFloat();
        }
        return defaultValue;
    }

    public static void 设置浮点(String key, float value) {
        确保已加载();
        状态数据.addProperty(key, value);
        保存到文件();
    }

    public static String 获取字符串(String key, String defaultValue) {
        确保已加载();
        if (状态数据.has(key) && 状态数据.get(key).isJsonPrimitive()) {
            return 状态数据.get(key).getAsString();
        }
        return defaultValue;
    }

    public static void 设置字符串(String key, String value) {
        确保已加载();
        状态数据.addProperty(key, value);
        保存到文件();
    }

    // ==================== 便捷方法（技能栏相关） ====================

    private static final String KEY_SKILL_BAR_COLLAPSED = "skill_bar.collapsed";
    private static final String KEY_SKILL_BAR_PRESET_INDEX = "skill_bar.preset_index";

    public static boolean 技能栏是否折叠() {
        return 获取布尔(KEY_SKILL_BAR_COLLAPSED, false);
    }

    public static void 设置技能栏折叠(boolean collapsed) {
        设置布尔(KEY_SKILL_BAR_COLLAPSED, collapsed);
    }

    public static int 获取上次预设索引() {
        return 获取整数(KEY_SKILL_BAR_PRESET_INDEX, 0);
    }

    public static void 设置上次预设索引(int index) {
        设置整数(KEY_SKILL_BAR_PRESET_INDEX, index);
    }

    // ==================== 便捷方法（编辑器相关） ====================

    private static final String KEY_EDITOR_TAB = "editor.last_tab";
    private static final String KEY_EDITOR_PANEL_X = "editor.panel_x";
    private static final String KEY_EDITOR_PANEL_Y = "editor.panel_y";

    public static int 获取编辑器标签页() {
        return 获取整数(KEY_EDITOR_TAB, 0);
    }

    public static void 设置编辑器标签页(int tab) {
        设置整数(KEY_EDITOR_TAB, tab);
    }

    public static int 获取编辑面板X() {
        return 获取整数(KEY_EDITOR_PANEL_X, -1);
    }

    public static int 获取编辑面板Y() {
        return 获取整数(KEY_EDITOR_PANEL_Y, -1);
    }

    public static void 设置编辑面板位置(int x, int y) {
        设置整数(KEY_EDITOR_PANEL_X, x);
        设置整数(KEY_EDITOR_PANEL_Y, y);
    }

    // ==================== 便捷方法（HUD位置相关） ====================

    private static final String KEY_HUD_PREFIX = "hud.";

    /**
     * 获取HUD位置
     * @param hudId HUD标识
     * @return [x, y] 或 null（如果未保存过）
     */
    @Nullable
    public static int[] 获取HUD位置(String hudId) {
        确保已加载();
        String keyX = KEY_HUD_PREFIX + hudId + ".x";
        String keyY = KEY_HUD_PREFIX + hudId + ".y";

        if (状态数据.has(keyX) && 状态数据.has(keyY)) {
            int x = 状态数据.get(keyX).getAsInt();
            int y = 状态数据.get(keyY).getAsInt();
            return new int[]{x, y};
        }
        return null;
    }

    /**
     * 设置HUD位置
     * @param hudId HUD标识
     * @param x X坐标
     * @param y Y坐标
     */
    public static void 设置HUD位置(String hudId, int x, int y) {
        确保已加载();
        String keyX = KEY_HUD_PREFIX + hudId + ".x";
        String keyY = KEY_HUD_PREFIX + hudId + ".y";
        状态数据.addProperty(keyX, x);
        状态数据.addProperty(keyY, y);
        保存到文件();
    }

    /**
     * 重置指定HUD位置（恢复默认）
     * @param hudId HUD标识
     */
    public static void 重置HUD位置(String hudId) {
        确保已加载();
        String keyX = KEY_HUD_PREFIX + hudId + ".x";
        String keyY = KEY_HUD_PREFIX + hudId + ".y";
        状态数据.remove(keyX);
        状态数据.remove(keyY);
        保存到文件();
    }

    // ==================== 工具方法 ====================

    private static void 确保已加载() {
        if (!已加载) {
            初始化();
        }
    }

    public static void 重置() {
        状态数据 = new JsonObject();
        保存到文件();
        PuellaMagi.LOGGER.info("客户端状态已重置");
    }
}
