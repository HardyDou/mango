package io.mango.authorization.api;

import io.mango.authorization.api.command.AppCommand;
import io.mango.authorization.api.vo.AppVO;
import io.mango.common.result.R;

import java.util.List;

/**
 * 授权应用入口 API。
 */
public interface AppApi {

    R<List<AppVO>> list();

    R<AppVO> get(Long appId);

    R<Long> create(AppCommand command);

    R<Boolean> update(AppCommand command);

    R<Boolean> delete(Long appId);
}
