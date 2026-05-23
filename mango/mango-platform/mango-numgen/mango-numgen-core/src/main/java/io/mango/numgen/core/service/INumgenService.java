package io.mango.numgen.core.service;

import io.mango.numgen.api.command.NumgenBatchCommand;
import io.mango.numgen.api.command.NumgenNextCommand;
import io.mango.numgen.api.command.NumgenValidateRuleCommand;
import io.mango.numgen.api.vo.NumgenRuleValidationVO;

import java.util.List;

public interface INumgenService {

    String nextValue(NumgenNextCommand command);

    List<String> batchValue(NumgenBatchCommand command);

    NumgenRuleValidationVO validateRule(NumgenValidateRuleCommand command);
}
