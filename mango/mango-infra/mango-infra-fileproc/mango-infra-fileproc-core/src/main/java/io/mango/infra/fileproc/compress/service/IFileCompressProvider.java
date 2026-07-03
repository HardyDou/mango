package io.mango.infra.fileproc.compress.service;

import io.mango.infra.fileproc.compress.command.CompressFileCommand;
import io.mango.infra.fileproc.compress.vo.CompressFileResultVO;

/**
 * 文件压缩实现提供者。
 */
public interface IFileCompressProvider {

    boolean supports(String fileName, String contentType);

    CompressFileResultVO compress(CompressFileCommand command);
}
