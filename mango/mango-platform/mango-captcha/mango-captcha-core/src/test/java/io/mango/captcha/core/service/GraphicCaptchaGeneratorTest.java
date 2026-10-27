package io.mango.captcha.core.service;

import io.mango.captcha.api.constant.CaptchaDifficulty;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.constant.GraphicCaptchaType;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.core.generator.DefaultGraphicCaptchaGenerator;
import io.mango.captcha.core.properties.GraphicCaptchaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GraphicCaptchaGeneratorTest {

    private DefaultGraphicCaptchaGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new DefaultGraphicCaptchaGenerator();
    }

    @Test
    void generate_simple_returnsFourCharacterAlphanumericCode() {
        CaptchaResponse response = generator.generate(CaptchaDifficulty.SIMPLE);

        assertThat(response.getType()).isEqualTo(CaptchaType.GRAPHIC);
        assertThat(response.getGraphicType()).isEqualTo(GraphicCaptchaType.ALPHANUMERIC);
        assertThat(response.getDifficulty()).isEqualTo(CaptchaDifficulty.SIMPLE);
        assertThat(response.getExtra()).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ]{4}");
        assertThat(response.getImage()).startsWith("data:image/png;base64,");
    }

    @Test
    void defaultGraphicPolicyUsesLettersAndOneInterferenceLine() {
        GraphicCaptchaProperties properties = new GraphicCaptchaProperties();

        assertThat(properties.getDefaultType()).isEqualTo(GraphicCaptchaType.ALPHANUMERIC);
        assertThat(properties.getInterfereCount()).isOne();
        assertThat(properties.getCharacters()).isEqualTo("ABCDEFGHJKLMNPQRSTUVWXYZ");
    }

    @Test
    void generate_mediumAndComplex_increaseCodeLength() {
        assertThat(generator.generate(CaptchaDifficulty.MEDIUM).getExtra()).hasSize(5);
        assertThat(generator.generate(CaptchaDifficulty.COMPLEX).getExtra()).hasSize(6);
    }
}
