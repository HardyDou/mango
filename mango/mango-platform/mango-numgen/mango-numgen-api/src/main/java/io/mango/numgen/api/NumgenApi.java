package io.mango.numgen.api;

import io.mango.common.result.R;
import io.mango.numgen.api.command.NumgenBatchCommand;
import io.mango.numgen.api.command.NumgenNextCommand;
import io.mango.numgen.api.command.NumgenValidateRuleCommand;
import io.mango.numgen.api.vo.NumgenRuleValidationVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 编号生成 API 契约。
 */
@Validated
public interface NumgenApi {

    R<String> nextValue(@Valid NumgenNextCommand command);

    R<List<String>> batchValue(@Valid NumgenBatchCommand command);

    R<NumgenRuleValidationVO> validateRule(@Valid NumgenValidateRuleCommand command);
}
