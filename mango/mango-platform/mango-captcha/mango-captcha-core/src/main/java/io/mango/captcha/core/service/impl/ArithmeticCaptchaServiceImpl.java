package io.mango.captcha.core.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.MathGenerator;
import cn.hutool.captcha.generator.RandomGenerator;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.core.service.ArithmeticCaptchaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Random;

/**
 * 算术验证码实现
 *
 * @author Mango
 */
@Slf4j
@Service
public class ArithmeticCaptchaServiceImpl implements ArithmeticCaptchaService {

    @Value("${mango.captcha.ttl:300}")
    private long defaultTtl;

    @Value("${mango.captcha.arithmetic.width:120}")
    private int width = 120;

    @Value("${mango.captcha.arithmetic.height:40}")
    private int height = 40;

    @Value("${mango.captcha.arithmetic.count:1}")
    private int count = 1;

    @Value("${mango.captcha.arithmetic.threshold:80}")
    private int threshold = 80;

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
        if (exp.contains("+")) {
            String[] parts = exp.split("\\+");
            return Integer.parseInt(parts[0].trim()) + Integer.parseInt(parts[1].trim());
        } else if (exp.contains("-")) {
            String[] parts = exp.split("-");
            return Integer.parseInt(parts[0].trim()) - Integer.parseInt(parts[1].trim());
        } else if (exp.contains("×")) {
            String[] parts = exp.split("×");
            return Integer.parseInt(parts[0].trim()) * Integer.parseInt(parts[1].trim());
        }
        return 0;
    }
}
