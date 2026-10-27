package io.mango.captcha.core.properties;

import io.mango.captcha.api.constant.CaptchaDifficulty;
import io.mango.captcha.api.constant.GraphicCaptchaType;
import lombok.Data;
import java.util.List;

/**
 * Unified graphic captcha policy and rendering configuration.
 */
@Data
public class GraphicCaptchaProperties {

    private static final int DEFAULT_WIDTH = 120;
    private static final int DEFAULT_HEIGHT = 40;
    private static final int DEFAULT_INTERFERE_COUNT = 1;
    private static final String DEFAULT_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ";

    private GraphicCaptchaType defaultType = GraphicCaptchaType.ALPHANUMERIC;
    private CaptchaDifficulty defaultLevel = CaptchaDifficulty.SIMPLE;
    private List<GraphicCaptchaType> allowedTypes = List.of(
            GraphicCaptchaType.ARITHMETIC, GraphicCaptchaType.ALPHANUMERIC);
    private List<CaptchaDifficulty> allowedLevels = List.of(
            CaptchaDifficulty.SIMPLE, CaptchaDifficulty.MEDIUM, CaptchaDifficulty.COMPLEX);
    private int width = DEFAULT_WIDTH;
    private int height = DEFAULT_HEIGHT;
    private int interfereCount = DEFAULT_INTERFERE_COUNT;
    private String characters = DEFAULT_CHARACTERS;
}
