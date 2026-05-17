package io.mango.authorization.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 逻辑应用集成模块 VO。
 */
@Data
public class AppModuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long bindingId;
    private String appCode;
    private String moduleCode;
    private String moduleName;
    private Integer status;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
