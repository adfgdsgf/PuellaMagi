// 文件路径: src/main/java/com/v2t/puellamagi/api/access/IParticleAccess.java

package com.v2t.puellamagi.api.access;

/**
 * 粒子访问接口
 */
public interface IParticleAccess {

    void puellamagi$setTimeStopCreated(boolean value);

    boolean puellamagi$isTimeStopCreated();

    /**
     * 存储当前帧时间（自动从 Minecraft.getFrameTime() 获取）
     */
    void puellamagi$setPreTSTick();

    float puellamagi$getPreTSTick();

    void puellamagi$resetPreTSTick();
}
