package io.mango.captcha.core.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.core.service.BlockPuzzleCaptchaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * 滑块验证码实现
 *
 * @author Mango
 */
@Slf4j
@Service
public class BlockPuzzleCaptchaServiceImpl implements BlockPuzzleCaptchaService {

    @Value("${mango.captcha.block-puzzle.width:280}")
    private int width = 280;

    @Value("${mango.captcha.block-puzzle.height:160}")
    private int height = 160;

    @Value("${mango.captcha.block-puzzle.slider-size:50}")
    private int sliderSize = 50;

    @Override
    public CaptchaResponse generate() {
        CaptchaResponse response = new CaptchaResponse();
        response.setType(CaptchaType.BLOCK_PUZZLE);

        try {
            Random random = new Random();
            // 生成滑块位置
            int slipX = random.nextInt(width - sliderSize - 20) + 10;
            int slipY = random.nextInt(height - sliderSize - 20) + 10;

            // 创建背景图
            LineCaptcha captcha = CaptchaUtil.createLineCaptcha(width, height);
            String backgroundBase64 = captcha.getImageBase64();

            response.setBackgroundImage("data:image/png;base64," + backgroundBase64);
            response.setX(slipX);

            log.debug("生成滑块验证码: slipX={}, slipY={}", slipX, slipY);

        } catch (Exception e) {
            log.error("生成滑块验证码失败", e);
            // 降级为简单图形
            LineCaptcha captcha = CaptchaUtil.createLineCaptcha(width, height);
            response.setBackgroundImage("data:image/png;base64," + captcha.getImageBase64());
            response.setX(width / 2);
        }

        return response;
    }
}
