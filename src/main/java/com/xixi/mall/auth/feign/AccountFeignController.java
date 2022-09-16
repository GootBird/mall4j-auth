package com.xixi.mall.auth.feign;

import com.xixi.mall.api.auth.bo.UserInfoInTokenBo;
import com.xixi.mall.api.auth.constant.SysTypeEnum;
import com.xixi.mall.api.auth.dto.AuthAccountDto;
import com.xixi.mall.api.auth.feign.AccountFeignClient;
import com.xixi.mall.api.auth.vo.AuthAccountVo;
import com.xixi.mall.api.auth.vo.TokenInfoVo;
import com.xixi.mall.auth.service.feign.AccountFeignService;
import com.xixi.mall.common.core.aop.PackResponseEnhance;
import com.xixi.mall.common.core.webbase.vo.ServerResponse;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class AccountFeignController implements AccountFeignClient {

    @Resource
    private AccountFeignService service;

    /**
     * 保存统一账户
     *
     * @param authAccountDto 账户信息
     * @return Long uid
     */
    @Override
    public ServerResponse<Long> save(AuthAccountDto authAccountDto) {
        return PackResponseEnhance.enhance(() -> service.save(authAccountDto));
    }

    /**
     * 更新统一账户
     *
     * @param authAccountDto 账户信息
     * @return void
     */
    @Override
    public ServerResponse<Void> update(AuthAccountDto authAccountDto) {
        return PackResponseEnhance.enhance(() -> service.update(authAccountDto));
    }

    /**
     * 更新账户状态
     *
     * @param authAccountDto 账户信息
     * @return void
     */
    @Override
    public ServerResponse<Void> updateAccountStatus(AuthAccountDto authAccountDto) {
        return PackResponseEnhance.enhance(() -> service.updateAccountStatus(authAccountDto));
    }

    /**
     * 根据用户id和系统类型删除用户
     *
     * @param userId 用户id
     * @return void
     */
    @Override
    public ServerResponse<Void> deleteById(Long userId) {
        return PackResponseEnhance.enhance(() -> service.deleteById(userId));
    }

    /**
     * 根据用户id和系统类型获取用户信息
     *
     * @param userId 用户id
     * @return void
     */
    @Override
    public ServerResponse<AuthAccountVo> getById(Long userId) {
        return PackResponseEnhance.enhance(() -> service.getById(userId));
    }

    /**
     * 保存用户信息，生成token，返回前端
     *
     * @param userInfoInTokenBo 账户信息 和社交账号信息
     * @return uid
     */
    @Override
    public ServerResponse<TokenInfoVo> storeTokenAndGet(UserInfoInTokenBo userInfoInTokenBo) {
        return PackResponseEnhance.enhance(() -> service.storeTokenAndGet(userInfoInTokenBo));
    }

    /**
     * 根据用户名和系统类型获取用户信息
     *
     * @param username 用户名
     * @param sysType  系统类型
     * @return resp
     */
    @Override
    public ServerResponse<AuthAccountVo> getByUsername(String username, SysTypeEnum sysType) {
        return PackResponseEnhance.enhance(() -> service.getByUsername(username, sysType));
    }

    /**
     * 根据用户id与用户类型更新用户信息
     *
     * @param userInfoInTokenBo 新的用户信息
     * @param userId            用户id
     * @param sysType           用户类型
     * @return resp
     */
    @Override
    public ServerResponse<Void> updateUser(UserInfoInTokenBo userInfoInTokenBo, Long userId, Integer sysType) {
        return PackResponseEnhance.enhance(
                () -> service.updateUser(userInfoInTokenBo, userId, sysType)
        );
    }

    /**
     * 根据租户id查询商家信息
     *
     * @param tenantId 租户Id
     * @return resp
     */
    @Override
    public ServerResponse<AuthAccountVo> getMerchantByTenantId(Long tenantId) {
        return PackResponseEnhance.enhance(
                () -> service.getMerchantByTenantId(tenantId)
        );
    }

}
