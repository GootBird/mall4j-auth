package com.xixi.mall.auth.controller;

import com.xixi.mall.auth.vo.request.UpdatePasswordReq;
import com.xixi.mall.auth.service.web.PasswordService;
import com.xixi.mall.common.core.aop.PackResponseEnhance;
import com.xixi.mall.common.core.webbase.vo.ServerResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@Api(tags = "密码")
@RequestMapping("/ua/password")
public class PasswordController {

    @Resource
    private PasswordService service;

    @PutMapping("/update")
    @ApiOperation(value = "更新密码", notes = "更新当前用户的密码, 更新密码之后要退出登录，清理token")
    public ServerResponse<Void> update(@Valid @RequestBody UpdatePasswordReq req) {
        return PackResponseEnhance.enhance(() -> service.update(req));
    }

}
