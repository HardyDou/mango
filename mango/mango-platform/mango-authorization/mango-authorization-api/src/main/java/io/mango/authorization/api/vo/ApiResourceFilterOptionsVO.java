package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * API 资源筛选维度。
 */
@Getter
@Setter
@Schema(description = "API 资源筛选维度")
public class ApiResourceFilterOptionsVO {

    @Schema(description = "已登记资源的来源模块名")
    private List<String> modules;

    @Schema(description = "已登记资源的接口领域")
    private List<String> domains;

    public List<String> getModules() {
        return new ArrayList<>(modules);
    }

    public void setModules(List<String> modules) {
        this.modules = modules == null ? new ArrayList<>() : new ArrayList<>(modules);
    }

    public List<String> getDomains() {
        return new ArrayList<>(domains);
    }

    public void setDomains(List<String> domains) {
        this.domains = domains == null ? new ArrayList<>() : new ArrayList<>(domains);
    }
}
