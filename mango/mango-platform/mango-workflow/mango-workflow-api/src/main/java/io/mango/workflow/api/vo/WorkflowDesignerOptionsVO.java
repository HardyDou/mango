package io.mango.workflow.api.vo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程设计器候选数据集合。
 */
@Getter
@Setter
@Schema(description = "流程设计器候选数据集合")
public class WorkflowDesignerOptionsVO {

    @Schema(description = "用户候选项")
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Jackson and Feign require this mutable collection getter"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Jackson and Feign require this mutable collection setter"))
    private List<WorkflowDesignerOptionVO> users = new ArrayList<>();

    @Schema(description = "角色候选项")
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Jackson and Feign require this mutable collection getter"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Jackson and Feign require this mutable collection setter"))
    private List<WorkflowDesignerOptionVO> roles = new ArrayList<>();

    @Schema(description = "岗位候选项")
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Jackson and Feign require this mutable collection getter"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Jackson and Feign require this mutable collection setter"))
    private List<WorkflowDesignerOptionVO> posts = new ArrayList<>();

    @Schema(description = "组织树候选项")
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Jackson and Feign require this mutable collection getter"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Jackson and Feign require this mutable collection setter"))
    private List<WorkflowDesignerOptionVO> organizations = new ArrayList<>();

    @Schema(description = "字典类型候选项")
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Jackson and Feign require this mutable collection getter"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Jackson and Feign require this mutable collection setter"))
    private List<WorkflowDesignerOptionVO> dictTypes = new ArrayList<>();
}
