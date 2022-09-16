package com.xixi.mall.auth.service.web;

import com.xixi.mall.api.auth.vo.TokenInfoVo;
import com.xixi.mall.auth.service.sys.TokenStoreSysService;
import com.xixi.mall.auth.vo.request.RefreshTokenReq;
import com.xixi.mall.common.security.bo.TokenInfoBo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class TokenService {

    @Resource
    private TokenStoreSysService tokenStoreSysService;

    public TokenInfoVo refreshToken(RefreshTokenReq req) {
        TokenInfoBo tokenInfoBo = tokenStoreSysService.refreshToken(req.getRefreshToken());
        TokenInfoVo tokenInfoVo = new TokenInfoVo();
        BeanUtils.copyProperties(tokenInfoBo, tokenInfoVo);
        return tokenInfoVo;
    }

}
