package io.mango.auth.core.service;

import io.mango.auth.api.command.BindExistingAccountCommand;
import io.mango.auth.api.command.CompleteProviderAuthorizationCommand;
import io.mango.auth.api.command.StartProviderAuthorizationCommand;
import io.mango.auth.api.vo.LoginVO;
import io.mango.auth.api.vo.ProviderAuthorizationResultVO;
import io.mango.auth.api.vo.ProviderAuthorizationVO;

public interface IExternalAuthorizationService {

    ProviderAuthorizationVO start(StartProviderAuthorizationCommand command);

    ProviderAuthorizationResultVO complete(CompleteProviderAuthorizationCommand command);

    LoginVO bindExisting(BindExistingAccountCommand command);

    Boolean refreshCurrentWecomProfile();
}
