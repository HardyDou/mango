package io.mango.notice.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import io.mango.common.contract.BinaryTransferContract;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.IOException;
import java.io.InputStream;

/** Attachment stream transferred by an inbound adapter. */
@Getter
@AllArgsConstructor
@BinaryTransferContract
public class InboundNoticeAttachmentRequest implements AutoCloseable {

    @Schema(description = "附件序号")
    private final int index;
    @Schema(description = "文件名称")
    private final String fileName;
    @Schema(description = "文件类型")
    private final String contentType;
    @Schema(description = "文件大小")
    private final long fileSize;
    @Schema(description = "文件内容流")
    private final InputStream content;

    public int index() { return index; }
    public String fileName() { return fileName; }
    public String contentType() { return contentType; }
    public long fileSize() { return fileSize; }
    public InputStream content() { return content; }

    @Override
    public void close() throws IOException {
        if (content != null) {
            content.close();
        }
    }
}
