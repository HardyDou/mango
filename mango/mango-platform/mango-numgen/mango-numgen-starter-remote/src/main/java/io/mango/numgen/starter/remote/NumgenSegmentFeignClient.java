package io.mango.numgen.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.NumgenSegmentApi;
import io.mango.numgen.api.command.SaveNumgenRuleSegmentCommand;
import io.mango.numgen.api.query.NumgenSegmentPageQuery;
import io.mango.numgen.api.vo.NumgenRuleSegmentVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mango-numgen", contextId = "numgenSegmentFeignClient", path = "/numgen/segments")
public interface NumgenSegmentFeignClient extends NumgenSegmentApi {

    @Override
    @GetMapping("/page")
    R<PageResult<NumgenRuleSegmentVO>> pageSegments(@SpringQueryMap NumgenSegmentPageQuery query);

    @Override
    @GetMapping("/detail")
    R<NumgenRuleSegmentVO> detailSegment(@RequestParam Long id);

    @Override
    @PostMapping
    R<Long> createSegment(@RequestBody SaveNumgenRuleSegmentCommand command);

    @Override
    @PutMapping
    R<Boolean> updateSegment(@RequestBody SaveNumgenRuleSegmentCommand command);

    @Override
    @DeleteMapping
    R<Boolean> deleteSegment(@RequestParam Long id);
}
