package io.mango.numgen.starter.remote;

import io.mango.common.result.R;
import io.mango.numgen.api.NumgenApi;
import io.mango.numgen.api.command.NumgenBatchCommand;
import io.mango.numgen.api.command.NumgenNextCommand;
import io.mango.numgen.api.command.NumgenValidateRuleCommand;
import io.mango.numgen.api.vo.NumgenRuleValidationVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "mango-numgen", contextId = "numgenFeignClient", path = "/numgen")
public interface NumgenFeignClient extends NumgenApi {

    @Override
    @PostMapping("/next")
    R<String> nextValue(@RequestBody NumgenNextCommand command);

    @Override
    @PostMapping("/batch")
    R<List<String>> batchValue(@RequestBody NumgenBatchCommand command);

    @Override
    @PostMapping("/rules/validate")
    R<NumgenRuleValidationVO> validateRule(@RequestBody NumgenValidateRuleCommand command);
}
