package io.mango.infra.fileproc.convert;

import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConvertFormatTest {

    @Test
    void parse_supportsEnumNamesAndExtensions() {
        assertThat(ConvertFormat.parse("text")).contains(ConvertFormat.TEXT);
        assertThat(ConvertFormat.parse("HTML")).contains(ConvertFormat.HTML);
        assertThat(ConvertFormat.parse("docx")).contains(ConvertFormat.DOCX);
        assertThat(ConvertFormat.parse("JPG")).contains(ConvertFormat.JPEG);
        assertThat(ConvertFormat.parse("jpg")).contains(ConvertFormat.JPEG);
        assertThat(ConvertFormat.parse("pdf")).contains(ConvertFormat.PDF);
    }

    @Test
    void parse_rejectsBlankAndUnknownValues() {
        assertThat(ConvertFormat.parse(null)).isEmpty();
        assertThat(ConvertFormat.parse("")).isEmpty();
        assertThat(ConvertFormat.parse("   ")).isEmpty();
        assertThat(ConvertFormat.parse("unknown")).isEmpty();
    }
}
