package com.xixi.mall.auth.vo.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Setter
@Getter
@ToString
public class UpdatePasswordReq {

    @NotBlank(message = "oldPassword NotBlank")
    @ApiModelProperty(value = "旧密码", required = true)
    private String oldPassword;

    @NotNull(message = "newPassword NotNull")
    @ApiModelProperty(value = "新密码", required = true)
    private String newPassword;

}
