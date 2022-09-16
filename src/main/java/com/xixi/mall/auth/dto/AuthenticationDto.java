package com.xixi.mall.auth.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 用于登陆传递账号密码
 */
@Setter
@Getter
@ToString
public class AuthenticationDto {

    /**
     * 用户名
     */
    @NotBlank(message = "principal不能为空")
    @ApiModelProperty(value = "用户名", required = true)
    protected String principal;

    /**
     * 密码
     */
    @NotBlank(message = "credentials不能为空")
    @ApiModelProperty(value = "一般用作密码", required = true)
    protected String credentials;

    /**
     * sysType 参考SysTypeEnum
     */
    @NotNull(message = "sysType不能为空")
    @ApiModelProperty(value = "系统类型 0.普通用户系统 1.商家端", required = true)
    protected Integer sysType;

}
