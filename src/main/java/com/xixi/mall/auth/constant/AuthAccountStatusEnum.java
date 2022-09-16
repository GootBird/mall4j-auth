package com.xixi.mall.auth.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一账户信息状态
 */
@Getter
@AllArgsConstructor
public enum AuthAccountStatusEnum {

    /**
     * 启用
     */
    ENABLE(1),

    /**
     * 禁用
     */
    DISABLE(0),

    /**
     * 删除
     */
    DELETE(-1);

    private final Integer value;

}
