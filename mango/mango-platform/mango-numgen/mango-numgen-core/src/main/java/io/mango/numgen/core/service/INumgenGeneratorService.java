package io.mango.numgen.core.service;

import io.mango.common.vo.PageResult;
import io.mango.numgen.api.command.SaveNumgenGeneratorCommand;
import io.mango.numgen.api.command.UpdateNumgenGeneratorStatusCommand;
import io.mango.numgen.api.query.NumgenGeneratorPageQuery;
import io.mango.numgen.api.vo.NumgenGeneratorVO;

public interface INumgenGeneratorService {

    PageResult<NumgenGeneratorVO> pageGenerators(NumgenGeneratorPageQuery query);

    NumgenGeneratorVO detailGenerator(Long id);

    Long createGenerator(SaveNumgenGeneratorCommand command);

    Boolean updateGenerator(SaveNumgenGeneratorCommand command);

    Boolean updateGeneratorStatus(UpdateNumgenGeneratorStatusCommand command);

    Boolean deleteGenerator(Long id);
}
