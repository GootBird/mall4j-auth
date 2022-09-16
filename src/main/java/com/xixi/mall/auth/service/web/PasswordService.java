package com.xixi.mall.auth.service.web;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xixi.mall.api.auth.bo.UserInfoInTokenBo;
import com.xixi.mall.auth.entity.AuthAccountEntity;
import com.xixi.mall.auth.manage.AuthAccountManage;
import com.xixi.mall.auth.mapper.AuthAccountMapper;
import com.xixi.mall.auth.vo.request.UpdatePasswordReq;
import com.xixi.mall.auth.service.sys.TokenStoreSysService;
import com.xixi.mall.common.core.utils.ThrowUtils;
import com.xixi.mall.common.security.context.AuthUserContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.xixi.mall.common.core.constant.Constant.VOID;

@Service
public class PasswordService {

    @Resource
    private AuthAccountMapper accountMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private AuthAccountManage accountManage;

    @Resource
    private TokenStoreSysService tokenStoreSysService;


    public Void update(UpdatePasswordReq req) {

        UserInfoInTokenBo userInfoInToken = AuthUserContext.get();

        Long userId = userInfoInToken.getUserId();
        Integer sysType = userInfoInToken.getSysType();

        AuthAccountEntity authAccount = accountManage.getByUserIdAndType(
                userId,
                sysType
        );

        if (!passwordEncoder.matches(req.getOldPassword(), authAccount.getPassword())) {
            ThrowUtils.throwErr("旧密码不正确");
        }

        accountMapper.update(null,
                Wrappers.<AuthAccountEntity>lambdaUpdate()
                        .eq(AuthAccountEntity::getUserId, userId)
                        .eq(AuthAccountEntity::getSysType, sysType)
                        .set(AuthAccountEntity::getPassword, req.getNewPassword())
        );

        tokenStoreSysService.deleteAllToken(sysType.toString(), userInfoInToken.getUid());

        return VOID;
    }
}
