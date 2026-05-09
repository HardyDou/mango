package io.mango.authorization.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用登录上下文 VO。
 */
@Data
public class AppLoginContextVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long contextId;
    private Long appId;
    private String appCode;
    private String realm;
    private String actorType;
    private Integer defaultFlag;
    private Integer status;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
