package com.xixi.mall.auth.service.web;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xixi.mall.api.auth.bo.UserInfoInTokenBo;
import com.xixi.mall.api.auth.vo.TokenInfoVo;
import com.xixi.mall.api.rabc.dto.ClearUserPermissionsCacheDto;
import com.xixi.mall.api.rabc.feign.PermissionFeignClient;
import com.xixi.mall.auth.constant.AuthAccountStatusEnum;
import com.xixi.mall.auth.entity.AuthAccountEntity;
import com.xixi.mall.auth.mapper.AuthAccountMapper;
import com.xixi.mall.auth.service.sys.TokenStoreSysService;
import com.xixi.mall.auth.vo.request.LoginReq;
import com.xixi.mall.common.core.constant.StatusEnum;
import com.xixi.mall.common.core.enums.ResponseEnum;
import com.xixi.mall.common.core.utils.ThrowUtils;
import com.xixi.mall.common.core.webbase.vo.ServerResponse;
import com.xixi.mall.common.security.context.AuthUserContext;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Objects;

import static com.xixi.mall.common.core.constant.Constant.VOID;

@Service
public class LoginService {

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private AuthAccountMapper authAccountMapper;

    @Resource
    private TokenStoreSysService tokenStoreSysService;

    @Resource
    private PermissionFeignClient permissionFeignClient;


    public static final String USER_NOT_FOUND_SECRET = "USER_NOT_FOUND_SECRET";

    private static String userNotFoundEncodedPassword;

    @PostConstruct
    public void setUserNotFoundEncodedPassword() {
        userNotFoundEncodedPassword = this.passwordEncoder.encode(USER_NOT_FOUND_SECRET);
    }

    /**
     * 登录
     *
     * @param req req
     * @return resp
     */
    public TokenInfoVo in(LoginReq req) {

        String userName = req.getPrincipal(),
                password = req.getCredentials();

        Integer sysType = req.getSysType();

        AuthAccountEntity authAccountEntity = authAccountMapper.selectOne(
                Wrappers.<AuthAccountEntity>lambdaQuery()
                        .eq(AuthAccountEntity::getSysType, sysType)
                        .ne(AuthAccountEntity::getStatus, StatusEnum.DELETE.getValue())
                        .eq(AuthAccountEntity::getUsername, userName)
        );

        if (Objects.isNull(authAccountEntity)) {
            passwordEncoder.matches(password, userNotFoundEncodedPassword); //通过一次无用的密码校验防止黑客进行计时攻击穷举网站存在的用户
            ThrowUtils.throwErr("用户名或密码不正确");
        }

        if (Objects.equals(authAccountEntity.getStatus(), AuthAccountStatusEnum.DISABLE.getValue())) {
            ThrowUtils.throwErr("用户已禁用，请联系客服");
        }

        if (!passwordEncoder.matches(password, authAccountEntity.getPassword())) {
            ThrowUtils.throwErr("用户名或密码不正确");
        }

        UserInfoInTokenBo infoInTokenBo = new UserInfoInTokenBo();
        BeanUtils.copyProperties(authAccountEntity, infoInTokenBo);

        ClearUserPermissionsCacheDto clearUserPermissionsCacheDto = new ClearUserPermissionsCacheDto()
                .setSysType(infoInTokenBo.getSysType())
                .setUserId(infoInTokenBo.getUserId());

        // 将以前的权限清理了,以免权限有缓存
        ServerResponse<Void> clearResponseEntity = permissionFeignClient
                .clearUserPermissionsCache(clearUserPermissionsCacheDto);

        if (!clearResponseEntity.isSuccess()) {
            ThrowUtils.throwErr(ResponseEnum.UNAUTHORIZED);
        }

        // 保存token，返回token数据给前端，这里是最重要的
        return tokenStoreSysService.storeAndGetVo(infoInTokenBo);
    }

    public Void out() {

        UserInfoInTokenBo userInfoInToken = AuthUserContext.get();

        ClearUserPermissionsCacheDto clearUserPermissionsCacheDto = new ClearUserPermissionsCacheDto()
                .setSysType(userInfoInToken.getSysType())
                .setUserId(userInfoInToken.getUserId());

        // 将以前的权限清理了,以免权限有缓存
        permissionFeignClient.clearUserPermissionsCache(clearUserPermissionsCacheDto);

        // 删除该用户在该系统的token
        tokenStoreSysService.deleteAllToken(userInfoInToken.getSysType().toString(), userInfoInToken.getUid());

        return VOID;

    }
}
