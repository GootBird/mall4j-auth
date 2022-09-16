package com.xixi.mall.auth.service.feign;

import cn.hutool.core.lang.generator.SnowflakeGenerator;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xixi.mall.api.auth.bo.UserInfoInTokenBo;
import com.xixi.mall.api.auth.constant.SysTypeEnum;
import com.xixi.mall.api.auth.dto.AuthAccountDto;
import com.xixi.mall.api.auth.vo.AuthAccountVo;
import com.xixi.mall.api.auth.vo.TokenInfoVo;
import com.xixi.mall.auth.entity.AuthAccountEntity;
import com.xixi.mall.auth.manage.AuthAccountManage;
import com.xixi.mall.auth.mapper.AuthAccountMapper;
import com.xixi.mall.auth.service.sys.TokenStoreSysService;
import com.xixi.mall.common.core.constant.StatusEnum;
import com.xixi.mall.common.core.enums.ResponseEnum;
import com.xixi.mall.common.core.utils.PrincipalUtil;
import com.xixi.mall.common.core.utils.ThrowUtils;
import com.xixi.mall.common.security.context.AuthUserContext;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Optional;

import static com.xixi.mall.common.core.constant.Constant.VOID;

@Service
public class AccountFeignService {


    @Resource
    private AuthAccountManage authAccountManage;

    @Resource
    private AuthAccountMapper authAccountMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private MapperFacade mapperFacade;

    @Resource
    private TokenStoreSysService tokenStoreSysService;

    private final SnowflakeGenerator snowflakeGenerator = new SnowflakeGenerator();

    /**
     * 保存统一账户
     *
     * @param authAccountDto 账户信息
     * @return Long uid
     */
    @Transactional(rollbackFor = Exception.class)
    public Long save(AuthAccountDto authAccountDto) {

        AuthAccountEntity newAccount = verifyUserIsExist(authAccountDto);
        newAccount.setUid(snowflakeGenerator.next());

        authAccountMapper.insert(newAccount);

        return newAccount.getUid();
    }

    /**
     * 更新统一账户
     *
     * @param authAccountDto 账户信息
     * @return void
     */
    public Void update(AuthAccountDto authAccountDto) {

        AuthAccountEntity newAccount = verifyUserIsExist(authAccountDto);
        authAccountManage.updateAccountInfo(newAccount);
        return VOID;
    }

    /**
     * 更新账户状态
     *
     * @param authAccountDto 账户信息
     * @return void
     */
    public Void updateAccountStatus(AuthAccountDto authAccountDto) {

        Optional.ofNullable(authAccountDto.getStatus())
                .orElseThrow(ThrowUtils.getSupErr(ResponseEnum.EXCEPTION));

        AuthAccountEntity authAccountEntity = mapperFacade.map(authAccountDto, AuthAccountEntity.class);
        authAccountManage.updateAccountInfo(authAccountEntity);

        return VOID;
    }

    /**
     * 根据用户id和系统类型删除用户
     *
     * @param userId 用户id
     * @return void
     */
    public Void deleteById(Long userId) {

        UserInfoInTokenBo userInfoInTokenBo = AuthUserContext.get();

        authAccountManage.deleteByUserIdAndSysType(userId, userInfoInTokenBo.getSysType());
        return VOID;
    }

    /**
     * 根据用户id和系统类型获取用户信息
     *
     * @param userId 用户id
     * @return void
     */
    public AuthAccountVo getById(Long userId) {
        UserInfoInTokenBo userInfoInTokenBo = AuthUserContext.get();
        AuthAccountEntity authAccountEntity = authAccountManage.getByUserIdAndType(userId, userInfoInTokenBo.getSysType());

        return mapperFacade.map(authAccountEntity, AuthAccountVo.class);
    }

    /**
     * 保存用户信息，生成token，返回前端
     *
     * @param userInfoInTokenBo 账户信息 和社交账号信息
     * @return uid
     */
    public TokenInfoVo storeTokenAndGet(UserInfoInTokenBo userInfoInTokenBo) {
        return tokenStoreSysService.storeAndGetVo(userInfoInTokenBo);
    }

    /**
     * 根据用户名和系统类型获取用户信息
     *
     * @param username 用户名
     * @param sysType  系统类型
     * @return resp
     */
    public AuthAccountVo getByUsername(String username, SysTypeEnum sysType) {
        AuthAccountEntity authAccountEntity = authAccountManage.getUserByUserName(username, sysType.getValue());
        AuthAccountVo vo = new AuthAccountVo();
        BeanUtils.copyProperties(authAccountEntity, vo);
        return vo;
    }

    private AuthAccountEntity verifyUserIsExist(AuthAccountDto authAccountDto) {

        if (!PrincipalUtil.isUserName(authAccountDto.getUsername())) {
            ThrowUtils.throwErr("用户名格式不正确");
        }

        int count = authAccountMapper.selectCount(
                Wrappers.<AuthAccountEntity>lambdaQuery()
                        .eq(AuthAccountEntity::getSysType, SysTypeEnum.MULTISHOP.getValue())
                        .eq(AuthAccountEntity::getUsername, authAccountDto.getUsername())
                        .ne(AuthAccountEntity::getStatus, StatusEnum.DELETE.getValue())
                        .ne(AuthAccountEntity::getUserId, authAccountDto.getUserId())
        );

        if (count > 0) {
            ThrowUtils.throwErr("用户名已存在，请更换用户名再次尝试");
        }

        AuthAccountEntity authAccountEntity = mapperFacade.map(authAccountDto, AuthAccountEntity.class);

        if (StrUtil.isNotBlank(authAccountEntity.getPassword())) {
            authAccountEntity.setPassword(passwordEncoder.encode(authAccountEntity.getPassword()));
        }

        return authAccountEntity;
    }

    /**
     * 根据用户id与用户类型更新用户信息
     *
     * @param userInfoInTokenBo 新的用户信息
     * @param userId            用户id
     * @param sysType           用户类型
     * @return resp
     */
    @Transactional(rollbackFor = Exception.class)
    public Void updateUser(UserInfoInTokenBo userInfoInTokenBo, Long userId, Integer sysType) {

        AuthAccountEntity userEntity = authAccountManage.getByUserIdAndType(userId, sysType);
        userInfoInTokenBo.setUid(userEntity.getUid());

        tokenStoreSysService.updateUserInfoByUidAndAppId(userEntity.getUid(), sysType.toString(), userInfoInTokenBo);

        int res = authAccountMapper.update(null,
                Wrappers.<AuthAccountEntity>lambdaUpdate()
                        .set(AuthAccountEntity::getTenantId, userInfoInTokenBo.getTenantId())
                        .eq(AuthAccountEntity::getUserId, userId)
                        .eq(AuthAccountEntity::getSysType, sysType)
                        .eq(AuthAccountEntity::getStatus, StatusEnum.DELETE.getValue())
                        .last("LIMIT 1")
        );

        if (res != 1) {
            ThrowUtils.throwErr("用户信息错误，更新失败");
        }

        return VOID;
    }

    /**
     * 根据租户id查询商家信息
     *
     * @param tenantId 租户Id
     * @return resp
     */
    public AuthAccountVo getMerchantByTenantId(Long tenantId) {
        return authAccountManage.getMerchantInfoByTenantId(tenantId);
    }
}
