package com.v2t.puellamagi.client;

import com.v2t.puellamagi.mixin.access.MouseHandlerAccessor;
import com.v2t.puellamagi.system.ability.epitaph.玩家输入帧;
import com.v2t.puellamagi.util.recording.实体帧数据;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 客户端复刻管理器
 *
 * 视角平滑：Catmull-Rom样条插值
 * 缓冲机制：网络包→缓冲区→ tick边界激活
 * 防瞬移：过渡保护机制
 * 键盘/鼠标事件：GLFW层录制+回放（自动兼容ESC/Screen/所有mod快捷键）
 */
@OnlyIn(Dist.CLIENT)
public class 客户端复刻管理器 {

        // ==================== 实体帧数据 ====================

        private static final Map<UUID, 实体帧数据> 当前帧表 = new HashMap<>();
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

        private static boolean 过渡保护 = false;

        // ==================== 结尾保护 ====================

        private static int 结尾保护剩余 = 0;
        private static final int 结尾保护时长 = 8;

        // ==================== 通用状态 ====================

        private static boolean 录制中 = false;
        private static boolean 活跃 = false;

        // ==================== 键盘/鼠标事件队列（录制用） ====================

        /**
         * 恢复初始按键状态（收到服务端通知时立刻调用）
         *
         * 遍历键名列表 → 找到对应KeyMapping → setDown(true)
         * 立刻设置，不等重放输入事件
         * → 下一帧handleKeybinds检查时已经是正确状态
         * → 所有mod的按键都恢复
         */
        public static void 恢复初始按键(List<String> keyNames) {
                Minecraft mc = Minecraft.getInstance();
                Map<String, net.minecraft.client.KeyMapping> allKeys =
                        com.v2t.puellamagi.mixin.access.KeyMappingAccessor.puellamagi$getAll();

                for (String name : keyNames) {
                        net.minecraft.client.KeyMapping key = allKeys.get(name);
                        if (key == null) continue;

                        //跳过MC的攻击和使用键（由初始状态恢复单独管）
                        if (key == mc.options.keyAttack || key == mc.options.keyUse) continue;

                        key.setDown(true);
                }
        }

        /**
         * 录制期间累积的键盘事件
         * GLFW回调线程写入，tick结束时客户端事件读取并清空
         * 用CopyOnWriteArrayList避免并发问题
         */
        private static final List<玩家输入帧.键盘事件> 键盘事件队列 = new CopyOnWriteArrayList<>();
        private static final List<玩家输入帧.鼠标事件> 鼠标事件队列 = new CopyOnWriteArrayList<>();

        /**
         * 重放标记：回放注入的事件不要被Mixin拦截
         */
        private static boolean 正在重放事件 = false;

        private 客户端复刻管理器() {}

        // ==================== 键盘/鼠标事件录制 ====================

        private static List<玩家输入帧.鼠标事件> 上一帧鼠标事件 = new ArrayList<>();
        private static List<玩家输入帧.键盘事件> 上一帧键盘事件 = new ArrayList<>();

        /**
         * 录制键盘事件（由Mixin在GLFW回调时调用）
         */
        public static void 添加键盘事件(int keyCode, int scanCode, int action, int modifiers) {
                键盘事件队列.add(new 玩家输入帧.键盘事件(keyCode, scanCode, action, modifiers));
        }

        /**
         * 录制鼠标事件（由Mixin在GLFW回调时调用）
         */
        public static void 添加鼠标事件(int button, int action, int modifiers, double cursorX, double cursorY) {
                鼠标事件队列.add(new 玩家输入帧.鼠标事件(button, action, modifiers, cursorX, cursorY));
        }

        /**
         * 获取并清空键盘事件队列（发包时调用）
         */
        public static List<玩家输入帧.键盘事件> 获取并清空键盘事件() {
                List<玩家输入帧.键盘事件> result = new ArrayList<>(键盘事件队列);
                键盘事件队列.clear();
                return result;
        }

        /**
         * 获取并清空鼠标事件队列（发包时调用）
         */
        public static List<玩家输入帧.鼠标事件> 获取并清空鼠标事件() {
                List<玩家输入帧.鼠标事件> result = new ArrayList<>(鼠标事件队列);
                鼠标事件队列.clear();
                return result;
        }

        // ==================== 键盘/鼠标事件回放 ====================

        /**
         * 是否正在重放事件（Mixin用来判断是否放行）
         */
        public static boolean 是否正在重放事件() {
                return 正在重放事件;
        }

        // ==================== 初始鼠标状态（录制开始时进行中的操作） ====================



        /**
         * 回放键盘和鼠标事件
         *
         * 在tick开始时调用（tick同步里）
         * 直接调MC的处理方法→ 和真实按键一模一样
         * → KeyMapping更新、Screen收到按键、mod快捷键触发
         */
        private static void 重放输入事件() {
                if (!输入回放活跃 || p2帧 == null) return;Minecraft mc = Minecraft.getInstance();
                if (mc.getWindow() == null) return;
                long window = mc.getWindow().getWindow();
                // 设置鼠标光标位置（容器操作精确还原）
                // 直接设MouseHandler内部值，不经过onMove回调
                // → 不触发视角旋转 → 不被Mixin拦截 → Screen读到正确坐标
                MouseHandlerAccessor mouseAccessor = (MouseHandlerAccessor) mc.mouseHandler;
                mouseAccessor.puellamagi$setXPos(p2帧.获取光标X());
                mouseAccessor.puellamagi$setYPos(p2帧.获取光标Y());

                正在重放事件 = true;
                try {
                        // 键盘事件去重
                        List<玩家输入帧.键盘事件> thisKeys = p2帧.获取键盘事件列表();
                        if (!thisKeys.equals(上一帧键盘事件)) {
                                KeyboardHandler keyboard = mc.keyboardHandler;
                                for (玩家输入帧.键盘事件 event : thisKeys) {
                                        keyboard.keyPress(window, event.keyCode(), event.scanCode(),
                                                event.action(), event.modifiers());
                                }
                        }
                        上一帧键盘事件 = new ArrayList<>(thisKeys);

                        // 鼠标事件去重
                        List<玩家输入帧.鼠标事件> thisMouse = p2帧.获取鼠标事件列表();
                        if (!thisMouse.equals(上一帧鼠标事件)) {
                                com.v2t.puellamagi.mixin.access.MouseHandlerAccessor mouse =(com.v2t.puellamagi.mixin.access.MouseHandlerAccessor) mc.mouseHandler;
                                for (玩家输入帧.鼠标事件 event : thisMouse) {
                                        mouse.puellamagi$setXPos(event.cursorX());
                                        mouse.puellamagi$setYPos(event.cursorY());
                                        mouse.puellamagi$invokeOnPress(window, event.button(), event.action(), event.modifiers());
                                }
                        }
                        上一帧鼠标事件 = new ArrayList<>(thisMouse);
                } finally {
                        正在重放事件 = false;
                }
        }

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
                if (!输入帧表.containsKey(local.getUUID())) return;缓冲输入帧 = 输入帧表.get(local.getUUID());}

        /**兼容方法 */
        public static void 接收鼠标样本(Map<UUID, List<float[]>> 鼠标样本表) {}

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
                        过渡保护 = false;
                }

                // 重放键盘/鼠标事件
                重放输入事件();}

        // ==================== 过渡保护 ====================

        public static void 启动过渡保护() {
                过渡保护 = true;
        }

        // ==================== 状态查询 ====================

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
                return 输入回放活跃 && p1帧 != null && p2帧 != null;
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
                float v3 = 2* v2 - v1;

                return Mth.clamp(catmullRom(v0, v1, v2, v3, partialTick), -90.0f, 90.0f);
        }

        private static float catmullRom(float p0, float p1, float p2, float p3, float t) {
                float t2 = t * t;
                float t3 = t2 * t;
                return 0.5f * ((2* p1)
                        + (-p0 + p2) * t
                        + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2
                        + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
        }

        //==================== 射线结果缓存（录制用） ====================

        @Nullable
        private static net.minecraft.world.phys.HitResult 当前射线结果 = null;

        /**
         * 由EpitaphReplayHitResultMixin在handleKeybinds HEAD调用
         * 存的是pick()刚算完、MC即将使用的hitResult
         */
        public static void 设置当前射线结果(net.minecraft.world.phys.HitResult result) {
                当前射线结果 = result;
        }

        /**
         * 获取并清空（发包时调用）
         */
        @Nullable
        public static net.minecraft.world.phys.HitResult 获取并清空射线结果() {
                net.minecraft.world.phys.HitResult result = 当前射线结果;
                当前射线结果 = null;
                return result;
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
                if (!active) {
                        键盘事件队列.clear();
                        鼠标事件队列.clear();
                }
        }

        public static boolean 是否录制中() {
                return 录制中;
        }

        // ==================== 生命周期 ====================

        public static void 清除全部() {
                当前帧表.clear();
                上一帧表.clear();
                键盘事件队列.clear();
                鼠标事件队列.clear();
                p0帧 = null;
                p1帧 = null;
                p2帧 = null;
                缓冲输入帧 = null;
                当前射线结果 = null;
                输入回放活跃 = false;
                过渡保护 = false;
                结尾保护剩余 = 0;
                活跃 = false;
                录制中 = false;
                正在重放事件 = false;
                上一帧鼠标事件 = new ArrayList<>();
                上一帧键盘事件 = new ArrayList<>();

                Map<String, net.minecraft.client.KeyMapping> allKeys =
                        com.v2t.puellamagi.mixin.access.KeyMappingAccessor.puellamagi$getAll();
                for (net.minecraft.client.KeyMapping key : allKeys.values()) {
                        key.setDown(false);
                        ((com.v2t.puellamagi.mixin.access.KeyMappingAccessor) key).puellamagi$setClickCount(0);
                }
        }
}
