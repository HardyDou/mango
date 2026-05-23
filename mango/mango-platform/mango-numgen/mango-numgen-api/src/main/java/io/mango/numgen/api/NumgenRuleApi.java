package io.mango.numgen.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.command.NumgenPreviewCommand;
import io.mango.numgen.api.command.NumgenPublishCommand;
import io.mango.numgen.api.command.SaveNumgenRuleCommand;
import io.mango.numgen.api.command.UpdateNumgenRuleStatusCommand;
import io.mango.numgen.api.query.NumgenRulePageQuery;
import io.mango.numgen.api.vo.NumgenRuleVO;
import io.mango.numgen.api.vo.NumgenPreviewVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * 编号规则 API 契约。
 */
@Validated
public interface NumgenRuleApi {

    R<PageResult<NumgenRuleVO>> pageRules(@Valid NumgenRulePageQuery query);

    R<NumgenRuleVO> detailRule(@NotNull(message = "编号规则 ID 不能为空") Long id);

    R<Long> createRule(@Valid SaveNumgenRuleCommand command);

    R<Boolean> updateRule(@Valid SaveNumgenRuleCommand command);

    R<Boolean> updateRuleStatus(@Valid UpdateNumgenRuleStatusCommand command);

    R<Boolean> deleteRule(@NotNull(message = "编号规则 ID 不能为空") Long id);

    R<Boolean> publishRule(@Valid NumgenPublishCommand command);

    R<NumgenPreviewVO> previewRule(@Valid NumgenPreviewCommand command);
}
