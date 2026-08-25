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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class AiConfigurationController implements AiConfigurationApi {
    private final IAiConfigurationService service;

    @Override
    @GetMapping("/prompts")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:prompt:list")
    public R<List<AiPromptVO>> prompts() {
        return R.ok(service.prompts());
    }

    @Override
    @PostMapping("/prompts")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:prompt:add")
    public R<Long> createPrompt(@Valid @RequestBody CreateAiPromptCommand command) {
        return R.ok(service.createPrompt(command));
    }

    @Override
    @PutMapping("/prompts")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:prompt:edit")
    public R<Boolean> updatePrompt(@Valid @RequestBody UpdateAiPromptCommand command) {
        return R.ok(service.updatePrompt(command));
    }

    @Override
    @DeleteMapping("/prompts")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:prompt:delete")
    public R<Boolean> deletePrompt(@NotNull @Positive @RequestParam Long id) {
        return R.ok(service.deletePrompt(id));
    }

    @Override
    @PutMapping("/prompts/publish")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:prompt:publish")
    public R<Boolean> publishPrompt(@NotNull @Positive @RequestParam Long id) {
        return R.ok(service.publishPrompt(id));
    }

    @Override
    @GetMapping("/skills")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:skill:list")
    public R<List<AiSkillVO>> skills() {
        return R.ok(service.skills());
    }

    @Override
    @PostMapping("/skills")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:skill:add")
    public R<Long> createSkill(@Valid @RequestBody CreateAiSkillCommand command) {
        return R.ok(service.createSkill(command));
    }

    @Override
    @PutMapping("/skills")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:skill:edit")
    public R<Boolean> updateSkill(@Valid @RequestBody UpdateAiSkillCommand command) {
        return R.ok(service.updateSkill(command));
    }

    @Override
    @DeleteMapping("/skills")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:skill:delete")
    public R<Boolean> deleteSkill(@NotNull @Positive @RequestParam Long id) {
        return R.ok(service.deleteSkill(id));
    }

    @Override
    @GetMapping("/tools")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:tool:list")
    public R<List<AiToolVO>> tools() {
        return R.ok(service.tools());
    }

    @Override
    @PostMapping("/tools")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:tool:add")
    public R<Long> createTool(@Valid @RequestBody CreateAiToolCommand command) {
        return R.ok(service.createTool(command));
    }

    @Override
    @PutMapping("/tools")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:tool:edit")
    public R<Boolean> updateTool(@Valid @RequestBody UpdateAiToolCommand command) {
        return R.ok(service.updateTool(command));
    }

    @Override
    @DeleteMapping("/tools")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:tool:delete")
    public R<Boolean> deleteTool(@NotNull @Positive @RequestParam Long id) {
        return R.ok(service.deleteTool(id));
    }

    @Override
    @GetMapping("/services")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:service:list")
    public R<List<AiServiceVO>> services() {
        return R.ok(service.services());
    }

    @Override
    @PostMapping("/services")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:service:add")
    public R<Long> createService(@Valid @RequestBody CreateAiServiceCommand command) {
        return R.ok(service.createService(command));
    }

    @Override
    @PutMapping("/services")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:service:edit")
    public R<Boolean> updateService(@Valid @RequestBody UpdateAiServiceCommand command) {
        return R.ok(service.updateService(command));
    }

    @Override
    @DeleteMapping("/services")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:service:delete")
    public R<Boolean> deleteService(@NotNull @Positive @RequestParam Long id) {
        return R.ok(service.deleteService(id));
    }
}
