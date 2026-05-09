package io.mango.authorization.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 授权应用入口 VO。
 */
@Data
public class AppVO implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long appId;
    private String appCode;
    private String appName;
    private List<AppLoginContextVO> loginContexts = new ArrayList<>();
    private String icon;
    private Integer sort;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
