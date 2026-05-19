package io.mango.captcha.api.dto;

import io.mango.captcha.api.constant.CaptchaType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 验证码响应
 *
 * @author Mango
 */
@Data
@Schema(description = "验证码响应")
public class CaptchaResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 验证码key - 用于后续验证
     */
    @Schema(description = "验证码键，用于后续校验")
    private String key;

    /**
     * 验证码类型
     */
    @Schema(description = "验证码类型")
    private CaptchaType type;

    /**
     * 图片Base64（算术/滑块）
     */
    @Schema(description = "验证码图片 Base64")
    private String image;

    /**
     * 滑块背景图
     */
    @Schema(description = "滑块背景图 Base64")
    private String backgroundImage;

    /**
     * 滑块图片
     */
    @Schema(description = "滑块图片 Base64")
    private String sliderImage;

    /**
     * 滑块背景图生成宽度
     */
    @Schema(description = "滑块背景图生成宽度，用于前端等比缩放坐标")
    private Integer backgroundWidth;

    /**
     * 滑块背景图生成高度
     */
    @Schema(description = "滑块背景图生成高度，用于前端等比缩放坐标")
    private Integer backgroundHeight;

    /**
     * 滑块拼图片生成尺寸
     */
    @Schema(description = "滑块拼图片生成尺寸，用于前端等比缩放拼图片")
    private Integer sliderSize;

    /**
     * 滑块X坐标（服务端校验用，不返回前端）
     */
    @Schema(description = "滑块 X 坐标，服务端校验用")
    private Integer x;

    /**
     * 滑块Y坐标
     */
    @Schema(description = "滑块 Y 坐标，用于前端渲染拼图块")
    private Integer y;

    /**
     * 过期时间（秒）
     */
    @Schema(description = "过期时间，单位秒")
    private Long expireTime;

    /**
     * 目标（手机号/邮箱）
     */
    @Schema(description = "验证码发送目标，例如手机号或邮箱")
    private String target;

    /**
     * 额外数据（算术验证码的答案等）
     */
    @Schema(description = "额外数据")
    private String extra;
}
