package com.xixi.mall.auth.vo.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotBlank;

/**
 * 刷新token
 */
@Getter
@Setter
@ToString
public class RefreshTokenReq {

    /**
     * refreshToken
     */
    @NotBlank(message = "refreshToken不能为空")
    @ApiModelProperty(value = "refreshToken", required = true)
    private String refreshToken;

}
