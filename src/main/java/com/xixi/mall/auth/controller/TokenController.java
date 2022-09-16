package com.xixi.mall.auth.controller;

import com.xixi.mall.api.auth.vo.TokenInfoVo;
import com.xixi.mall.auth.service.web.TokenService;
import com.xixi.mall.auth.vo.request.RefreshTokenReq;
import com.xixi.mall.common.core.aop.PackResponseEnhance;
import com.xixi.mall.common.core.webbase.vo.ServerResponse;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@Api(tags = "token")
public class TokenController {

    @Resource
    private TokenService service;

    @PostMapping("/ua/token/refresh")
    public ServerResponse<TokenInfoVo> refreshToken(@Valid @RequestBody RefreshTokenReq req) {
        return PackResponseEnhance.enhance(() -> service.refreshToken(req));
    }

}
