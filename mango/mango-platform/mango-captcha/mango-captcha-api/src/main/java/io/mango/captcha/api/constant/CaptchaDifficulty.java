package io.mango.captcha.api.constant;

/**
 * 图形验证码难度级别。
 */
public enum CaptchaDifficulty {

    /**
     * 单步、低干扰题目，默认级别。
     */
    SIMPLE(1),

    /**
     * 中等干扰或更大数值范围题目。
     */
    MEDIUM(2),

    /**
     * 较强干扰或多步题目。
     */
    COMPLEX(3);

    private final int rank;

    CaptchaDifficulty(int rank) {
        this.rank = rank;
    }

    /**
     * 判断当前级别是否不低于目标级别。
     *
     * @param target 目标级别
     * @return 当前级别是否达到目标级别
     */
    public boolean atLeast(CaptchaDifficulty target) {
        return rank >= target.rank;
    }
}
