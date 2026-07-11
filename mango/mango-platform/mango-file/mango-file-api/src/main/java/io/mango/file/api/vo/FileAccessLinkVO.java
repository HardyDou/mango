package io.mango.file.api.vo;

import io.mango.file.api.enums.FileAccessAction;
import lombok.Data;

@Data
public class FileAccessLinkVO {
    private String url;
    private FileAccessAction action;
    private Long expireSeconds;
}
