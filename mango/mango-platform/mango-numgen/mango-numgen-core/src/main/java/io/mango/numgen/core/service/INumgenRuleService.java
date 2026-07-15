package io.mango.numgen.core.service;

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

    PageResult<NumgenRuleVO> pageRules(NumgenRulePageQuery query);

    NumgenRuleVO detailRule(Long id);

    Long createRule(SaveNumgenRuleCommand command);

    Boolean updateRule(SaveNumgenRuleCommand command);

    Boolean updateRuleStatus(UpdateNumgenRuleStatusCommand command);

    Boolean deleteRule(Long id);

    Boolean publishRule(NumgenPublishCommand command);

    NumgenPreviewVO previewRule(NumgenPreviewCommand command);
}
