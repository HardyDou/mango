package io.mango.information.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.information.api.command.SaveInformationFormCommand;
import io.mango.information.api.query.InformationFormPageQuery;
import io.mango.information.api.vo.InformationFormDetailVO;
import io.mango.information.api.vo.InformationFormVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** 信息采集表单管理 API。 */
public interface InformationFormApi {
    R<PageResult<InformationFormVO>> page(@Valid InformationFormPageQuery query);
    R<InformationFormDetailVO> detail(@NotNull Long id);
    R<Long> save(@Valid SaveInformationFormCommand command);
    R<Boolean> delete(@NotNull Long id);
}
