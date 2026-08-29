package io.mango.workflow.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.WorkflowDefinitionApi;
import io.mango.workflow.api.command.EnsureWorkflowDefinitionCommand;
import io.mango.workflow.api.command.SaveWorkflowDefinitionCommand;
import io.mango.workflow.api.command.UpdateWorkflowDefinitionStatusCommand;
import io.mango.workflow.api.query.WorkflowDefinitionPageQuery;
import io.mango.workflow.api.query.WorkflowDefinitionVersionQuery;
import io.mango.workflow.api.vo.WorkflowDefinitionVO;
import io.mango.workflow.api.vo.WorkflowDefinitionVersionVO;
import io.mango.workflow.api.vo.WorkflowDeployVO;
import io.mango.workflow.api.vo.WorkflowDesignerOptionsVO;
import io.mango.workflow.api.vo.WorkflowNodeCatalogVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 流程定义远程客户端。
 */
@FeignClient(name = "mango-workflow", contextId = "workflowDefinitionFeignClient", path = "/workflow/definitions")
public interface WorkflowDefinitionFeignClient extends WorkflowDefinitionApi {

    @Override
    @GetMapping("/page")
    R<PageResult<WorkflowDefinitionVO>> page(@SpringQueryMap WorkflowDefinitionPageQuery query);

    @Override
    @GetMapping("/detail")
    R<WorkflowDefinitionVO> get(@RequestParam("id") Long id);

    @Override
    @PostMapping
    R<String> create(@RequestBody SaveWorkflowDefinitionCommand command);

    @Override
    @PutMapping
    R<Boolean> update(@RequestBody SaveWorkflowDefinitionCommand command);

    @Override
    @DeleteMapping
    R<Boolean> delete(@RequestParam("id") Long id);

    @Override
    @PutMapping("/status")
    R<Boolean> updateStatus(@RequestBody UpdateWorkflowDefinitionStatusCommand command);

    @Override
    @PostMapping("/discard-draft")
    R<Boolean> discardDraft(@RequestParam("id") Long id);

    @Override
    @PostMapping("/deploy")
    R<WorkflowDeployVO> deploy(@RequestParam("id") Long id);

    @Override
    @PostMapping("/internal/ensure-published")
    R<WorkflowDeployVO> ensurePublished(@RequestBody EnsureWorkflowDefinitionCommand command);

    @Override
    @GetMapping("/versions")
    R<List<WorkflowDefinitionVersionVO>> versions(@SpringQueryMap WorkflowDefinitionVersionQuery query);

    @Override
    @GetMapping("/version-detail")
    R<WorkflowDefinitionVersionVO> versionDetail(@RequestParam("id") Long id);

    @Override
    @GetMapping("/node-catalog")
    R<List<WorkflowNodeCatalogVO>> nodeCatalog();

    @Override
    @GetMapping("/designer-options")
    R<WorkflowDesignerOptionsVO> designerOptions();
}
