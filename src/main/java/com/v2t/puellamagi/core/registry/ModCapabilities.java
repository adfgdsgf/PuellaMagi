package com.v2t.puellamagi.core.registry;

import com.v2t.puellamagi.api.I可变身;
import com.v2t.puellamagi.system.skill.技能能力;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * Capability注册和引用
 * 所有Capability在此统一管理
 */
public class ModCapabilities {

    /**
     * 变身能力
     */
    public static final Capability<I可变身> 变身能力 = CapabilityManager.get(new CapabilityToken<>() {});

    /**
     * 技能能力
     */
    public static final Capability<技能能力> 技能能力 = CapabilityManager.get(new CapabilityToken<>() {});

    // TODO: 后续添加// public static final Capability<契约能力> 契约能力 = CapabilityManager.get(new CapabilityToken<>() {});
}
