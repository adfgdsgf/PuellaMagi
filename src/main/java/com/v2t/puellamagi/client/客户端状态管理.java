// 文件路径: src/main/java/com/v2t/puellamagi/client/客户端状态管理.java

package com.v2t.puellamagi.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.v2t.puellamagi.PuellaMagi;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 客户端UI状态持久化管理
 *
 * 保存内容：
 * - 技能栏折叠状态
 * - 最后使用的预设索引
 * - 编辑器标签页位置
 * - 编辑面板位置
 * - 其他UI偏好设置
 *
 * 存储位置：config/puellamagi/client_state.json
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
            // 确保目录存在
            if (!Files.exists(配置目录)) {
                Files.createDirectories(配置目录);
            }

            // 读取配置文件
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

    /**
     * 获取布尔值
     */
    public static boolean 获取布尔(String key, boolean defaultValue) {
        确保已加载();
        if (状态数据.has(key) && 状态数据.get(key).isJsonPrimitive()) {
            return 状态数据.get(key).getAsBoolean();
        }
        return defaultValue;
    }

    /**
     * 设置布尔值
     */
    public static void 设置布尔(String key, boolean value) {
        确保已加载();
        状态数据.addProperty(key, value);
        保存到文件();
    }

    /**
     * 获取整数值
     */
    public static int 获取整数(String key, int defaultValue) {
        确保已加载();
        if (状态数据.has(key) && 状态数据.get(key).isJsonPrimitive()) {
            return 状态数据.get(key).getAsInt();
        }
        return defaultValue;
    }

    /**
     * 设置整数值
     */
    public static void 设置整数(String key, int value) {
        确保已加载();
        状态数据.addProperty(key, value);
        保存到文件();
    }

    /**
     * 获取浮点值
     */
    public static float 获取浮点(String key, float defaultValue) {
        确保已加载();
        if (状态数据.has(key) && 状态数据.get(key).isJsonPrimitive()) {
            return 状态数据.get(key).getAsFloat();
        }
        return defaultValue;
    }

    /**
     * 设置浮点值
     */
    public static void 设置浮点(String key, float value) {
        确保已加载();
        状态数据.addProperty(key, value);
        保存到文件();
    }

    /**
     * 获取字符串值
     */
    public static String 获取字符串(String key, String defaultValue) {
        确保已加载();
        if (状态数据.has(key) && 状态数据.get(key).isJsonPrimitive()) {
            return 状态数据.get(key).getAsString();
        }
        return defaultValue;
    }

    /**
     * 设置字符串值
     */
    public static void 设置字符串(String key, String value) {
        确保已加载();
        状态数据.addProperty(key, value);
        保存到文件();
    }

    // ==================== 便捷方法（常用状态） ====================

    // --- 技能栏相关 ---

    private static final String KEY_SKILL_BAR_COLLAPSED = "skill_bar.collapsed";
    private static final String KEY_SKILL_BAR_PRESET_INDEX = "skill_bar.preset_index";

    /**
     * 获取技能栏是否折叠
     */
    public static boolean 技能栏是否折叠() {
        return 获取布尔(KEY_SKILL_BAR_COLLAPSED, false);
    }

    /**
     * 设置技能栏折叠状态
     */
    public static void 设置技能栏折叠(boolean collapsed) {
        设置布尔(KEY_SKILL_BAR_COLLAPSED, collapsed);
    }

    /**
     * 获取上次使用的预设索引
     */
    public static int 获取上次预设索引() {
        return 获取整数(KEY_SKILL_BAR_PRESET_INDEX, 0);
    }

    /**
     * 设置上次使用的预设索引
     */
    public static void 设置上次预设索引(int index) {
        设置整数(KEY_SKILL_BAR_PRESET_INDEX, index);
    }

    // --- 编辑器相关 ---

    private static final String KEY_EDITOR_TAB = "editor.last_tab";
    private static final String KEY_EDITOR_SCROLL = "editor.scroll_position";

    /**
     * 获取编辑器上次标签页
     */
    public static int 获取编辑器标签页() {
        return 获取整数(KEY_EDITOR_TAB, 0);
    }

    /**
     * 设置编辑器标签页
     */
    public static void 设置编辑器标签页(int tab) {
        设置整数(KEY_EDITOR_TAB, tab);
    }

    // --- 编辑面板位置 ---

    private static final String KEY_EDITOR_PANEL_X = "editor.panel_x";
    private static final String KEY_EDITOR_PANEL_Y = "editor.panel_y";

    /**
     * 获取编辑面板X位置（-1表示使用默认）
     */
    public static int 获取编辑面板X() {
        return 获取整数(KEY_EDITOR_PANEL_X, -1);
    }

    /**
     * 获取编辑面板Y位置（-1表示使用默认）
     */
    public static int 获取编辑面板Y() {
        return 获取整数(KEY_EDITOR_PANEL_Y, -1);
    }

    /**
     * 设置编辑面板位置
     */
    public static void 设置编辑面板位置(int x, int y) {
        设置整数(KEY_EDITOR_PANEL_X, x);
        设置整数(KEY_EDITOR_PANEL_Y, y);
    }

    // ==================== 工具方法 ====================

    private static void 确保已加载() {
        if (!已加载) {
            初始化();
        }
    }

    /**
     * 重置所有状态（调试用）
     */
    public static void 重置() {
        状态数据 = new JsonObject();
        保存到文件();
        PuellaMagi.LOGGER.info("客户端状态已重置");
    }
}
