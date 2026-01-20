// 文件路径: src/main/java/com/v2t/puellamagi/api/timestop/时停实例.java

package com.v2t.puellamagi.api.timestop;

/**
 * 客户端时停实例数据
 *
 * 客户端不一定能获取到实体引用，所以用ID和坐标存储
 */
public class 时停实例 {

    public final int 实体ID;
    public final double x;
    public final double y;
    public final double z;
    public final double 范围;

    public 时停实例(int entityId, double x, double y, double z, double range) {
        this.实体ID = entityId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.范围 = range;
    }

    /**
     * 判断位置是否在范围内
     */
    public boolean 在范围内(double px, double py, double pz) {
        if (范围 <= 0) {
            return true; // 无限范围
        }
        double dx = px - x;
        double dy = py - y;
        double dz = pz - z;
        return (dx * dx + dy * dy + dz * dz) <= (范围 * 范围);
    }
}
