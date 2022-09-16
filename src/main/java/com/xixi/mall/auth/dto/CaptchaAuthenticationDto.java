package com.xixi.mall.auth.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 验证码登陆
 */
@Getter
@Setter
@ToString(callSuper = true)
public class CaptchaAuthenticationDto extends AuthenticationDto {

    @ApiModelProperty(value = "验证码", required = true)
    private String captchaVerification;

}
