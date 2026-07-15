package io.mango.authorization.api;

import io.mango.authorization.api.command.AppCommand;
import io.mango.authorization.api.vo.AppRuntimeDescriptorVO;
import io.mango.authorization.api.vo.AppVO;
import io.mango.common.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 授权应用 API。
 * <p>
 * 授权应用表达用户入口和授权边界；前端加载方式由运行配置补充。
 */
public interface AppApi {

    R<List<AppVO>> list();

    R<AppVO> get(@Positive Long appId);

    R<List<AppVO>> runtime();

    R<AppRuntimeDescriptorVO> runtimeDescriptor(@NotBlank String appCode);

    R<AppVO> runtimeDetail(@NotBlank String appCode);

    R<Long> create(@Valid AppCommand command);

    R<Boolean> update(@Valid AppCommand command);

    R<Boolean> delete(@Positive Long appId);
}
