package io.mango.numgen.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.command.SaveNumgenGeneratorCommand;
import io.mango.numgen.api.command.UpdateNumgenGeneratorStatusCommand;
import io.mango.numgen.api.query.NumgenGeneratorPageQuery;
import io.mango.numgen.api.vo.NumgenGeneratorVO;

public interface INumgenGeneratorService {

    R<PageResult<NumgenGeneratorVO>> pageGenerators(NumgenGeneratorPageQuery query);

    R<NumgenGeneratorVO> detailGenerator(Long id);

    R<Long> createGenerator(SaveNumgenGeneratorCommand command);

    R<Boolean> updateGenerator(SaveNumgenGeneratorCommand command);

    R<Boolean> updateGeneratorStatus(UpdateNumgenGeneratorStatusCommand command);

    R<Boolean> deleteGenerator(Long id);
}
