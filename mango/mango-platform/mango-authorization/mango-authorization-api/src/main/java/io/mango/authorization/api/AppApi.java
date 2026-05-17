package io.mango.authorization.api;

import io.mango.authorization.api.command.AppCommand;
import io.mango.authorization.api.vo.AppRuntimeDescriptorVO;
import io.mango.authorization.api.vo.AppVO;
import io.mango.common.result.R;

import java.util.List;

/**
 * 授权应用 API。
 * <p>
 * 授权应用表达用户入口和授权边界；前端加载方式由运行配置补充。
 */
public interface AppApi {

    R<List<AppVO>> list();

    R<AppVO> get(Long appId);

    R<List<AppVO>> runtime();

    R<AppRuntimeDescriptorVO> runtimeDescriptor(String appCode);

    R<AppVO> runtimeDetail(String appCode);

    R<Long> create(AppCommand command);

    R<Boolean> update(AppCommand command);

    R<Boolean> delete(Long appId);
}
