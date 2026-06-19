package io.mango.numgen.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.NumgenRuleApi;
import io.mango.numgen.api.command.NumgenPreviewCommand;
import io.mango.numgen.api.command.NumgenPublishCommand;
import io.mango.numgen.api.command.SaveNumgenRuleCommand;
import io.mango.numgen.api.command.UpdateNumgenRuleStatusCommand;
import io.mango.numgen.api.query.NumgenRulePageQuery;
import io.mango.numgen.api.vo.NumgenPreviewVO;
import io.mango.numgen.api.vo.NumgenRuleVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mango-numgen", contextId = "numgenRuleFeignClient", path = "/numgen/rules")
public interface NumgenRuleFeignClient extends NumgenRuleApi {

    @Override
    @GetMapping("/page")
    R<PageResult<NumgenRuleVO>> pageRules(@SpringQueryMap NumgenRulePageQuery query);

    @Override
    @GetMapping("/detail")
    R<NumgenRuleVO> detailRule(@RequestParam Long id);

    @Override
    @PostMapping
    R<Long> createRule(@RequestBody SaveNumgenRuleCommand command);

    @Override
    @PutMapping
    R<Boolean> updateRule(@RequestBody SaveNumgenRuleCommand command);

    @Override
    @PutMapping("/status")
    R<Boolean> updateRuleStatus(@RequestBody UpdateNumgenRuleStatusCommand command);

    @Override
    @DeleteMapping
    R<Boolean> deleteRule(@RequestParam Long id);

    @Override
    @PostMapping("/publish")
    R<Boolean> publishRule(@RequestBody NumgenPublishCommand command);

    @Override
    @PostMapping("/preview")
    R<NumgenPreviewVO> previewRule(@RequestBody NumgenPreviewCommand command);
}
