package com.xixi.mall.auth.feign;

import com.xixi.mall.api.auth.bo.UserInfoInTokenBo;
import com.xixi.mall.api.auth.feign.TokenFeignClient;
import com.xixi.mall.auth.service.sys.TokenStoreSysService;
import com.xixi.mall.common.core.aop.PackResponseEnhance;
import com.xixi.mall.common.core.webbase.vo.ServerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
public class TokenFeignController implements TokenFeignClient {


    @Resource
    private TokenStoreSysService tokenStoreSysService;

    @Override
    public ServerResponse<UserInfoInTokenBo> checkToken(String accessToken) {
        return PackResponseEnhance.enhance(
                () -> tokenStoreSysService.getUserInfoByAccessToken(accessToken, true)
        );
    }

}
