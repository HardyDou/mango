package io.mango.captcha.core.generator;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.CodeGenerator;
import io.mango.captcha.api.constant.CaptchaDifficulty;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.CaptchaResponse;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 算术验证码实现。
 */
public class DefaultArithmeticCaptchaGenerator implements ArithmeticCaptchaGenerator {

    private static final int DEFAULT_WIDTH = 120;
    private static final int DEFAULT_HEIGHT = 40;
    private static final int SIMPLE_MAX_OPERAND = 9;
    private static final int MEDIUM_MAX_OPERAND = 20;
    private static final int COMPLEX_LEFT_MAX_OPERAND = 50;
    private static final int COMPLEX_RIGHT_MAX_OPERAND = 20;
    private static final String SIMPLE_OPERATORS = "+-";
    private static final String MEDIUM_OPERATORS = "+-×";
    private static final String COMPLEX_OPERATORS = "+-";

    @Value("${mango.captcha.ttl:300}")
    private long defaultTtl;

    @Value("${mango.captcha.arithmetic.width:120}")
    private int width = DEFAULT_WIDTH;

    @Value("${mango.captcha.arithmetic.height:40}")
    private int height = DEFAULT_HEIGHT;

    @Override
    public CaptchaResponse generate() {
        return generate(CaptchaDifficulty.SIMPLE);
    }

    @Override
    public CaptchaResponse generate(CaptchaDifficulty difficulty) {
        CaptchaDifficulty actualDifficulty = difficulty == null ? CaptchaDifficulty.SIMPLE : difficulty;
        ArithmeticProblem problem = createProblem(actualDifficulty);
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(width, height);
        captcha.setGenerator(new FixedCodeGenerator(problem.expression(), problem.answer()));
        captcha.createCode();

        CaptchaResponse response = new CaptchaResponse();
        response.setType(CaptchaType.ARITHMETIC);
        response.setDifficulty(actualDifficulty);
        response.setImage("data:image/png;base64," + captcha.getImageBase64());
        response.setExtra(String.valueOf(problem.answer()));
        response.setExpireTime(defaultTtl);
        return response;
    }

    private ArithmeticProblem createProblem(CaptchaDifficulty difficulty) {
        return switch (difficulty) {
            case SIMPLE -> createSingleOperation(SIMPLE_MAX_OPERAND, SIMPLE_OPERATORS);
            case MEDIUM -> createSingleOperation(MEDIUM_MAX_OPERAND, MEDIUM_OPERATORS);
            case COMPLEX -> createTwoStepOperation();
        };
    }

    private ArithmeticProblem createSingleOperation(int maxOperand, String operators) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        char operator = operators.charAt(random.nextInt(operators.length()));
        int operandMax = operator == '×' ? Math.min(maxOperand, SIMPLE_MAX_OPERAND) : maxOperand;
        int left = random.nextInt(1, operandMax + 1);
        int right = random.nextInt(1, operandMax + 1);
        if (operator == '-' && left < right) {
            int temporary = left;
            left = right;
            right = temporary;
        }
        int answer = calculate(left, operator, right);
        return new ArithmeticProblem(left + String.valueOf(operator) + right + "=?", answer);
    }

    private ArithmeticProblem createTwoStepOperation() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int left = random.nextInt(1, COMPLEX_LEFT_MAX_OPERAND + 1);
        int middle = random.nextInt(1, COMPLEX_RIGHT_MAX_OPERAND + 1);
        int right = random.nextInt(1, COMPLEX_RIGHT_MAX_OPERAND + 1);
        char firstOperator = COMPLEX_OPERATORS.charAt(random.nextInt(COMPLEX_OPERATORS.length()));
        char secondOperator = COMPLEX_OPERATORS.charAt(random.nextInt(COMPLEX_OPERATORS.length()));
        int answer = calculate(calculate(left, firstOperator, middle), secondOperator, right);
        if (answer < 0) {
            return new ArithmeticProblem(left + "+" + middle + "+" + right + "=?",
                    left + middle + right);
        }
        return new ArithmeticProblem(left + String.valueOf(firstOperator) + middle
                + secondOperator + right + "=?", answer);
    }

    private int calculate(int left, char operator, int right) {
        return switch (operator) {
            case '+' -> left + right;
            case '-' -> left - right;
            case '×' -> left * right;
            default -> throw new IllegalArgumentException("不支持的算术运算符: " + operator);
        };
    }

    private record ArithmeticProblem(String expression, int answer) {
    }

    private static final class FixedCodeGenerator implements CodeGenerator {

        private final String expression;
        private final int answer;

        private FixedCodeGenerator(String expression, int answer) {
            this.expression = expression;
            this.answer = answer;
        }

        @Override
        public String generate() {
            return expression;
        }

        @Override
        public boolean verify(String code, String userInput) {
            return String.valueOf(answer).equals(userInput);
        }
    }
}
