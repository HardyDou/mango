package io.mango.cms.api.vo;

import lombok.Data;

@Data
public class SiteBannerVO {
    private Long id;
    private String position;
    private String title;
    private String subtitle;
    private String mediaType;
    private String mediaFileId;
    private String jumpUrl;
    private Integer sort;
}
