package io.mango.captcha.api.dto;

import io.mango.captcha.api.constant.CaptchaType;
import lombok.Data;

import java.io.Serializable;

/**
 * 验证码响应
 *
 * @author Mango
 */
@Data
public class CaptchaResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 验证码key - 用于后续验证
     */
    private String key;

    /**
     * 验证码类型
     */
    private CaptchaType type;

    /**
     * 图片Base64（算术/滑块）
     */
    private String image;

    /**
     * 滑块背景图
     */
    private String backgroundImage;

    /**
     * 滑块图片
     */
    private String sliderImage;

    /**
     * 滑块X坐标（服务端校验用，不返回前端）
     */
    private Integer x;

    /**
     * 过期时间（秒）
     */
    private Long expireTime;

    /**
     * 目标（手机号/邮箱）
     */
    private String target;

    /**
     * 额外数据（算术验证码的答案等）
     */
    private String extra;
}
