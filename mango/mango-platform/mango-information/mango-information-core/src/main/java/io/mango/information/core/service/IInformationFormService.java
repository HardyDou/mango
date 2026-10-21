package io.mango.information.core.service;

import io.mango.common.vo.PageResult;
import io.mango.information.api.command.SaveInformationFormCommand;
import io.mango.information.api.command.UpdateInformationFormStatusCommand;
import io.mango.information.api.query.InformationFormPageQuery;
import io.mango.information.api.vo.InformationFormDetailVO;
import io.mango.information.api.vo.InformationFormVO;
import io.mango.information.core.entity.InformationFormEntity;

public interface IInformationFormService {
    PageResult<InformationFormVO> page(InformationFormPageQuery query);
    InformationFormDetailVO detail(Long id);
    InformationFormDetailVO publicDetail(String formCode);
    /** 取已发布表单实体，供公开入口按表单归属处理提交与上传。 */
    InformationFormEntity selectPublished(String formCode);
    Long save(SaveInformationFormCommand command);
    boolean updateStatus(UpdateInformationFormStatusCommand command);
    String regenerateSecret(Long id);
    boolean delete(Long id);
}
