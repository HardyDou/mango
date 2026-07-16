package io.mango.area.api;

import io.mango.area.api.command.SaveAreaCommand;
import io.mango.area.api.vo.SysAreaTreeNodeVO;
import io.mango.area.api.vo.SysAreaVO;
import io.mango.common.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface SysAreaApi {

    R<List<SysAreaTreeNodeVO>> tree(@Max(Integer.MAX_VALUE) Integer type);

    R<List<SysAreaVO>> listByPid(@NotNull Long parentId);

    R<SysAreaVO> getById(@NotNull Long id);

    R<SysAreaVO> getByAdcode(@NotNull Long adcode);

    R<Void> create(@Valid SaveAreaCommand command);

    R<Void> update(@Valid SaveAreaCommand command);

    R<Void> delete(@NotNull Long id);

    R<List<SysAreaVO>> listActive();
}
