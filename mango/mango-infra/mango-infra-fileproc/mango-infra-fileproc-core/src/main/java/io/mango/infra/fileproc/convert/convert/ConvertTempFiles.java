package io.mango.infra.fileproc.convert.convert;

import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 转换临时文件处理工具。
 */
final class ConvertTempFiles {

    private ConvertTempFiles() {
    }

    static Path createWorkDir() {
        try {
            return Files.createTempDirectory("mango-convert-");
        } catch (IOException ex) {
            throw new ConvertToolException("创建转换临时目录失败", ex);
        }
    }

    static Path writeInput(Path workDir, ConvertCommand command) {
        if (command.hasSourcePath()) {
            return command.sourcePath();
        }
        String extension = ConvertFileNames.extension(command.fileName(), command.sourceFormat());
        try {
            Path inputFile = Files.createTempFile(workDir, "source-", "." + extension);
            Files.copy(command.inputStream(), inputFile, StandardCopyOption.REPLACE_EXISTING);
            return inputFile;
        } catch (IOException ex) {
            throw new ConvertToolException("写入转换源文件失败", ex);
        }
    }

    static byte[] readIfNeeded(ConvertCommand command, Path outputFile) {
        if (command.hasTargetPath()) {
            return new byte[0];
        }
        try {
            return Files.readAllBytes(outputFile);
        } catch (IOException ex) {
            throw new ConvertToolException("读取转换结果失败", ex);
        }
    }

    static Path output(Path workDir, ConvertCommand command, ConvertFormat targetFormat) {
        if (command.hasTargetPath()) {
            createParent(command.targetPath());
            return command.targetPath();
        }
        return output(workDir, targetFormat);
    }

    static Path output(Path workDir, ConvertFormat targetFormat) {
        return workDir.resolve("converted." + targetFormat.extension());
    }

    static void createParent(Path path) {
        if (path == null || path.getParent() == null) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException ex) {
            throw new ConvertToolException("创建转换输出目录失败", ex);
        }
    }

    static void deleteQuietly(Path workDir) {
        if (workDir != null) {
            FileUtils.deleteQuietly(workDir.toFile());
        }
    }
}
