// 文件路径: src/main/java/com/v2t/puellamagi/system/interaction/menu/搜身区域管理器.java

package com.v2t.puellamagi.system.interaction.menu;

import com.v2t.puellamagi.api.interaction.I搜身槽位提供者.区域类型;
import com.v2t.puellamagi.api.interaction.I搜身槽位提供者.搜身容器信息;
import com.v2t.puellamagi.api.interaction.I搜身槽位提供者.槽位限制;
import net.minecraft.world.Container;

import java.util.ArrayList;
import java.util.List;

import static com.v2t.puellamagi.system.interaction.menu.搜身布局常量.*;

/**
 * 搜身区域管理器
 *
 * 管理单个区域（主区域或侧边区域）的分页逻辑
 * 每个区域独立计算分页和槽位位置
 */
public class 搜身区域管理器 {

    //==================== 配置 ====================

    private final 区域类型 区域;
    private final int 最大内容高度;
    private final int 基准X;

    // ==================== 数据 ====================

    private final List<分组数据> 所有分组 = new ArrayList<>();
    private final List<页数据> 所有页 = new ArrayList<>();

    // ==================== 状态 ====================

    private int 当前页 = 0;

    // ==================== 输出数据 ====================

    private final List<槽位信息> 当前页槽位 = new ArrayList<>();
    private final List<区域显示信息> 当前页区域= new ArrayList<>();
    private int 当前页内容高度 = 0;

    // ==================== 数据结构 ====================

    /**
     * 分组数据（一个容器的完整信息）
     */
    public record 分组数据(
            搜身容器信息 容器信息,
            int 行数,
            int 占用高度
    ) {}

    /**
     * 页数据（记录每页包含哪些分组）
     */
    private record 页数据(
            int 起始分组索引,
            int 分组数量,
            int 内容高度
    ) {}

    /**
     * 槽位信息（用于菜单添加Slot）
     */
    public record 槽位信息(
            Container 容器,
            int 容器槽位索引,
            int x,
            int y,
            槽位限制 限制
    ) {}

    /**
     * 区域显示信息（用于Screen渲染）
     */
    public record 区域显示信息(
            String 容器ID,
            String 显示名称键,
            int 槽位数量,
            int 相对X,
            int 相对Y,
            int 列数
    ) {
        public int 计算行数() {
            return (int) Math.ceil((double) 槽位数量 / 列数);
        }
    }

    // ==================== 构造器 ====================

    public 搜身区域管理器(区域类型 区域, int 最大内容高度, int 基准X) {
        this.区域 = 区域;
        this.最大内容高度 = 最大内容高度;
        this.基准X = 基准X;
    }

    // ==================== 初始化 ====================

    public void 添加容器(搜身容器信息 容器信息) {
        if (容器信息.槽位数量() <= 0) return;
        if (容器信息.区域() != this.区域) return;

        int 列数 = 容器信息.列数();
        int 行数 = (int) Math.ceil((double) 容器信息.槽位数量() / 列数);
        int 占用高度 = 标签高度 + 行数 * 槽位尺寸 + 区域间距;

        所有分组.add(new 分组数据(容器信息, 行数, 占用高度));}

    public void 计算分页() {
        所有页.clear();

        if (所有分组.isEmpty()) {
            所有页.add(new 页数据(0, 0, 0));
            return;
        }

        int 当前页起始 = 0;
        int 当前页分组数 = 0;
        int 当前页高度 = 0;

        for (int i = 0; i < 所有分组.size(); i++) {
            分组数据 分组 = 所有分组.get(i);

            if (当前页分组数 > 0 && 当前页高度 + 分组.占用高度() > 最大内容高度) {
                所有页.add(new 页数据(当前页起始, 当前页分组数, 当前页高度));
                当前页起始 = i;
                当前页分组数 = 0;
                当前页高度 = 0;
            }

            当前页分组数++;
            当前页高度 += 分组.占用高度();
        }

        if (当前页分组数 > 0) {
            所有页.add(new 页数据(当前页起始, 当前页分组数, 当前页高度));
        }
    }

    public void 构建当前页(int 起始Y) {
        当前页槽位.clear();
        当前页区域.clear();

        页数据 页 = 所有页.get(当前页);
        int currentY = 起始Y;

        for (int i = 0; i < 页.分组数量(); i++) {
            int 分组索引 = 页.起始分组索引() + i;
            分组数据 分组 = 所有分组.get(分组索引);搜身容器信息 容器信息 = 分组.容器信息();

            int slotsY = currentY + 标签高度;
            int 列数 = 容器信息.列数();

            当前页区域.add(new 区域显示信息(
                    容器信息.容器ID(),
                    容器信息.显示名称键(),
                    容器信息.槽位数量(),
                    基准X,
                    currentY,
                    列数
            ));

            for (int j = 0; j < 容器信息.槽位数量(); j++) {
                int row = j / 列数;
                int col = j % 列数;
                int x = 基准X + col * 槽位尺寸 + 1;
                int y = slotsY + row * 槽位尺寸 + 1;
                int containerSlotIndex = 容器信息.起始槽位() + j;槽位限制 限制 = 容器信息.获取槽位限制(j);

                当前页槽位.add(new 槽位信息(
                        容器信息.容器(),
                        containerSlotIndex,
                        x,
                        y,
                        限制
                ));
            }

            currentY += 分组.占用高度();
        }

        当前页内容高度 = 页.内容高度();
    }

    // ==================== 翻页 API ====================

    public void 上一页() {
        if (当前页 > 0) {
            当前页--;}
    }

    public void 下一页() {
        if (当前页 < 所有页.size() - 1) {
            当前页++;
        }
    }

    public void 跳转到页(int page) {
        当前页 = Math.max(0, Math.min(page, 所有页.size() - 1));
    }

    public int 获取当前页() {
        return 当前页;
    }

    public int 获取总页数() {
        return 所有页.size();
    }

    public boolean 需要翻页() {
        return 所有页.size() > 1;
    }

    // ==================== Getter ====================

    public List<槽位信息> 获取当前页槽位() {
        return 当前页槽位;
    }

    public List<区域显示信息> 获取当前页区域() {
        return 当前页区域;
    }

    public int 获取当前页内容高度() {
        return 当前页内容高度;
    }

    public int 获取当前页槽位数() {
        return 当前页槽位.size();
    }

    public boolean 是否有内容() {
        return !所有分组.isEmpty();
    }

    public 区域类型 获取区域类型() {
        return 区域;
    }
}
