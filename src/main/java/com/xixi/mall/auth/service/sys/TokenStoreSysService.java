package com.xixi.mall.auth.service.sys;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.xixi.mall.api.auth.bo.UserInfoInTokenBo;
import com.xixi.mall.api.auth.constant.SysTypeEnum;
import com.xixi.mall.api.auth.vo.TokenInfoVo;
import com.xixi.mall.common.cache.constant.CacheNames;
import com.xixi.mall.common.core.enums.ResponseEnum;
import com.xixi.mall.common.core.utils.PrincipalUtil;
import com.xixi.mall.common.core.utils.ThrowUtils;
import com.xixi.mall.common.security.bo.TokenInfoBo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.xixi.mall.common.core.constant.Constant.VOID;

/**
 * token管理 1. 登陆返回token 2. 刷新token 3. 清除用户过去token 4. 校验token
 */
@Component
@RefreshScope
public class TokenStoreSysService {

    private static final Logger logger = LoggerFactory.getLogger(TokenStoreSysService.class);

    private final RedisTemplate<Object, Object> redisTemplate;

    private final RedisSerializer<Object> redisSerializer;

    private final StringRedisTemplate stringRedisTemplate;


    /**
     * 普通用户token过期时间  1小时
     * 以秒为单位
     */
    private static final int GENERAL_USER_EXPIRES = 3600;

    /**
     * 系统管理远token过期时间  2小时
     * 以秒为单位
     */
    private static final int ADMIN_USER_EXPIRES = 7200;

    /**
     * 传递给前端的加密token数据由真实token + 系统当前时间 + 系统类型组成
     * 该字段为真实token的长度
     */
    private static final int WEB_TOKEN_PREFIX_LENGTH = 32;


    public TokenStoreSysService(RedisTemplate<Object, Object> redisTemplate,
                                RedisSerializer<Object> redisSerializer,
                                StringRedisTemplate stringRedisTemplate) {

        this.redisTemplate = redisTemplate;
        this.redisSerializer = redisSerializer;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 生成用户token并缓存
     *
     * @param userInfoInToken 用户在token中的信息
     * @return token信息
     */
    public TokenInfoBo storeAccessToken(UserInfoInTokenBo userInfoInToken) {

        TokenInfoBo tokenInfoBo = new TokenInfoBo();

        String accessToken = IdUtil.simpleUUID();
        String refreshToken = IdUtil.simpleUUID();

        tokenInfoBo.setUserInfoInToken(userInfoInToken);
        tokenInfoBo.setExpiresIn(getExpiresIn(userInfoInToken.getSysType()));

        String uidToAccessKeyStr = getUidToAccessKey(getApprovalKey(userInfoInToken));
        String accessKeyStr = getAccessKey(accessToken);
        String refreshToAccessKeyStr = getRefreshToAccessKey(refreshToken);

        // 一个用户会登陆很多次，每次登陆的token都会存在 uid_to_access里面
        // 但是每次保存都会更新这个key的时间，而key里面的token有可能会过期，过期就要移除掉
        List<String> existsAccessTokens = new LinkedList<>();

        existsAccessTokens.add(accessToken + StrUtil.COLON + refreshToken); // 新的token数据

        Optional.ofNullable(redisTemplate.opsForSet().size(uidToAccessKeyStr)) //移除过期token
                .filter(size -> size != 0)
                .map(size -> stringRedisTemplate.opsForSet().pop(uidToAccessKeyStr, size))
                .filter(CollectionUtil::isNotEmpty)
                .ifPresent(tokenInfoBoList -> tokenInfoBoList.forEach(
                        accessTokenWithRefreshToken -> {
                            String accessTokenData = accessTokenWithRefreshToken.split(StrUtil.COLON)[0];
                            if (BooleanUtil.isTrue(stringRedisTemplate.hasKey(getAccessKey(accessTokenData)))) {
                                existsAccessTokens.add(accessTokenWithRefreshToken);
                            }
                        })
                );

        redisTemplate.executePipelined((RedisCallback<Void>) connection -> {

            long expiresIn = tokenInfoBo.getExpiresIn();

            byte[] uidKey = uidToAccessKeyStr.getBytes(StandardCharsets.UTF_8),
                    refreshKey = refreshToAccessKeyStr.getBytes(StandardCharsets.UTF_8),
                    accessKey = accessKeyStr.getBytes(StandardCharsets.UTF_8);

            for (String existsAccessToken : existsAccessTokens) {
                connection.sAdd(uidKey, existsAccessToken.getBytes(StandardCharsets.UTF_8));
            }

            // 通过uid + sysType 保存access_token，当需要禁用用户的时候，可以根据uid + sysType 禁用用户
            connection.expire(uidKey, expiresIn);

            // 通过refresh_token获取用户的access_token从而刷新token
            connection.setEx(refreshKey, expiresIn, accessToken.getBytes(StandardCharsets.UTF_8));

            // 通过access_token保存用户的租户id，用户id，uid
            connection.setEx(accessKey, expiresIn, Objects.requireNonNull(redisSerializer.serialize(userInfoInToken)));

            return VOID;
        });

        // 返回给前端是加密的token
        tokenInfoBo.setAccessToken(encryptToken(accessToken, userInfoInToken.getSysType()));
        tokenInfoBo.setRefreshToken(encryptToken(refreshToken, userInfoInToken.getSysType()));

        return tokenInfoBo;
    }

    private int getExpiresIn(int sysType) {

        // 普通用户
        if (Objects.equals(sysType, SysTypeEnum.ORDINARY.getValue())) {
            return GENERAL_USER_EXPIRES;
        }

        // 商家平台管理员过期时间 2小时
        if (Objects.equals(sysType, SysTypeEnum.MULTISHOP.getValue())
                || Objects.equals(sysType, SysTypeEnum.PLATFORM.getValue())) {
            return ADMIN_USER_EXPIRES;
        }

        return GENERAL_USER_EXPIRES;
    }

    /**
     * 根据accessToken 获取用户信息
     *
     * @param accessToken accessToken
     * @param needDecrypt 是否需要解密
     * @return 用户信息
     */
    public UserInfoInTokenBo getUserInfoByAccessToken(String accessToken, Boolean needDecrypt) {

        if (StrUtil.isBlank(accessToken)) {
            ThrowUtils.throwErr("accessToken is blank");
        }

        String realAccessToken = needDecrypt
                ? decryptToken(accessToken)
                : accessToken;

        UserInfoInTokenBo userInfoInTokenBo = (UserInfoInTokenBo) redisTemplate.opsForValue()
                .get(getAccessKey(realAccessToken));

        if (userInfoInTokenBo == null) {
            ThrowUtils.throwErr("accessToken 已过期");
        }

        return userInfoInTokenBo;
    }

    /**
     * 刷新token，并返回新的token
     *
     * @param refreshToken 被刷新的token
     * @return 新的token信息
     */
    public TokenInfoBo refreshToken(String refreshToken) {

        if (StrUtil.isBlank(refreshToken)) {
            ThrowUtils.throwErr("refreshToken is blank");
        }

        String realRefreshToken = decryptToken(refreshToken);

        String accessToken = stringRedisTemplate.opsForValue().get(getRefreshToAccessKey(realRefreshToken));

        if (StrUtil.isBlank(accessToken)) {
            ThrowUtils.throwErr("refreshToken 已过期");
        }

        UserInfoInTokenBo userInfoInTokenBo = getUserInfoByAccessToken(accessToken, false);

        // 删除旧的refresh_token
        stringRedisTemplate.delete(getRefreshToAccessKey(realRefreshToken));

        // 删除旧的access_token
        stringRedisTemplate.delete(getAccessKey(accessToken));

        // 保存一份新的token
        return storeAccessToken(userInfoInTokenBo);
    }

    /**
     * 删除全部的token
     */
    public void deleteAllToken(String appId, Long uid) {

        String uidKey = getUidToAccessKey(getApprovalKey(appId, uid));

        Long size = redisTemplate.opsForSet().size(uidKey);

        if (size == null || size == 0) return;

        List<String> tokenInfoBoList = stringRedisTemplate.opsForSet().pop(uidKey, size);

        if (CollUtil.isEmpty(tokenInfoBoList)) return;

        for (String accessTokenWithRefreshToken : tokenInfoBoList) {

            String[] accessTokenWithRefreshTokenArr = accessTokenWithRefreshToken.split(StrUtil.COLON);

            String accessToken = accessTokenWithRefreshTokenArr[0];
            String refreshToken = accessTokenWithRefreshTokenArr[1];

            redisTemplate.delete(getRefreshToAccessKey(refreshToken));
            redisTemplate.delete(getAccessKey(accessToken));
        }

        redisTemplate.delete(uidKey);
    }

    /**
     * sysType : uid
     *
     * @param userInfoInToken 用户token信息
     * @return sysType : uid
     */
    private static String getApprovalKey(UserInfoInTokenBo userInfoInToken) {
        return getApprovalKey(userInfoInToken.getSysType().toString(), userInfoInToken.getUid());
    }

    /**
     * sysType : uid
     *
     * @param sysType 系统类型
     * @param uid     uid
     * @return sysType : uid
     */
    private static String getApprovalKey(String sysType, Long uid) {
        return Objects.isNull(uid)
                ? sysType
                : sysType + StrUtil.COLON + uid;
    }

    /**
     * base64加密token
     *
     * @param token   token
     * @param sysType 系统类型
     * @return 加密token
     */
    private String encryptToken(String token, Integer sysType) {
        return Base64.encode(token + System.currentTimeMillis() + sysType);
    }

    /**
     * 解密token
     *
     * @param token token
     * @return 解密后的token
     */
    private String decryptToken(String token) {

        String decryptToken = null,
                decryptStr = null;

        try {

            decryptStr = Base64.decodeStr(token);
            decryptToken = decryptStr.substring(0, WEB_TOKEN_PREFIX_LENGTH);

        } catch (Exception e) {
            logger.error(e.getMessage());
            ThrowUtils.throwErr("token 格式有误");
        }

        // 创建token的时间
        long createTokenTime = Long.parseLong(decryptStr.substring(WEB_TOKEN_PREFIX_LENGTH, 45));

        // 系统类型
        int sysType = Integer.parseInt(decryptStr.substring(45));

        // token的过期时间
        int expiresIn = getExpiresIn(sysType);

        if (System.currentTimeMillis() - createTokenTime > expiresIn) {
            ThrowUtils.throwErr("token 格式有误");
        }

        // 防止解密后的token是脚本，从而对redis进行攻击，uuid只能是数字和小写字母
        if (!PrincipalUtil.isSimpleChar(decryptToken)) {
            ThrowUtils.throwErr("token 格式有误");
        }

        return decryptToken;
    }

    /**
     * access:token
     *
     * @param accessToken token
     * @return access:token
     */
    public String getAccessKey(String accessToken) {
        return CacheNames.ACCESS + accessToken;
    }

    /**
     * uid_to_access:sysType:uid
     *
     * @param approvalKey sysType:uid
     * @return uid_to_access:sysType:uid
     */
    public String getUidToAccessKey(String approvalKey) {
        return CacheNames.UID_TO_ACCESS + approvalKey;
    }

    /**
     * refresh_to_access:refreshToken
     *
     * @param refreshToken refreshToken
     * @return refresh_to_access:refreshToken
     */
    public String getRefreshToAccessKey(String refreshToken) {
        return CacheNames.REFRESH_TO_ACCESS + refreshToken;
    }

    public TokenInfoVo storeAndGetVo(UserInfoInTokenBo userInfoInToken) {

        TokenInfoBo tokenInfoBo = storeAccessToken(userInfoInToken);

        return new TokenInfoVo()
                .setAccessToken(tokenInfoBo.getAccessToken())
                .setRefreshToken(tokenInfoBo.getRefreshToken())
                .setExpiresIn(tokenInfoBo.getExpiresIn());
    }

    /**
     * 更新用户token
     *
     * @param uid               用户Id
     * @param sysType           系统
     * @param userInfoInTokenBo 用户token信息
     */
    public void updateUserInfoByUidAndAppId(Long uid, String sysType, UserInfoInTokenBo userInfoInTokenBo) {

        if (Objects.isNull(userInfoInTokenBo)) {
            return;
        }

        String uidToAccessKeyStr = getUidToAccessKey(getApprovalKey(sysType, uid));

        Set<String> accessRefreshTokenSet = stringRedisTemplate.opsForSet().members(uidToAccessKeyStr);

        if (CollectionUtil.isEmpty(accessRefreshTokenSet)) {
            ThrowUtils.throwErr(ResponseEnum.UNAUTHORIZED);
        }

        for (String accessTokenWithRefreshToken : accessRefreshTokenSet) {

            String[] accessTokenWithRefreshTokenArr = accessTokenWithRefreshToken.split(StrUtil.COLON);

            String accessKey = this.getAccessKey(accessTokenWithRefreshTokenArr[0]);

            UserInfoInTokenBo oldUserInfoInTokenBo = (UserInfoInTokenBo) redisTemplate.opsForValue().get(accessKey);

            if (oldUserInfoInTokenBo == null) {
                continue;
            }

            BeanUtils.copyProperties(userInfoInTokenBo, oldUserInfoInTokenBo);
            redisTemplate.opsForValue().set(accessKey, Objects.requireNonNull(userInfoInTokenBo), getExpiresIn(userInfoInTokenBo.getSysType()), TimeUnit.SECONDS);

        }
    }
}
