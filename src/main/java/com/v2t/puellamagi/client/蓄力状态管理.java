// 文件路径: src/main/java/com/v2t/puellamagi/client/蓄力状态管理.java

package com.v2t.puellamagi.client;

import com.v2t.puellamagi.api.I技能;
import com.v2t.puellamagi.system.skill.技能注册表;
import com.v2t.puellamagi.system.skill.技能槽位数据;
import com.v2t.puellamagi.util.能力工具;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * 客户端蓄力状态管理
 *
 * 职责：
 * - 蓄力进度追踪
 * - 淡出效果管理
 * - 技能开启状态查询
 *
 * 纯客户端运行时状态，不持久化
 */
public final class 蓄力状态管理 {
    private 蓄力状态管理() {}

    //==================== 蓄力状态====================

    // 当前正在蓄力的槽位索引，-1表示无
    private static int 蓄力槽位索引 = -1;

    // 蓄力开始的时间戳（毫秒）
    private static long 蓄力开始时间 = 0;

    // 蓄力总时长（tick）
    private static int 蓄力总时长 = 0;

    // 蓄满后是否自动释放
    private static boolean 蓄满自动释放 = true;

    // ==================== 淡出状态 ====================

    private static int 淡出槽位索引 = -1;
    private static long 淡出开始时间 = 0;
    private static final long 淡出持续毫秒 = 150;

    // ==================== 蓄力控制 ====================

    /**
     * 尝试开始蓄力（根据技能类型自动判断）
     *
     * @param player 玩家
     * @param slotIndex 槽位索引
     * @return 是否成功开始蓄力
     */
    public static boolean 尝试开始蓄力(Player player, int slotIndex) {
        var skillInfo = 获取槽位技能信息(player, slotIndex);
        if (skillInfo.isEmpty()) {
            return false;
        }

        I技能 skill = skillInfo.get();
        I技能.按键类型 type = skill.获取按键类型();

        // 切换类/蓄力切换类：已开启时按下是关闭，不开始蓄力
        if (type == I技能.按键类型.切换 || type == I技能.按键类型.蓄力切换) {
            if (skill.是否开启(player)) {
                return false;
            }
        }

        // 只有蓄力类和蓄力切换类需要显示蓄力进度
        if (type != I技能.按键类型.蓄力 && type != I技能.按键类型.蓄力切换) {
            return false;
        }

        // 获取实际蓄力时间（考虑创造模式等）
        int duration = skill.获取实际蓄力时间(player);
        if (duration <= 0) {
            return false;
        }

        开始蓄力(slotIndex, duration, skill.蓄满自动释放());
        return true;
    }

    /**
     * 开始蓄力（内部方法）
     */
    private static void 开始蓄力(int slotIndex, int durationTicks, boolean autoRelease) {
        蓄力槽位索引 = slotIndex;
        蓄力总时长 = durationTicks;
        蓄力开始时间 = System.currentTimeMillis();
        蓄满自动释放 = autoRelease;
        // 清除淡出状态
        淡出槽位索引 = -1;
    }

    /**
     * 结束蓄力（进入淡出状态）
     */
    public static void 结束蓄力() {
        if (蓄力槽位索引 >= 0) {
            淡出槽位索引 = 蓄力槽位索引;
            淡出开始时间 = System.currentTimeMillis();}
        蓄力槽位索引 = -1;
        蓄力总时长 = 0;
        蓄力开始时间 = 0;
        蓄满自动释放 = true;
    }

    /**
     * 结束指定槽位的蓄力
     */
    public static void 结束槽位蓄力(int slotIndex) {
        if (蓄力槽位索引 == slotIndex) {
            结束蓄力();
        }
    }

    /**
     * 清除所有蓄力状态（切换界面、解除变身时调用）
     */
    public static void 清除所有状态() {
        蓄力槽位索引 = -1;
        蓄力总时长 = 0;
        蓄力开始时间 = 0;
        蓄满自动释放 = true;
        淡出槽位索引 = -1;
        淡出开始时间 = 0;
    }

    // ==================== 状态查询 ====================

    /**
     * 获取当前显示蓄力条的槽位索引
     * @return 槽位索引，-1表示无
     */
    public static int 获取蓄力槽位() {
        // 先调用获取蓄力进度，触发自动释放检测
        获取蓄力进度();

        // 正在蓄力
        if (蓄力槽位索引 >= 0) {
            return 蓄力槽位索引;
        }

        // 淡出期间
        if (淡出槽位索引 >= 0) {
            long elapsed = System.currentTimeMillis() - 淡出开始时间;
            if (elapsed < 淡出持续毫秒) {
                return 淡出槽位索引;
            } else {
                淡出槽位索引 = -1;
            }
        }

        return -1;
    }

    /**
     * 获取蓄力进度
     * @return 0.0~1.0，0表示刚开始，1表示蓄满
     */
    public static float 获取蓄力进度() {
        // 正在蓄力
        if (蓄力槽位索引 >= 0 && 蓄力总时长 > 0) {
            long elapsed = System.currentTimeMillis() - 蓄力开始时间;
            float elapsedTicks = elapsed / 50f;
            float progress = elapsedTicks / 蓄力总时长;

            if (progress >= 1.0f) {
                if (蓄满自动释放) {
                    结束蓄力();
                    return 1.0f;
                } else {
                    return 1.0f;
                }
            }

            return progress;
        }

        // 淡出期间保持满进度
        if (淡出槽位索引 >= 0) {
            long elapsed = System.currentTimeMillis() - 淡出开始时间;
            if (elapsed < 淡出持续毫秒) {
                return 1.0f;
            }}

        return 0f;
    }

    /**
     * 是否正在蓄力（不包括淡出）
     */
    public static boolean 是否正在蓄力() {
        return 蓄力槽位索引 >= 0;
    }

    // ==================== 技能状态查询 ====================

    /**
     * 检查槽位技能是否处于开启状态（切换类技能）
     */
    public static boolean 槽位技能是否开启(Player player, int slotIndex) {
        var skillInfo = 获取槽位技能信息(player, slotIndex);
        if (skillInfo.isEmpty()) {
            return false;
        }

        I技能 skill = skillInfo.get();
        I技能.按键类型 type = skill.获取按键类型();

        if (type == I技能.按键类型.切换 || type == I技能.按键类型.蓄力切换) {
            return skill.是否开启(player);
        }

        return false;
    }

    /**
     * 获取槽位技能ID
     */
    public static Optional<ResourceLocation> 获取槽位技能ID(Player player, int slotIndex) {
        return 能力工具.获取技能能力(player).flatMap(cap -> {
            var preset = cap.获取当前预设();
            if (slotIndex < preset.获取槽位数量()) {
                技能槽位数据 slotData = preset.获取槽位(slotIndex);
                if (slotData != null && !slotData.是否为空()) {
                    return Optional.ofNullable(slotData.获取技能ID());
                }
            }
            return Optional.empty();
        });
    }

    // ==================== 内部工具方法 ====================

    /**
     * 获取槽位对应的技能实例
     */
    private static Optional<I技能> 获取槽位技能信息(Player player, int slotIndex) {
        return 能力工具.获取技能能力(player).flatMap(cap -> {
            var preset = cap.获取当前预设();
            if (slotIndex < preset.获取槽位数量()) {
                技能槽位数据 slotData = preset.获取槽位(slotIndex);
                if (slotData != null && !slotData.是否为空()) {
                    return 技能注册表.创建实例(slotData.获取技能ID());
                }
            }
            return Optional.empty();
        });
    }
}
