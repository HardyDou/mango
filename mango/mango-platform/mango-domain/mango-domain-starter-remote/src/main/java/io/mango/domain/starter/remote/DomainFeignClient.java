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
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 业务域 Feign 适配器。
 */
@FeignClient(name = "mango-domain", contextId = "domainFeignClient", path = "/domain/domains")
public interface DomainFeignClient extends DomainApi {

    /**
     * 分页查询业务域。
     *
     * @param query 查询条件。
     * @return 业务域分页结果。
     */
    @Override
    @GetMapping("/page")
    R<PageResult<DomainVO>> page(@SpringQueryMap DomainPageQuery query);

    /**
     * 查询业务域树。
     *
     * @param query 查询条件。
     * @return 业务域树。
     */
    @Override
    @GetMapping("/tree")
    R<List<DomainVO>> tree(@SpringQueryMap DomainPageQuery query);

    /**
     * 查询启用业务域树。
     *
     * @return 启用的业务域树。
     */
    @Override
    @GetMapping("/enabled-tree")
    R<List<DomainVO>> enabledTree();

    /**
     * 查询业务域详情。
     *
     * @param id 业务域 ID。
     * @return 业务域详情。
     */
    @Override
    @GetMapping("/detail")
    R<DomainVO> detail(@RequestParam("id") Long id);

    /**
     * 按编码查询业务域详情。
     *
     * @param domainCode 业务域编码。
     * @return 业务域详情。
     */
    @Override
    @GetMapping("/code")
    R<DomainVO> detailByCode(@RequestParam("domainCode") String domainCode);

    /**
     * 新增业务域。
     *
     * @param command 新增命令。
     * @return 新增业务域 ID。
     */
    @Override
    @PostMapping
    R<Long> create(@RequestBody CreateDomainCommand command);

    /**
     * 修改业务域。
     *
     * @param command 修改命令。
     * @return 是否修改成功。
     */
    @Override
    @PutMapping
    R<Boolean> update(@RequestBody UpdateDomainCommand command);

    /**
     * 更新业务域状态。
     *
     * @param command 状态更新命令。
     * @return 是否更新成功。
     */
    @Override
    @PutMapping("/status")
    R<Boolean> updateStatus(@RequestBody UpdateDomainStatusCommand command);

    /**
     * 逻辑删除业务域。
     *
     * @param id 业务域 ID。
     * @return 是否删除成功。
     */
    @Override
    @DeleteMapping
    R<Boolean> delete(@RequestParam("id") Long id);
}
