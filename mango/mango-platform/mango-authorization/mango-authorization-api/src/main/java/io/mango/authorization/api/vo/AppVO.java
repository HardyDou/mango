package io.mango.authorization.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 授权应用入口 VO。
 */
@Data
public class AppVO implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long appId;
    private String appCode;
    private String appName;
    private String realm;
    private String actorType;
    private String icon;
    private Integer sort;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
