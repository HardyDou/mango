package io.mango.template.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.template.api.TemplateCategoryApi;
import io.mango.template.api.command.SaveTemplateCategoryCommand;
import io.mango.template.api.command.UpdateTemplateCategoryStatusCommand;
import io.mango.template.api.query.TemplateCategoryPageQuery;
import io.mango.template.api.vo.TemplateCategoryVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模板分类 Feign 适配器。
 */
@FeignClient(name = "mango-template", path = "/template/categories")
public interface TemplateCategoryFeignClient extends TemplateCategoryApi {

    @Override
    @GetMapping("/page")
    R<PageResult<TemplateCategoryVO>> page(TemplateCategoryPageQuery query);

    @Override
    @GetMapping("/list")
    R<List<TemplateCategoryVO>> list(TemplateCategoryPageQuery query);

    @Override
    @GetMapping("/detail")
    R<TemplateCategoryVO> detail(@RequestParam("id") Long id);

    @Override
    @PostMapping
    R<Long> create(@RequestBody SaveTemplateCategoryCommand command);

    @Override
    @PutMapping
    R<Boolean> update(@RequestBody SaveTemplateCategoryCommand command);

    @Override
    @PutMapping("/status")
    R<Boolean> updateStatus(@RequestBody UpdateTemplateCategoryStatusCommand command);

    @Override
    @DeleteMapping
    R<Boolean> delete(@RequestParam("id") Long id);
}
