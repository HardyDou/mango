package io.mango.file.preview.support.remote;

import java.io.File;
import java.io.IOException;

/**
 * 远程取源传输：把引擎侧「按票据取源文件」的需求交给 Mango 主应用执行。
 *
 * <p>独立部署的预览引擎进程里没有 Mango 的文件服务，只能通过本接口把票据发回 Mango，
 * 由 Mango 侧校验票据并回传源文件字节流。实现方必须流式转发，不得把整个文件读入内存。</p>
 */
public interface FilePreviewSourceTransport {

    /**
     * 把票据对应的源文件流式写入目标文件。
     *
     * <p>目标文件的父目录由调用方保证存在；实现方负责创建或覆盖目标文件本身，
     * 并且必须边读边写，不能把整个文件缓冲到内存。票据有效性由 Mango 侧取流接口自行校验，
     * 实现方不再额外做一次远程有效性探测。</p>
     *
     * @param token  取源票据
     * @param target 目标文件
     * @throws IOException 网络异常或写入失败
     */
    void downloadSource(String token, File target) throws IOException;
}
