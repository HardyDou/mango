package io.mango.home.core.service;

import io.mango.home.api.command.CreateHomeTemplateCommand;
import io.mango.home.api.command.SaveHomeTemplateAuthorizationsCommand;
import io.mango.home.api.command.UpdateHomeTemplateDraftCommand;
import io.mango.home.api.command.UpdateHomeTemplateStatusCommand;
import io.mango.home.api.query.HomeTemplateAuthorizationQuery;
import io.mango.home.api.query.HomeTemplateQuery;
import io.mango.home.api.query.UserHomeViewQuery;
import io.mango.home.api.vo.HomePageVO;
import io.mango.home.api.vo.HomeTemplateAuthorizationVO;
import io.mango.home.api.vo.HomeTemplateVO;

import java.util.List;

public interface IHomeTemplateService {

    List<HomeTemplateVO> list(HomeTemplateQuery query);

    HomeTemplateVO detail(Long id);

    HomeTemplateVO create(CreateHomeTemplateCommand command);

    HomeTemplateVO updateDraft(UpdateHomeTemplateDraftCommand command);

    HomeTemplateVO copy(Long id);

    HomeTemplateVO publish(Long id);

    HomeTemplateVO updateStatus(UpdateHomeTemplateStatusCommand command);

    void delete(Long id);

    List<HomeTemplateAuthorizationVO> listAuthorizations(HomeTemplateAuthorizationQuery query);

    List<HomeTemplateAuthorizationVO> saveAuthorizations(SaveHomeTemplateAuthorizationsCommand command);

    List<HomePageVO> resolveUserPages(UserHomeViewQuery query);
}
