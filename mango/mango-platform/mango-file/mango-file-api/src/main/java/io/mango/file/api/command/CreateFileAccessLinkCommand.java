package io.mango.file.api.command;

import io.mango.file.api.enums.FileAccessAction;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateFileAccessLinkCommand {
    @NotNull(message = "文件ID不能为空")
    private Long fileId;
    @NotNull(message = "访问动作不能为空")
    private FileAccessAction action;
}
