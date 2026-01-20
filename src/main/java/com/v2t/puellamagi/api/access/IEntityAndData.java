// 文件路径: src/main/java/com/v2t/puellamagi/api/access/IEntityAndData.java

package com.v2t.puellamagi.api.access;

/**
 * 实体扩展数据接口
 *
 * 用于存储时停前的状态快照，防止渲染抖动
 * 参考 Roundabout 的 IEntityAndData
 */
public interface IEntityAndData {

    //==================== 时停前位置快照 ====================

    double puellamagi$getPrevX();
    double puellamagi$getPrevY();
    double puellamagi$getPrevZ();

    void puellamagi$setPrevX(double x);
    void puellamagi$setPrevY(double y);
    void puellamagi$setPrevZ(double z);

    // ==================== 时停前动画快照 ====================

    float puellamagi$getPrevAttackAnim();
    void puellamagi$setPrevAttackAnim(float value);

    float puellamagi$getPrevYBodyRot();
    void puellamagi$setPrevYBodyRot(float value);

    float puellamagi$getPrevYHeadRot();
    void puellamagi$setPrevYHeadRot(float value);

    // ==================== 时停前 PartialTick（关键！）====================

    /**
     * 获取时停前的 partialTick
     * 用于渲染时保持动画静止
     */
    float puellamagi$getPreTSTick();

    /**
     * 设置时停前的 partialTick
     */
    void puellamagi$setPreTSTick(float value);

    /**
     * 重置 preTSTick
     */
    void puellamagi$resetPreTSTick();

    // ==================== 辅助方法 ====================

    void puellamagi$storeTimestopSnapshot();
    void puellamagi$applyTimestopSnapshot();
}
