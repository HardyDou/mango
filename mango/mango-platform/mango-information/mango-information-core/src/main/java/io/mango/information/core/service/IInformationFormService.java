package io.mango.information.core.service;

import io.mango.common.vo.PageResult;
import io.mango.information.api.command.SaveInformationFormCommand;
import io.mango.information.api.command.UpdateInformationFormStatusCommand;
import io.mango.information.api.query.InformationFormPageQuery;
import io.mango.information.api.vo.InformationFormDetailVO;
import io.mango.information.api.vo.InformationFormVO;

public interface IInformationFormService {
    PageResult<InformationFormVO> page(InformationFormPageQuery query);
    InformationFormDetailVO detail(Long id);
    InformationFormDetailVO publicDetail(String formCode);
    Long save(SaveInformationFormCommand command);
    boolean updateStatus(UpdateInformationFormStatusCommand command);
    String regenerateSecret(Long id);
    boolean delete(Long id);
}
