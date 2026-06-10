package io.mango.domain.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.domain.api.DomainApi;
import io.mango.domain.api.command.CreateDomainCommand;
import io.mango.domain.api.command.UpdateDomainCommand;
import io.mango.domain.api.command.UpdateDomainStatusCommand;
import io.mango.domain.api.query.DomainPageQuery;
import io.mango.domain.api.vo.DomainVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业务域 Feign 适配器。
 */
@FeignClient(name = "mango-domain", path = "/domain/domains")
public interface DomainFeignClient extends DomainApi {

    @Override
    @GetMapping("/page")
    R<PageResult<DomainVO>> page(DomainPageQuery query);

    @Override
    @GetMapping("/tree")
    R<List<DomainVO>> tree(DomainPageQuery query);

    @Override
    @GetMapping("/enabled-tree")
    R<List<DomainVO>> enabledTree();

    @Override
    @GetMapping("/detail")
    R<DomainVO> detail(@RequestParam("id") Long id);

    @Override
    @GetMapping("/code")
    R<DomainVO> detailByCode(@RequestParam("domainCode") String domainCode);

    @Override
    @PostMapping
    R<Long> create(@RequestBody CreateDomainCommand command);

    @Override
    @PutMapping
    R<Boolean> update(@RequestBody UpdateDomainCommand command);

    @Override
    @PutMapping("/status")
    R<Boolean> updateStatus(@RequestBody UpdateDomainStatusCommand command);

    @Override
    @DeleteMapping
    R<Boolean> delete(@RequestParam("id") Long id);
}
