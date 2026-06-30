package io.mango.system.api.vo;

import lombok.Data;

@Data
public class AdminBrandingVO {

    private Boolean enabled;

    private String title;

    private String shortTitle;

    private String subtitle;

    private String loginTitle;

    private String loginSubtitle;

    private String logoFile;

    private String faviconFile;

    private String loginImageFile;

    private String footerCopyright;

    private String icp;

    private String contact;
}
