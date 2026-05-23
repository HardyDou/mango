package io.mango.infra.fileproc.convert.convert;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.convert.ConvertApi;
import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertFormatPairVO;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;

import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 默认格式转换能力实现。
 */
public class DefaultConvertApi implements ConvertApi {

    private final ConvertRegistry registry;

    public DefaultConvertApi(ConvertRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean canConvert(ConvertFormat sourceFormat, ConvertFormat targetFormat) {
        if (sourceFormat == null || targetFormat == null) {
            return false;
        }
        if (sourceFormat == targetFormat) {
            return true;
        }
        return registry.findProvider(sourceFormat, targetFormat).isPresent();
    }

    @Override
    public ConvertResultVO convert(ConvertCommand command) {
        Require.notNull(command, "转换命令不能为空");
        if (command.sourceFormat() == command.targetFormat()) {
            return complete(command, new SameFormatConverter().convert(command));
        }
        IConvertProvider provider = registry.findProvider(command.sourceFormat(), command.targetFormat())
                .orElseThrow(() -> new ConvertToolException(
                        "不支持的格式转换：" + command.sourceFormat() + " -> " + command.targetFormat()));
        return complete(command, provider.convert(command));
    }

    @Override
    public Set<ConvertFormatPairVO> supportedConversions() {
        Set<ConvertFormatPairVO> pairs = new LinkedHashSet<>();
        for (ConvertFormat sourceFormat : ConvertFormat.values()) {
            for (ConvertFormat targetFormat : ConvertFormat.values()) {
                if (sourceFormat != targetFormat && canConvert(sourceFormat, targetFormat)) {
                    pairs.add(new ConvertFormatPairVO(sourceFormat, targetFormat));
                }
            }
        }
        return pairs;
    }

    private ConvertResultVO complete(ConvertCommand command, ConvertResultVO result) {
        if (!command.hasTargetPath() || result.hasOutputPath()) {
            return result;
        }
        try {
            ConvertTempFiles.createParent(command.targetPath());
            Files.write(command.targetPath(), result.content());
            return ConvertResultVO.builder()
                    .format(result.format())
                    .fileName(result.fileName())
                    .contentType(result.contentType())
                    .outputPath(command.targetPath())
                    .build();
        } catch (IOException ex) {
            throw new ConvertToolException("写入转换目标文件失败", ex);
        }
    }
}
