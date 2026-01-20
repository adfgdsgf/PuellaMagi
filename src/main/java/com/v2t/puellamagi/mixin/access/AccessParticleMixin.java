// 文件路径: src/main/java/com/v2t/puellamagi/mixin/access/AccessParticleMixin.java

package com.v2t.puellamagi.mixin.access;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 粒子字段访问器 - 必须是 interface
 */
@Mixin(Particle.class)
public interface AccessParticleMixin {

    @Accessor("x")
    double puellamagi$getX();

    @Accessor("y")
    double puellamagi$getY();

    @Accessor("z")
    double puellamagi$getZ();

    @Accessor("xo")
    double puellamagi$getXO();

    @Accessor("yo")
    double puellamagi$getYO();

    @Accessor("zo")
    double puellamagi$getZO();

    @Accessor("xo")
    void puellamagi$setXO(double value);

    @Accessor("yo")
    void puellamagi$setYO(double value);

    @Accessor("zo")
    void puellamagi$setZO(double value);
}
