// 文件路径: src/main/java/com/v2t/puellamagi/mixin/access/EntityAndDataMixin.java

package com.v2t.puellamagi.mixin.access;

import com.v2t.puellamagi.api.access.IEntityAndData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Entity数据扩展 Mixin
 *
 * 存储时停前的状态快照
 * 参考 Roundabout 的 EntityAndData
 */
@Mixin(Entity.class)
public abstract class EntityAndDataMixin implements IEntityAndData {

    // ==================== 位置快照 ====================

    @Unique
    private double puellamagi$prevX = 0;

    @Unique
    private double puellamagi$prevY = 0;

    @Unique
    private double puellamagi$prevZ = 0;

    // ==================== 动画快照 ====================

    @Unique
    private float puellamagi$prevAttackAnim = 0;

    @Unique
    private float puellamagi$prevYBodyRot = 0;

    @Unique
    private float puellamagi$prevYHeadRot = 0;

    // ==================== PartialTick 快照（关键！）====================

    @Unique
    private float puellamagi$preTSTick = 0;

    // ==================== 位置 Getter/Setter ====================

    @Override
    public double puellamagi$getPrevX() {
        return puellamagi$prevX;
    }

    @Override
    public double puellamagi$getPrevY() {
        return puellamagi$prevY;
    }

    @Override
    public double puellamagi$getPrevZ() {
        return puellamagi$prevZ;
    }

    @Override
    public void puellamagi$setPrevX(double x) {
        puellamagi$prevX = x;
    }

    @Override
    public void puellamagi$setPrevY(double y) {
        puellamagi$prevY = y;
    }

    @Override
    public void puellamagi$setPrevZ(double z) {
        puellamagi$prevZ = z;
    }

    // ==================== 动画 Getter/Setter ====================

    @Override
    public float puellamagi$getPrevAttackAnim() {
        return puellamagi$prevAttackAnim;
    }

    @Override
    public void puellamagi$setPrevAttackAnim(float value) {
        puellamagi$prevAttackAnim = value;
    }

    @Override
    public float puellamagi$getPrevYBodyRot() {
        return puellamagi$prevYBodyRot;
    }

    @Override
    public void puellamagi$setPrevYBodyRot(float value) {
        puellamagi$prevYBodyRot = value;
    }

    @Override
    public float puellamagi$getPrevYHeadRot() {
        return puellamagi$prevYHeadRot;
    }

    @Override
    public void puellamagi$setPrevYHeadRot(float value) {
        puellamagi$prevYHeadRot = value;
    }

    // ==================== PartialTick ====================

    @Override
    public float puellamagi$getPreTSTick() {
        return puellamagi$preTSTick;
    }

    @Override
    public void puellamagi$setPreTSTick(float value) {
        puellamagi$preTSTick = value;
    }

    @Override
    public void puellamagi$resetPreTSTick() {
        puellamagi$preTSTick = 0;
    }

    // ==================== 辅助方法 ====================

    @Override
    public void puellamagi$storeTimestopSnapshot() {
        Entity self = (Entity) (Object) this;
        puellamagi$prevX = self.getX();
        puellamagi$prevY = self.getY();
        puellamagi$prevZ = self.getZ();

        if (self instanceof LivingEntity living) {
            puellamagi$prevAttackAnim = living.attackAnim;
            puellamagi$prevYBodyRot = living.yBodyRot;
            puellamagi$prevYHeadRot = living.yHeadRot;
        }
    }

    @Override
    public void puellamagi$applyTimestopSnapshot() {
        Entity self = (Entity) (Object) this;

        if (self instanceof LivingEntity living) {
            living.attackAnim = puellamagi$prevAttackAnim;
            living.oAttackAnim = puellamagi$prevAttackAnim;

            living.yBodyRot = puellamagi$prevYBodyRot;
            living.yBodyRotO = puellamagi$prevYBodyRot;

            living.yHeadRot = puellamagi$prevYHeadRot;
            living.yHeadRotO = puellamagi$prevYHeadRot;
        }
    }
}
