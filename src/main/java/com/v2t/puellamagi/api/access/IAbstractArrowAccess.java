// 文件路径: src/main/java/com/v2t/puellamagi/api/access/IAbstractArrowAccess.java

package com.v2t.puellamagi.api.access;

/**
 * AbstractArrow 访问接口
 *
 * 用于访问箭的inGround 字段
 */
public interface IAbstractArrowAccess {

    boolean puellamagi$isInGround();

    void puellamagi$setInGround(boolean inGround);
}
