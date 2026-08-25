package io.mango.ai.starter.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.ai.api.AiConfigurationApi;
import io.mango.ai.api.command.CreateAiPromptCommand;
import io.mango.ai.api.command.CreateAiServiceCommand;
import io.mango.ai.api.command.CreateAiSkillCommand;
import io.mango.ai.api.command.CreateAiToolCommand;
import io.mango.ai.api.command.UpdateAiPromptCommand;
import io.mango.ai.api.command.UpdateAiServiceCommand;
import io.mango.ai.api.command.UpdateAiSkillCommand;
import io.mango.ai.api.command.UpdateAiToolCommand;
import io.mango.ai.api.vo.AiPromptVO;
import io.mango.ai.api.vo.AiServiceVO;
import io.mango.ai.api.vo.AiSkillVO;
import io.mango.ai.api.vo.AiToolVO;
import io.mango.ai.core.service.IAiConfigurationService;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** AI Prompt、Skill、工具和服务配置 HTTP 适配器。 */
@RestController
@Validated
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring injects the application service; copying a container-managed collaborator is not valid"))
@RequestMapping("/ai")
@Tag(name = "AI 配置管理", description = "管理 Prompt、Skill、工具和 AI 服务")
public class AiConfigurationController implements AiConfigurationApi {
    private final IAiConfigurationService service;

    @Override
    @GetMapping("/prompts")
    @Operation(summary = "查询 Prompt", description = "查询当前租户的 Prompt 配置")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:prompt:list")
    public R<List<AiPromptVO>> prompts() {
        return R.ok(service.prompts());
    }

    @Override
    @PostMapping("/prompts")
    @Operation(summary = "新增 Prompt", description = "新增 Prompt 配置")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:prompt:add")
    public R<Long> createPrompt(@RequestBody CreateAiPromptCommand command) {
        return R.ok(service.createPrompt(command));
    }

    @Override
    @PutMapping("/prompts")
    @Operation(summary = "修改 Prompt", description = "修改 Prompt 配置并保留发布状态语义")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:prompt:edit")
    public R<Boolean> updatePrompt(@RequestBody UpdateAiPromptCommand command) {
        return R.ok(service.updatePrompt(command));
    }

    @Override
    @DeleteMapping("/prompts")
    @Operation(summary = "删除 Prompt", description = "删除未被 AI 服务引用的 Prompt")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:prompt:delete")
    public R<Boolean> deletePrompt(@Parameter(description = "Prompt 标识") @RequestParam("id") Long id) {
        return R.ok(service.deletePrompt(id));
    }

    @Override
    @PutMapping("/prompts/publish")
    @Operation(summary = "发布 Prompt", description = "发布指定 Prompt 版本供 AI 服务运行")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:prompt:publish")
    public R<Boolean> publishPrompt(@Parameter(description = "Prompt 标识") @RequestParam("id") Long id) {
        return R.ok(service.publishPrompt(id));
    }

    @Override
    @GetMapping("/skills")
    @Operation(summary = "查询 Skill", description = "查询当前租户的 Skill 配置")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:skill:list")
    public R<List<AiSkillVO>> skills() {
        return R.ok(service.skills());
    }

    @Override
    @PostMapping("/skills")
    @Operation(summary = "新增 Skill", description = "新增 Skill 指令和工具引用")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:skill:add")
    public R<Long> createSkill(@RequestBody CreateAiSkillCommand command) {
        return R.ok(service.createSkill(command));
    }

    @Override
    @PutMapping("/skills")
    @Operation(summary = "修改 Skill", description = "修改 Skill 指令和工具引用")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:skill:edit")
    public R<Boolean> updateSkill(@RequestBody UpdateAiSkillCommand command) {
        return R.ok(service.updateSkill(command));
    }

    @Override
    @DeleteMapping("/skills")
    @Operation(summary = "删除 Skill", description = "删除未被 AI 服务引用的 Skill")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:skill:delete")
    public R<Boolean> deleteSkill(@Parameter(description = "Skill 标识") @RequestParam("id") Long id) {
        return R.ok(service.deleteSkill(id));
    }

    @Override
    @GetMapping("/tools")
    @Operation(summary = "查询工具", description = "查询当前租户的 MCP 和 HTTP 工具配置")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:tool:list")
    public R<List<AiToolVO>> tools() {
        return R.ok(service.tools());
    }

    @Override
    @PostMapping("/tools")
    @Operation(summary = "新增工具", description = "新增 MCP 或 HTTP 工具配置")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:tool:add")
    public R<Long> createTool(@RequestBody CreateAiToolCommand command) {
        return R.ok(service.createTool(command));
    }

    @Override
    @PutMapping("/tools")
    @Operation(summary = "修改工具", description = "修改 MCP 或 HTTP 工具配置")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:tool:edit")
    public R<Boolean> updateTool(@RequestBody UpdateAiToolCommand command) {
        return R.ok(service.updateTool(command));
    }

    @Override
    @DeleteMapping("/tools")
    @Operation(summary = "删除工具", description = "删除未被 Skill 引用的工具")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:tool:delete")
    public R<Boolean> deleteTool(@Parameter(description = "工具标识") @RequestParam("id") Long id) {
        return R.ok(service.deleteTool(id));
    }

    @Override
    @GetMapping("/services")
    @Operation(summary = "查询 AI 服务", description = "查询可进入统一会话工作台的 AI 服务")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:service:list")
    public R<List<AiServiceVO>> services() {
        return R.ok(service.services());
    }

    @Override
    @PostMapping("/services")
    @Operation(summary = "新增 AI 服务", description = "新增 AI 服务并关联 Prompt、Skill 和 Schema")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:service:add")
    public R<Long> createService(@RequestBody CreateAiServiceCommand command) {
        return R.ok(service.createService(command));
    }

    @Override
    @PutMapping("/services")
    @Operation(summary = "修改 AI 服务", description = "修改 AI 服务定义和启用状态")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:service:edit")
    public R<Boolean> updateService(@RequestBody UpdateAiServiceCommand command) {
        return R.ok(service.updateService(command));
    }

    @Override
    @DeleteMapping("/services")
    @Operation(summary = "删除 AI 服务", description = "删除指定 AI 服务定义")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:service:delete")
    public R<Boolean> deleteService(@Parameter(description = "AI 服务标识") @RequestParam("id") Long id) {
        return R.ok(service.deleteService(id));
    }
}
