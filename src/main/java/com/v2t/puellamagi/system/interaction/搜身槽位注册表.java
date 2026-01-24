// 文件路径: src/main/java/com/v2t/puellamagi/system/interaction/搜身槽位注册表.java

package com.v2t.puellamagi.system.interaction;

import com.v2t.puellamagi.api.interaction.I搜身槽位提供者;
import com.v2t.puellamagi.api.interaction.I搜身槽位提供者.区域类型;
import com.v2t.puellamagi.api.interaction.I搜身槽位提供者.搜身容器信息;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 搜身槽位注册表
 *
 * 管理所有槽位提供者的注册
 * 按优先级排序返回可用的容器
 * 支持按区域类型（主区域/侧边）分类获取
 */
public final class 搜身槽位注册表 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/SearchSlot");

    private static final List<I搜身槽位提供者> 提供者列表 = new CopyOnWriteArrayList<>();

    private 搜身槽位注册表() {}

    /**
     * 注册槽位提供者
     */
    public static void 注册(I搜身槽位提供者 provider) {
        if (provider == null) return;

        // 检查重复
        for (I搜身槽位提供者 existing : 提供者列表) {
            if (existing.获取提供者ID().equals(provider.获取提供者ID())) {
                LOGGER.warn("槽位提供者 {} 已存在，跳过重复注册", provider.获取提供者ID());
                return;
            }
        }

        提供者列表.add(provider);
        LOGGER.info("注册搜身槽位提供者: {}", provider.获取提供者ID());
    }

    /**
     * 获取目标玩家的所有可搜身容器
     * 按优先级排序
     *
     * @param target 被搜身的玩家
     * @return 所有可用容器信息列表
     */
    public static List<搜身容器信息> 获取所有容器(Player target) {
        List<搜身容器信息> result = new ArrayList<>();

        // 按优先级排序
        List<I搜身槽位提供者> sorted = new ArrayList<>(提供者列表);
        sorted.sort(Comparator.comparingInt(I搜身槽位提供者::获取优先级));

        for (I搜身槽位提供者 provider : sorted) {
            if (!provider.是否可用()) {
                continue;
            }

            try {
                List<搜身容器信息> containers = provider.获取容器(target);
                if (containers != null) {
                    result.addAll(containers);
                }
            } catch (Exception e) {
                LOGGER.error("槽位提供者 {} 获取容器时出错", provider.获取提供者ID(), e);
            }
        }

        return result;
    }

    /**
     * 获取指定区域的容器
     *
     * @param target 被搜身的玩家
     * @param 区域 区域类型
     * @return 该区域的容器列表
     */
    public static List<搜身容器信息> 获取区域容器(Player target, 区域类型 区域) {
        List<搜身容器信息> all = 获取所有容器(target);
        List<搜身容器信息> result = new ArrayList<>();

        for (搜身容器信息 info : all) {
            if (info.区域() == 区域) {
                result.add(info);
            }
        }

        return result;
    }

    /**
     * 获取主区域容器（背包、快捷栏等）
     */
    public static List<搜身容器信息> 获取主区域容器(Player target) {
        return 获取区域容器(target, 区域类型.主区域);
    }

    /**
     * 获取侧边区域容器（装备、副手、饰品等）
     */
    public static List<搜身容器信息> 获取侧边区域容器(Player target) {
        return 获取区域容器(target, 区域类型.侧边区域);
    }

    /**
     * 获取已注册的提供者数量
     */
    public static int 获取提供者数量() {
        return 提供者列表.size();
    }

    /**
     * 清空所有注册（测试用）
     */
    public static void 清空() {
        提供者列表.clear();
    }
}
