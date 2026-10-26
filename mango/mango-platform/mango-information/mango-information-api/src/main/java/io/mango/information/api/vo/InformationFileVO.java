package io.mango.information.api.vo;

import lombok.Data;

/** 信息采集公开上传返回的文件信息，字段与平台文件记录保持一致，便于前端直接复用。 */
@Data
public class InformationFileVO {
    private Long id;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private String url;
    private String previewUrl;
    private String downloadUrl;
}
