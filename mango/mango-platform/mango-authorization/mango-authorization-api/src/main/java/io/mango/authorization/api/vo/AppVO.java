package io.mango.authorization.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 授权应用 VO。
 * <p>
 * 基础字段来自 authorization_app；前端运行配置字段来自 authorization_frontend_app_registry。
 */
@Data
public class AppVO implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long appId;
    private String appCode;
    private String appName;
    private String appType;
    private String deployMode;
    private String entryUrl;
    private String mountPath;
    private String activeRule;
    private String framework;
    private String version;
    private String healthCheckUrl;
    private Boolean sandboxEnabled;
    private String styleIsolation;
    private List<AppLoginContextVO> loginContexts = new ArrayList<>();
    private String icon;
    private Integer sort;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
