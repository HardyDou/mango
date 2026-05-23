package io.mango.infra.fileproc.convert;

import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.convert.ConvertRegistry;
import io.mango.infra.fileproc.convert.convert.IConvertProvider;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConvertRegistryTest {

    @Test
    void registry_defensivelyCopiesInputAndReturnsFirstMatch() {
        IConvertProvider first = converter(ConvertFormat.HTML, ConvertFormat.TEXT, "first");
        IConvertProvider second = converter(ConvertFormat.HTML, ConvertFormat.TEXT, "second");
        List<IConvertProvider> source = new ArrayList<>(List.of(first));

        ConvertRegistry registry = new ConvertRegistry(source);
        source.add(second);

        assertThat(registry.providers()).containsExactly(first);
        assertThat(registry.findProvider(ConvertFormat.HTML, ConvertFormat.TEXT)).contains(first);
        assertThatThrownBy(() -> registry.providers().add(second))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void registry_returnsEmptyWhenNoConverterMatches() {
        ConvertRegistry registry = new ConvertRegistry(List.of(converter(ConvertFormat.DOCX, ConvertFormat.PDF, "pdf")));

        assertThat(registry.findProvider(ConvertFormat.HTML, ConvertFormat.TEXT)).isEmpty();
    }

    private static IConvertProvider converter(ConvertFormat source, ConvertFormat target, String fileName) {
        return new IConvertProvider() {
            @Override
            public boolean supports(ConvertFormat sourceFormat, ConvertFormat targetFormat) {
                return source == sourceFormat && target == targetFormat;
            }

            @Override
            public ConvertResultVO convert(ConvertCommand request) {
                return ConvertResultVO.builder()
                        .format(target)
                        .fileName(fileName)
                        .content(new byte[0])
                        .build();
            }
        };
    }
}
