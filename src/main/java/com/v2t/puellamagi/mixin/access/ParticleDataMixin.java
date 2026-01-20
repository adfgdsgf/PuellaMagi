// 文件路径: src/main/java/com/v2t/puellamagi/mixin/access/ParticleDataMixin.java

package com.v2t.puellamagi.mixin.access;

import com.v2t.puellamagi.api.access.IParticleAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * 粒子数据扩展 - 实现 IParticleAccess
 */
@Mixin(Particle.class)
public class ParticleDataMixin implements IParticleAccess {

    @Unique
    private boolean puellamagi$timeStopCreated = false;

    @Unique
    private float puellamagi$preTSTick = 0.0f;

    @Override
    public void puellamagi$setTimeStopCreated(boolean value) {
        this.puellamagi$timeStopCreated = value;
    }

    @Override
    public boolean puellamagi$isTimeStopCreated() {
        return this.puellamagi$timeStopCreated;
    }

    @Override
    public void puellamagi$setPreTSTick() {
        Minecraft mc = Minecraft.getInstance();
        this.puellamagi$preTSTick = mc.getFrameTime();
    }

    @Override
    public float puellamagi$getPreTSTick() {
        return this.puellamagi$preTSTick;
    }

    @Override
    public void puellamagi$resetPreTSTick() {
        this.puellamagi$preTSTick = 0.0f;
    }
}
