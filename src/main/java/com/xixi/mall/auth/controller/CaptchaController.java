package com.xixi.mall.auth.controller;

import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.xixi.mall.common.core.aop.PackResponseEnhance;
import com.xixi.mall.common.core.webbase.vo.ServerResponse;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/ua/captcha")
@Api(tags = "验证码")
public class CaptchaController {

    @Resource
    private CaptchaService captchaService;

    /**
     * 获取验证码
     *
     * @param captchaVo vo
     * @return 验证码
     */
    @PostMapping({"/get"})
    public ServerResponse<ResponseModel> get(@RequestBody CaptchaVO captchaVo) {
        return PackResponseEnhance.enhance(() -> captchaService.get(captchaVo));
    }

    /**
     * 校验验证码
     *
     * @param captchaVo vo
     * @return 校验结果
     */
    @PostMapping({"/check"})
    public ServerResponse<ResponseModel> check(@RequestBody CaptchaVO captchaVo) {
        return PackResponseEnhance.enhance(() -> captchaService.check(captchaVo));
    }

}
