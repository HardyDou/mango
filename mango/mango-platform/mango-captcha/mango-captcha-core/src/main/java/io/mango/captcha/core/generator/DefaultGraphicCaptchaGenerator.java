package io.mango.captcha.core.generator;

import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import io.mango.captcha.api.constant.CaptchaDifficulty;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.constant.GraphicCaptchaType;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.core.properties.GraphicCaptchaProperties;

/**
 * 默认字母数字图形验证码生成器。
 */
public class DefaultGraphicCaptchaGenerator implements GraphicCaptchaGenerator {

    private final GraphicCaptchaProperties properties;

    public DefaultGraphicCaptchaGenerator() {
        this(new GraphicCaptchaProperties());
    }

    public DefaultGraphicCaptchaGenerator(GraphicCaptchaProperties properties) {
        this.properties = properties;
    }

    @Override
    public CaptchaResponse generate(CaptchaDifficulty difficulty) {
        CaptchaDifficulty actualDifficulty = difficulty == null ? CaptchaDifficulty.SIMPLE : difficulty;
        int length = switch (actualDifficulty) {
            case SIMPLE -> 4;
            case MEDIUM -> 5;
            case COMPLEX -> 6;
        };
        if (properties.getCharacters() == null || properties.getCharacters().isBlank()) {
            throw new IllegalStateException("图形验证码字符集不能为空");
        }

        LineCaptcha captcha = new LineCaptcha(
                properties.getWidth(),
                properties.getHeight(),
                new RandomGenerator(properties.getCharacters(), length),
                properties.getInterfereCount());
        captcha.createCode();

        CaptchaResponse response = new CaptchaResponse();
        response.setType(CaptchaType.GRAPHIC);
        response.setGraphicType(GraphicCaptchaType.ALPHANUMERIC);
        response.setDifficulty(actualDifficulty);
        response.setImage("data:image/png;base64," + captcha.getImageBase64());
        response.setExtra(captcha.getCode());
        return response;
    }
}
