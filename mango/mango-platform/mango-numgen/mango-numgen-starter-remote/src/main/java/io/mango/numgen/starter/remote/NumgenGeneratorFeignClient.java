package io.mango.numgen.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.NumgenGeneratorApi;
import io.mango.numgen.api.command.SaveNumgenGeneratorCommand;
import io.mango.numgen.api.command.UpdateNumgenGeneratorStatusCommand;
import io.mango.numgen.api.query.NumgenGeneratorPageQuery;
import io.mango.numgen.api.vo.NumgenGeneratorVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mango-numgen", contextId = "numgenGeneratorFeignClient", path = "/numgen/generators")
public interface NumgenGeneratorFeignClient extends NumgenGeneratorApi {

    @Override
    @GetMapping("/page")
    R<PageResult<NumgenGeneratorVO>> pageGenerators(@SpringQueryMap NumgenGeneratorPageQuery query);

    @Override
    @GetMapping("/detail")
    R<NumgenGeneratorVO> detailGenerator(@RequestParam Long id);

    @Override
    @PostMapping
    R<Long> createGenerator(@RequestBody SaveNumgenGeneratorCommand command);

    @Override
    @PutMapping
    R<Boolean> updateGenerator(@RequestBody SaveNumgenGeneratorCommand command);

    @Override
    @PutMapping("/status")
    R<Boolean> updateGeneratorStatus(@RequestBody UpdateNumgenGeneratorStatusCommand command);

    @Override
    @DeleteMapping
    R<Boolean> deleteGenerator(@RequestParam Long id);
}
