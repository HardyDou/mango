package io.mango.infra.fileproc.compress.service;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.compress.FileCompressApi;
import io.mango.infra.fileproc.compress.command.CompressFileCommand;
import io.mango.infra.fileproc.compress.enums.FileCompression;
import io.mango.infra.fileproc.compress.vo.CompressFileResultVO;

import java.util.List;

/**
 * 默认文件压缩门面。
 */
public class DefaultFileCompressApi implements FileCompressApi {

    private final List<IFileCompressProvider> providers;

    public DefaultFileCompressApi(List<IFileCompressProvider> providers) {
        this.providers = providers == null ? List.of() : List.copyOf(providers);
    }

    @Override
    public boolean supports(String fileName, String contentType) {
        return providers.stream().anyMatch(provider -> provider.supports(fileName, contentType));
    }

    @Override
    public CompressFileResultVO compress(CompressFileCommand command) {
        Require.notNull(command, "文件压缩命令不能为空");
        if (!command.resolvedCompression().enabled()) {
            byte[] source = command.readAllBytes();
            return new CompressFileResultVO(command.fileName(), command.contentType(), source,
                    source.length, source.length, command.targetSizeBytes(), targetReached(source.length, command));
        }
        return providers.stream()
                .filter(provider -> provider.supports(command.fileName(), command.contentType()))
                .findFirst()
                .orElseThrow(() -> new CompressionToolException("当前文件类型不支持压缩"))
                .compress(command);
    }

    private boolean targetReached(long size, CompressFileCommand command) {
        return command.targetSizeBytes() == null || size <= command.targetSizeBytes();
    }
}
