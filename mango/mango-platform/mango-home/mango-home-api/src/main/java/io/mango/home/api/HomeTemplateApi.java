package io.mango.home.api;

import io.mango.common.result.R;
import io.mango.home.api.command.CreateHomeTemplateCommand;
import io.mango.home.api.command.HomeTemplateIdCommand;
import io.mango.home.api.command.SaveHomeTemplateAuthorizationsCommand;
import io.mango.home.api.command.UpdateHomeTemplateDraftCommand;
import io.mango.home.api.command.UpdateHomeTemplateStatusCommand;
import io.mango.home.api.query.HomeTemplateAuthorizationQuery;
import io.mango.home.api.query.HomeTemplateQuery;
import io.mango.home.api.query.UserHomeViewQuery;
import io.mango.home.api.vo.HomePageVO;
import io.mango.home.api.vo.HomeTemplateAuthorizationVO;
import io.mango.home.api.vo.HomeTemplateVO;
import jakarta.validation.Valid;

import java.util.List;

public interface HomeTemplateApi {

    R<List<HomeTemplateVO>> list(@Valid HomeTemplateQuery query);

    R<HomeTemplateVO> detail(Long id);

    R<HomeTemplateVO> create(@Valid CreateHomeTemplateCommand command);

    R<HomeTemplateVO> updateDraft(@Valid UpdateHomeTemplateDraftCommand command);

    R<HomeTemplateVO> copy(@Valid HomeTemplateIdCommand command);

    R<HomeTemplateVO> publish(@Valid HomeTemplateIdCommand command);

    R<HomeTemplateVO> updateStatus(@Valid UpdateHomeTemplateStatusCommand command);

    R<Void> delete(@Valid HomeTemplateIdCommand command);

    R<List<HomeTemplateAuthorizationVO>> listAuthorizations(@Valid HomeTemplateAuthorizationQuery query);

    R<List<HomeTemplateAuthorizationVO>> saveAuthorizations(@Valid SaveHomeTemplateAuthorizationsCommand command);

    R<List<HomePageVO>> resolveUserPages(@Valid UserHomeViewQuery query);
}
