package io.mango.captcha.core.generator;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.MathGenerator;
import cn.hutool.captcha.generator.RandomGenerator;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.constant.CaptchaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.Base64;
import java.util.Random;

/**
 * 算术验证码实现
 *
 * @author Mango
 */
@Slf4j
public class DefaultArithmeticCaptchaGenerator implements ArithmeticCaptchaGenerator {

    private static final int DEFAULT_WIDTH = 120;
    private static final int DEFAULT_HEIGHT = 40;
    private static final String OPERATOR_ADD = "+";
    private static final String OPERATOR_SUBTRACT = "-";
    private static final String OPERATOR_MULTIPLY = "×";

    @Value("${mango.captcha.ttl:300}")
    private long defaultTtl;

    @Value("${mango.captcha.arithmetic.width:120}")
    private int width = DEFAULT_WIDTH;

    @Value("${mango.captcha.arithmetic.height:40}")
    private int height = DEFAULT_HEIGHT;

    @Override
    public CaptchaResponse generate() {
        // 使用数学表达式验证码 - 使用默认构造器
        MathGenerator mathGenerator = new MathGenerator();
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(width, height);
        captcha.setGenerator(mathGenerator);
        captcha.createCode();
        String code = captcha.getCode();

        // 计算实际答案
        int answer = calculateAnswer(code);
        String imageBase64 = captcha.getImageBase64();

        CaptchaResponse response = new CaptchaResponse();
        response.setType(CaptchaType.ARITHMETIC);
        response.setImage("data:image/png;base64," + imageBase64);
        response.setExtra(String.valueOf(answer));
        response.setExpireTime(defaultTtl);

        log.debug("生成算术验证码: code={}, answer={}", code, answer);

        return response;
    }

    private int calculateAnswer(String expression) {
        // 解析 "3+2=?" 格式的表达式
        try {
            String exp = expression.replace("=", "?");
            String[] parts = exp.split("[?]");
            if (parts.length > 0) {
                String mathExp = parts[0].trim();
                return evaluateExpression(mathExp);
            }
        } catch (Exception e) {
            log.error("解析验证码表达式失败: {}", expression, e);
        }
        return 0;
    }

    private int evaluateExpression(String exp) {
        exp = exp.trim();
        if (exp.contains(OPERATOR_ADD)) {
            String[] parts = exp.split("\\+");
            return Integer.parseInt(parts[0].trim()) + Integer.parseInt(parts[1].trim());
        } else if (exp.contains(OPERATOR_SUBTRACT)) {
            String[] parts = exp.split(OPERATOR_SUBTRACT);
            return Integer.parseInt(parts[0].trim()) - Integer.parseInt(parts[1].trim());
        } else if (exp.contains(OPERATOR_MULTIPLY)) {
            String[] parts = exp.split(OPERATOR_MULTIPLY);
            return Integer.parseInt(parts[0].trim()) * Integer.parseInt(parts[1].trim());
        }
        return 0;
    }
}
