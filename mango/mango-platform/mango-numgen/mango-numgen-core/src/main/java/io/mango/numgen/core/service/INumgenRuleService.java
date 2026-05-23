package io.mango.numgen.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.command.NumgenPreviewCommand;
import io.mango.numgen.api.command.NumgenPublishCommand;
import io.mango.numgen.api.command.SaveNumgenRuleCommand;
import io.mango.numgen.api.command.UpdateNumgenRuleStatusCommand;
import io.mango.numgen.api.query.NumgenRulePageQuery;
import io.mango.numgen.api.vo.NumgenPreviewVO;
import io.mango.numgen.api.vo.NumgenRuleVO;

/**
 * 编号规则服务。
 */
public interface INumgenRuleService {

    R<PageResult<NumgenRuleVO>> pageRules(NumgenRulePageQuery query);

    R<NumgenRuleVO> detailRule(Long id);

    R<Long> createRule(SaveNumgenRuleCommand command);

    R<Boolean> updateRule(SaveNumgenRuleCommand command);

    R<Boolean> updateRuleStatus(UpdateNumgenRuleStatusCommand command);

    R<Boolean> deleteRule(Long id);

    R<Boolean> publishRule(NumgenPublishCommand command);

    R<NumgenPreviewVO> previewRule(NumgenPreviewCommand command);
}
