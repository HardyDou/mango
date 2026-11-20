package cn.keking.spi;

import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;

/**
 * 可扩展的文件下载器。
 *
 * <p>引擎默认的下载实现（HTTP / file 协议等）保持不变；需要接入自有存储时，注册一个
 * {@code supports} 返回 true 的实现，由它负责把源文件落到 {@code fileAttribute} 指定的路径。</p>
 */
public interface FileDownloader {

    /**
     * 是否能处理该文件属性。
     *
     * @param fileAttribute 文件属性
     * @return true 表示由本实现下载
     */
    boolean supports(FileAttribute fileAttribute);

    /**
     * 下载文件到 {@link FileAttribute#getOutFilePath()} 约定的路径。
     *
     * @param fileAttribute 文件属性
     * @param fileName      引擎侧文件名
     * @return 与引擎内置下载一致的返回结构：{@code code=0} 且 {@code content=落盘路径}
     */
    ReturnResponse<String> download(FileAttribute fileAttribute, String fileName);

}
