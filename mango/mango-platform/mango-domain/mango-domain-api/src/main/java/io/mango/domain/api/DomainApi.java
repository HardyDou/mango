package io.mango.domain.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.domain.api.command.CreateDomainCommand;
import io.mango.domain.api.command.UpdateDomainCommand;
import io.mango.domain.api.command.UpdateDomainStatusCommand;
import io.mango.domain.api.query.DomainPageQuery;
import io.mango.domain.api.vo.DomainVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 业务域 API 契约。
 */
@Validated
public interface DomainApi {

    /**
     * 分页查询业务域。
     *
     * @param query 查询条件。
     * @return 业务域分页结果。
     */
    R<PageResult<DomainVO>> page(@Valid DomainPageQuery query);

    /**
     * 查询业务域树。
     *
     * @param query 查询条件。
     * @return 业务域树。
     */
    R<List<DomainVO>> tree(@Valid DomainPageQuery query);

    /**
     * 查询启用业务域树。
     *
     * @return 启用的业务域树。
     */
    R<List<DomainVO>> enabledTree();

    /**
     * 查询业务域详情。
     *
     * @param id 业务域 ID。
     * @return 业务域详情。
     */
    R<DomainVO> detail(
            @NotNull(message = "业务域ID不能为空")
            @Positive(message = "业务域ID必须大于0") Long id);

    /**
     * 根据编码查询业务域。
     *
     * @param domainCode 业务域编码。
     * @return 业务域详情。
     */
    R<DomainVO> detailByCode(
            @NotBlank(message = "业务域编码不能为空") String domainCode);

    /**
     * 新增业务域。
     *
     * @param command 新增命令。
     * @return 新增业务域 ID。
     */
    R<Long> create(@Valid CreateDomainCommand command);

    /**
     * 修改业务域。
     *
     * @param command 修改命令。
     * @return 是否修改成功。
     */
    R<Boolean> update(@Valid UpdateDomainCommand command);

    /**
     * 启停业务域。
     *
     * @param command 状态更新命令。
     * @return 是否更新成功。
     */
    R<Boolean> updateStatus(@Valid UpdateDomainStatusCommand command);

    /**
     * 逻辑删除业务域。
     *
     * @param id 业务域 ID。
     * @return 是否删除成功。
     */
    R<Boolean> delete(
            @NotNull(message = "业务域ID不能为空")
            @Positive(message = "业务域ID必须大于0") Long id);
}
