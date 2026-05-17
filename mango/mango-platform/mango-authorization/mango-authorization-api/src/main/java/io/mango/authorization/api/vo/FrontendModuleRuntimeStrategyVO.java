package io.mango.authorization.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 前端模块运行策略 VO。
 */
@Data
public class FrontendModuleRuntimeStrategyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long strategyId;
    private String appCode;
    private String moduleCode;
    private String deployProfile;
    private String pageType;
    private String runtimeCode;
    private Integer status;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
