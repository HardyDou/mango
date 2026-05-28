package io.mango.system.core.service;

import io.mango.system.api.command.SavePersonalConfigCommand;
import io.mango.system.api.query.PersonalConfigQuery;
import io.mango.system.api.vo.PersonalConfigVO;

import java.util.List;

public interface IPersonalConfigService {

    List<PersonalConfigVO> listCurrentUser(PersonalConfigQuery query);

    PersonalConfigVO getCurrentUserValue(PersonalConfigQuery query);

    PersonalConfigVO saveCurrentUser(SavePersonalConfigCommand command);

    boolean deleteCurrentUser(PersonalConfigQuery query);
}
