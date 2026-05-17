package io.mango.authorization.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 当前主体可访问的前端运行描述。
 */
@Data
public class AppRuntimeDescriptorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前生效的部署配置档。 */
    private String deployProfile;

    /** 当前主体可访问的前端运行单元。 */
    private List<AppVO> apps = new ArrayList<>();

    /** 当前配置档下的模块运行策略。 */
    private List<FrontendModuleRuntimeStrategyVO> moduleStrategies = new ArrayList<>();
}
