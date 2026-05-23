package io.mango.infra.fileproc.convert.convert;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 同格式直通复制转换器。
 */
public class SameFormatConverter implements IConvertProvider {

    @Override
    public boolean supports(ConvertFormat sourceFormat, ConvertFormat targetFormat) {
        return sourceFormat != null && sourceFormat == targetFormat;
    }

    @Override
    public ConvertResultVO convert(ConvertCommand command) {
        Require.notNull(command, "转换命令不能为空");
        try {
            if (command.hasTargetPath()) {
                ConvertTempFiles.createParent(command.targetPath());
                Files.copy(command.inputStream(), command.targetPath(), StandardCopyOption.REPLACE_EXISTING);
                return ConvertResultVO.builder()
                        .format(command.targetFormat())
                        .fileName(resolveFileName(command))
                        .contentType(command.targetFormat().contentType())
                        .outputPath(command.targetPath())
                        .build();
            }
            return ConvertResultVO.builder()
                    .format(command.targetFormat())
                    .fileName(resolveFileName(command))
                    .contentType(command.targetFormat().contentType())
                    .content(command.inputStream().readAllBytes())
                    .build();
        } catch (IOException ex) {
            throw new ConvertToolException("同格式内容复制失败", ex);
        }
    }

    private String resolveFileName(ConvertCommand command) {
        if (command.fileName() == null || command.fileName().isBlank()) {
            return null;
        }
        String fileName = command.fileName().trim();
        String extension = "." + command.targetFormat().extension();
        if (fileName.toLowerCase().endsWith(extension.toLowerCase())) {
            return fileName;
        }
        return fileName + extension;
    }
}
