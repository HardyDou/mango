package io.mango.system.api;

import io.mango.common.result.R;
import io.mango.system.api.command.SavePersonalConfigCommand;
import io.mango.system.api.query.PersonalConfigQuery;
import io.mango.system.api.vo.PersonalConfigVO;

import java.util.List;

/**
 * 个人参数配置 API。
 */
public interface PersonalConfigApi {

    /**
     * 查询当前用户个人配置列表。
     *
     * @param query 查询条件。
     * @return 当前用户配置列表。
     */
    R<List<PersonalConfigVO>> list(PersonalConfigQuery query);

    /**
     * 查询当前用户单个配置。
     *
     * @param query 查询条件。
     * @return 当前用户配置。
     */
    R<PersonalConfigVO> getValue(PersonalConfigQuery query);

    /**
     * 保存当前用户个人配置。
     *
     * @param command 保存命令。
     * @return 保存后的配置。
     */
    R<PersonalConfigVO> save(SavePersonalConfigCommand command);

    /**
     * 删除当前用户个人配置。
     *
     * @param query 删除条件。
     * @return 是否删除成功。
     */
    R<Boolean> delete(PersonalConfigQuery query);
}
