package com.xixi.mall.auth.controller;

import com.xixi.mall.api.auth.vo.TokenInfoVo;
import com.xixi.mall.auth.service.web.LoginService;
import com.xixi.mall.auth.vo.request.LoginReq;
import com.xixi.mall.common.core.aop.PackResponseEnhance;
import com.xixi.mall.common.core.webbase.vo.ServerResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@Api(tags = "登录")
@RequestMapping("/ua/login")
public class LoginController {

    @Resource
    private LoginService loginService;


    @PostMapping("/in")
    @ApiOperation(value = "账号密码", notes = "通过账号登录，还要携带用户的类型，也就是用户所在的系统")
    public ServerResponse<TokenInfoVo> in(@Valid @RequestBody LoginReq req) {
        return PackResponseEnhance.enhance(() -> loginService.in(req));
    }

    @PostMapping("/out")
    @ApiOperation(value = "退出登陆", notes = "点击退出登陆，清除token，清除菜单缓存")
    public ServerResponse<Void> out() {
        return PackResponseEnhance.enhance(() -> loginService.out());
    }

}
