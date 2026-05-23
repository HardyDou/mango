package io.mango.numgen.starter.remote;

import io.mango.common.result.R;
import io.mango.numgen.api.NumgenGeneratorApi;
import io.mango.numgen.api.NumgenRuleApi;
import io.mango.numgen.api.NumgenSegmentApi;
import io.mango.numgen.api.command.NumgenBatchCommand;
import io.mango.numgen.api.command.NumgenNextCommand;
import io.mango.numgen.api.command.NumgenValidateRuleCommand;
import io.mango.numgen.api.command.NumgenPreviewCommand;
import io.mango.numgen.api.command.NumgenPublishCommand;
import io.mango.numgen.api.command.SaveNumgenGeneratorCommand;
import io.mango.numgen.api.command.SaveNumgenRuleCommand;
import io.mango.numgen.api.command.SaveNumgenRuleSegmentCommand;
import io.mango.numgen.api.command.UpdateNumgenGeneratorStatusCommand;
import io.mango.numgen.api.command.UpdateNumgenRuleStatusCommand;
import io.mango.numgen.api.query.NumgenGeneratorPageQuery;
import io.mango.numgen.api.query.NumgenRulePageQuery;
import io.mango.numgen.api.query.NumgenSegmentPageQuery;
import io.mango.numgen.api.vo.NumgenGeneratorVO;
import io.mango.numgen.api.vo.NumgenPreviewVO;
import io.mango.numgen.api.vo.NumgenRuleValidationVO;
import io.mango.numgen.api.vo.NumgenRuleSegmentVO;
import io.mango.numgen.api.vo.NumgenRuleVO;
import io.mango.common.vo.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "mango-numgen", path = "/numgen")
public interface NumgenFeignClient extends NumgenGeneratorApi, NumgenRuleApi, NumgenSegmentApi, io.mango.numgen.api.NumgenApi {

    @Override
    @PostMapping("/next")
    R<String> nextValue(@RequestBody NumgenNextCommand command);

    @Override
    @PostMapping("/batch")
    R<List<String>> batchValue(@RequestBody NumgenBatchCommand command);

    @Override
    @PostMapping("/rules/validate")
    R<NumgenRuleValidationVO> validateRule(@RequestBody NumgenValidateRuleCommand command);

    @Override
    @GetMapping("/generators/page")
    R<PageResult<NumgenGeneratorVO>> pageGenerators(@SpringQueryMap NumgenGeneratorPageQuery query);

    @Override
    @GetMapping("/generators/detail")
    R<NumgenGeneratorVO> detailGenerator(@RequestParam Long id);

    @Override
    @PostMapping("/generators")
    R<Long> createGenerator(@RequestBody SaveNumgenGeneratorCommand command);

    @Override
    @PutMapping("/generators")
    R<Boolean> updateGenerator(@RequestBody SaveNumgenGeneratorCommand command);

    @Override
    @PutMapping("/generators/status")
    R<Boolean> updateGeneratorStatus(@RequestBody UpdateNumgenGeneratorStatusCommand command);

    @Override
    @DeleteMapping("/generators")
    R<Boolean> deleteGenerator(@RequestParam Long id);

    @Override
    @GetMapping("/rules/page")
    R<PageResult<NumgenRuleVO>> pageRules(@SpringQueryMap NumgenRulePageQuery query);

    @Override
    @GetMapping("/rules/detail")
    R<NumgenRuleVO> detailRule(@RequestParam Long id);

    @Override
    @PostMapping("/rules")
    R<Long> createRule(@RequestBody SaveNumgenRuleCommand command);

    @Override
    @PutMapping("/rules")
    R<Boolean> updateRule(@RequestBody SaveNumgenRuleCommand command);

    @Override
    @PutMapping("/rules/status")
    R<Boolean> updateRuleStatus(@RequestBody UpdateNumgenRuleStatusCommand command);

    @Override
    @DeleteMapping("/rules")
    R<Boolean> deleteRule(@RequestParam Long id);

    @Override
    @PostMapping("/rules/publish")
    R<Boolean> publishRule(@RequestBody NumgenPublishCommand command);

    @Override
    @PostMapping("/rules/preview")
    R<NumgenPreviewVO> previewRule(@RequestBody NumgenPreviewCommand command);

    @Override
    @GetMapping("/segments/page")
    R<PageResult<NumgenRuleSegmentVO>> pageSegments(@SpringQueryMap NumgenSegmentPageQuery query);

    @Override
    @GetMapping("/segments/detail")
    R<NumgenRuleSegmentVO> detailSegment(@RequestParam Long id);

    @Override
    @PostMapping("/segments")
    R<Long> createSegment(@RequestBody SaveNumgenRuleSegmentCommand command);

    @Override
    @PutMapping("/segments")
    R<Boolean> updateSegment(@RequestBody SaveNumgenRuleSegmentCommand command);

    @Override
    @DeleteMapping("/segments")
    R<Boolean> deleteSegment(@RequestParam Long id);
}
