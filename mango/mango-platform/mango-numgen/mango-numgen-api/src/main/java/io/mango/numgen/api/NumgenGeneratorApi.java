package io.mango.numgen.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.command.SaveNumgenGeneratorCommand;
import io.mango.numgen.api.command.UpdateNumgenGeneratorStatusCommand;
import io.mango.numgen.api.query.NumgenGeneratorPageQuery;
import io.mango.numgen.api.vo.NumgenGeneratorVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface NumgenGeneratorApi {

    R<PageResult<NumgenGeneratorVO>> pageGenerators(@Valid NumgenGeneratorPageQuery query);

    R<NumgenGeneratorVO> detailGenerator(@NotNull(message = "编号生成器 ID 不能为空") Long id);

    R<Long> createGenerator(@Valid SaveNumgenGeneratorCommand command);

    R<Boolean> updateGenerator(@Valid SaveNumgenGeneratorCommand command);

    R<Boolean> updateGeneratorStatus(@Valid UpdateNumgenGeneratorStatusCommand command);

    R<Boolean> deleteGenerator(@NotNull(message = "编号生成器 ID 不能为空") Long id);
}
