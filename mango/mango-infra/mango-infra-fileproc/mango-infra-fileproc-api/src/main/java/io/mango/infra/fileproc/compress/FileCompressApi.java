package io.mango.infra.fileproc.compress;

import io.mango.infra.fileproc.compress.command.CompressFileCommand;
import io.mango.infra.fileproc.compress.vo.CompressFileResultVO;

/**
 * 文件压缩能力契约。
 * <p>
 * 本接口只处理文件内容压缩，不处理文件中心 ID、租户、权限或存储生命周期。
 */
public interface FileCompressApi {

    /**
     * 判断当前文件是否支持压缩。
     *
     * @param fileName 文件名。
     * @param contentType 内容类型。
     * @return 支持压缩返回 true。
     */
    boolean supports(String fileName, String contentType);

    /**
     * 压缩文件内容。
     *
     * @param command 压缩命令。
     * @return 压缩结果。
     */
    CompressFileResultVO compress(CompressFileCommand command);
}
