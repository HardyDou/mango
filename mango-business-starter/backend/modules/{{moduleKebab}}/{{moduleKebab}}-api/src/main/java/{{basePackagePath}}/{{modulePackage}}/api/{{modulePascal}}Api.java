package {{basePackage}}.{{modulePackage}}.api;

import {{basePackage}}.{{modulePackage}}.api.command.Create{{aggregatePascal}}Command;
import {{basePackage}}.{{modulePackage}}.api.command.Update{{aggregatePascal}}Command;
import {{basePackage}}.{{modulePackage}}.api.query.{{aggregatePascal}}PageQuery;
import {{basePackage}}.{{modulePackage}}.api.vo.{{aggregatePascal}}VO;
import io.mango.common.result.R;
import io.mango.infra.persistence.api.crud.DeleteCommand;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * {{moduleName}}对外接口。
 */
public interface {{modulePascal}}Api {

    R<Long> create(@Valid Create{{aggregatePascal}}Command command);

    R<Boolean> update(@Valid Update{{aggregatePascal}}Command command);

    R<Boolean> delete(@Valid DeleteCommand command);

    R<PersistencePageResult<{{aggregatePascal}}VO>> page(@Valid {{aggregatePascal}}PageQuery query);

    R<{{aggregatePascal}}VO> detail(@NotNull Long id);
}
