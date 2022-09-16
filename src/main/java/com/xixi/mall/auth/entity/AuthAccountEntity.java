package com.xixi.mall.auth.entity;

import com.xixi.mall.common.core.webbase.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 统一账户信息
 */
@Setter
@Getter
@ToString
public class AuthAccountEntity extends BaseEntity {

    /**
     * 全平台用户唯一id
     */
    private Long uid;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 创建ip
     */
    private String createIp;

    /**
     * 状态 1:启用 0:禁用 -1:删除
     */
    private Integer status;

    /**
     * 系统类型见SysTypeEnum 0.普通用户系统 1.商家端
     */
    private Integer sysType;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 所属租户
     */
    private Long tenantId;

    /**
     * 是否是管理员
     */
    private Integer isAdmin;

}
