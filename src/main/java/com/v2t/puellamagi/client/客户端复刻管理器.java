package com.v2t.puellamagi.client;

import com.v2t.puellamagi.system.ability.epitaph.玩家输入帧;
import com.v2t.puellamagi.util.recording.实体帧数据;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 客户端复刻管理器
 *
 * 视角平滑：Catmull-Rom样条插值
 * 缓冲机制：网络包→缓冲区→ tick边界激活
 * 防瞬移：过渡保护机制
 *按键时启动→ MouseMixin每帧同步位置旧值 → 传送静默发生→ 回放激活后关闭
 */
@OnlyIn(Dist.CLIENT)
public class 客户端复刻管理器 {

        //==================== 实体帧数据 ====================

        private static final Map<UUID, 实体帧数据> 当前帧表= new HashMap<>();
        private static final Map<UUID, 实体帧数据> 上一帧表 = new HashMap<>();

        // ==================== 输入帧（历史 +缓冲） ====================

        @Nullable
        private static 玩家输入帧 缓冲输入帧 = null;

        @Nullable
        private static 玩家输入帧 p0帧 = null;
        @Nullable
        private static 玩家输入帧 p1帧 = null;
        @Nullable
        private static 玩家输入帧 p2帧 = null;

        private static boolean 输入回放活跃 = false;

        // ==================== 过渡保护 ====================

        /**
         * 过渡保护
         *
         * 按技能键时启动 → 是否需要接管() 返回true
         * → MouseMixin每帧cancel鼠标 + 同步位置旧值
         * → 传送包到达时xo被立即设为x → render看到xo=x → 无瞬移
         * 回放正式激活后关闭
         */
        private static boolean 过渡保护 = false;

        // ==================== 结尾保护 ====================

        private static int 结尾保护剩余 = 0;
        private static final int 结尾保护时长 = 8;

        // ==================== 通用状态 ====================

        private static boolean 录制中 = false;
        private static boolean 活跃 = false;

        private 客户端复刻管理器() {}

        // ==================== 接收网络数据 ====================

        public static void 接收帧(UUID 使用者UUID, List<实体帧数据> 帧列表, List<实体帧数据> 上一帧列表param) {
                if (帧列表.isEmpty()) {
                        清除全部();
                        return;
                }

                当前帧表.clear();
                上一帧表.clear();

                for (实体帧数据 frame : 帧列表) {
                        当前帧表.put(frame.获取UUID(), frame);
                }

                for (实体帧数据 frame : 上一帧列表param) {
                        上一帧表.put(frame.获取UUID(), frame);
                }

                活跃 = true;
        }

        public static void 接收输入帧(Map<UUID, 玩家输入帧> 输入帧表) {
                if (输入帧表.isEmpty()) {
                        缓冲输入帧 = null;
                        if (输入回放活跃) {
                                结尾保护剩余 = 结尾保护时长;
                                结束输入回放();
                        }
                        return;
                }

                LocalPlayer local = Minecraft.getInstance().player;
                if (local == null) return;
                if (!输入帧表.containsKey(local.getUUID())) return;

                缓冲输入帧 = 输入帧表.get(local.getUUID());}

        /**兼容方法 */
        public static void 接收鼠标样本(Map<UUID, List<float[]>> 鼠标样本表) {
        }

        // ==================== Tick同步 ====================

        public static void tick同步() {
                if (结尾保护剩余 > 0) {
                        结尾保护剩余--;
                }

                if (缓冲输入帧 == null) return;

                p0帧 = p1帧;
                p1帧 = p2帧;
                p2帧 = 缓冲输入帧;
                缓冲输入帧 = null;

                if (p1帧 != null) {
                        输入回放活跃 = true;
                        // 回放激活 → 过渡保护完成使命
                        过渡保护 = false;
                }
        }

        // ==================== 过渡保护 ====================

        /**
         * 启动过渡保护
         * 客户端按技能键时调用（此时比服务端回滚早一个网络往返）
         */
        public static void 启动过渡保护() {
                过渡保护 = true;
        }

        // ==================== 状态查询 ====================

        /**
         * 是否需要接管鼠标和位置同步
         *
         * 三种情况都返回true：
         * 1. 过渡保护中（按键后、回放前）—覆盖传送包到达的窗口期
         * 2. 回放中
         * 3. 结尾保护中
         */
        public static boolean 是否需要接管() {
                return 过渡保护 || 输入回放活跃 || 结尾保护剩余 > 0;
        }

        public static boolean 是否过渡保护中() {
                return 过渡保护;
        }

        public static boolean 是否结尾保护中() {
                return 结尾保护剩余 > 0;
        }

        // ==================== 实体帧查询 ====================

        public static boolean 实体是否被控制(UUID entityUUID) {
                return 活跃 && 当前帧表.containsKey(entityUUID);
        }

        @Nullable
        public static 实体帧数据 获取当前帧(UUID entityUUID) {
                return 当前帧表.get(entityUUID);
        }

        @Nullable
        public static 实体帧数据 获取上一帧(UUID entityUUID) {
                return 上一帧表.get(entityUUID);
        }

        // ==================== 输入回放查询 ====================

        public static boolean 本地玩家是否输入回放中() {
                return 输入回放活跃&& p1帧 != null && p2帧 != null;
        }

        @Nullable
        public static 玩家输入帧 获取本地玩家输入帧() {
                return p2帧;
        }

        @Nullable
        public static 玩家输入帧 获取上一帧本地玩家输入帧() {
                return p1帧;
        }

        // ==================== Catmull-Rom 视角插值 ====================

        public static float 获取视角插值YRot(float partialTick) {
                if (p1帧 == null || p2帧 == null) return Float.NaN;

                float v1 = p1帧.获取YRot();
                float v2 = p2帧.获取YRot();

                float d2 = Mth.wrapDegrees(v2 - v1);
                float d0 = (p0帧 != null) ? Mth.wrapDegrees(p0帧.获取YRot() - v1) : -d2;
                float d3 = 2.0f * d2 - d0;

                return v1 + catmullRom(d0, 0, d2, d3, partialTick);
        }

        public static float 获取视角插值XRot(float partialTick) {
                if (p1帧 == null || p2帧 == null) return Float.NaN;

                float v1 = p1帧.获取XRot();
                float v2 = p2帧.获取XRot();
                float v0 = (p0帧 != null) ? p0帧.获取XRot() : (2* v1 - v2);
                float v3 = 2 * v2 - v1;

                return Mth.clamp(catmullRom(v0, v1, v2, v3, partialTick), -90.0f, 90.0f);
        }

        private static float catmullRom(float p0, float p1, float p2, float p3, float t) {
                float t2 = t * t;
                float t3 = t2 * t;

                return 0.5f * ((2 * p1) +
                        (-p0 + p2) * t +
                        (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 +
                        (-p0 + 3 * p1 - 3 * p2 + p3) * t3
                );
        }

        // ==================== 位置旧值同步 ====================

        public static void 同步位置旧值(LocalPlayer player) {
                player.xo = player.getX();
                player.yo = player.getY();
                player.zo = player.getZ();
                player.xOld = player.getX();
                player.yOld = player.getY();
                player.zOld = player.getZ();

                player.walkDistO = player.walkDist;
                player.oBob = player.bob;
                player.yBodyRotO = player.yBodyRot;
        }

        // ==================== 内部方法 ====================

        private static void 结束输入回放() {
                p0帧 = null;
                p1帧 = null;
                p2帧 = null;
                缓冲输入帧 = null;
                输入回放活跃 = false;
        }

        // ==================== 录制状态 ====================

        public static void 设置录制中(boolean active) {
                录制中 = active;
        }

        public static boolean 是否录制中() {
                return 录制中;
        }

        // ==================== 按键快照（录制用） ====================

        private static Map<String, int[]> 按键快照 = new HashMap<>();

        public static void 设置按键快照(Map<String, int[]> snapshot) {
                按键快照 = snapshot;
        }

        public static Map<String, int[]> 获取按键快照() {
                return 按键快照;
        }

        public static void 清除按键快照() {
                if (!按键快照.isEmpty()) {
                        按键快照.clear();
                }
        }

        // ==================== 生命周期 ====================

        public static void 清除全部() {
                当前帧表.clear();
                上一帧表.clear();
                按键快照.clear();
                p0帧 = null;
                p1帧 = null;
                p2帧 = null;
                缓冲输入帧 = null;
                输入回放活跃 = false;
                过渡保护 = false;结尾保护剩余 = 0;
                活跃 = false;
                录制中 = false;

                // 清空所有按键状态（防止isDown卡住导致自动挖/自动攻击）
                Map<String, net.minecraft.client.KeyMapping> allKeys =
                        com.v2t.puellamagi.mixin.access.KeyMappingAccessor.puellamagi$getAll();
                for (net.minecraft.client.KeyMapping key : allKeys.values()) {
                        key.setDown(false);
                        ((com.v2t.puellamagi.mixin.access.KeyMappingAccessor) key).puellamagi$setClickCount(0);
                }
        }
}
