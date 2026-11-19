package io.mango.system.api;

import io.mango.common.result.R;
import io.mango.system.api.command.SaveDictDataCommand;
import io.mango.system.api.command.SaveDictTypeCommand;
import io.mango.system.api.query.DictOptionsBatchQuery;
import io.mango.system.api.vo.DictDataVO;
import io.mango.system.api.vo.DictOptionGroupVO;
import io.mango.system.api.vo.DictOptionVO;
import io.mango.system.api.vo.DictTreeNodeVO;
import io.mango.system.api.vo.DictTypeVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface DictApi {
    R<List<DictTypeVO>> listTypes(@Size(max = 64) String domainCode);
    R<DictTypeVO> getType(@NotNull Long id);
    R<Long> createType(@Valid SaveDictTypeCommand command);
    R<Boolean> updateType(@Valid SaveDictTypeCommand command);
    R<Boolean> deleteType(@NotNull Long id);
    R<List<DictDataVO>> listData(@Max(Long.MAX_VALUE) Long typeId);
    R<DictDataVO> getData(@NotNull Long id);
    R<Long> createData(@Valid SaveDictDataCommand command);
    R<Boolean> updateData(@Valid SaveDictDataCommand command);
    R<Boolean> deleteData(@NotNull Long id);
    R<List<DictOptionVO>> getOptions(@NotBlank @Size(max = 50) String typeCode);

    /**
     * 批量读取平铺字典选项。
     *
     * <p>一次查询覆盖全部字典类型，用于页面同时渲染多个字典（最多 24 个）时替代多次 {@link #getOptions(String)}。
     * 返回顺序与入参去重后的顺序一致；编码为空或只有空白的入参会被忽略；不存在的字典类型不报错，
     * 在结果里给出空选项列表。
     *
     * @param query 字典类型编码，最多 24 个，单个不超过 50 字符
     */
    R<List<DictOptionGroupVO>> getOptionsBatch(@Valid DictOptionsBatchQuery query);
    R<List<DictTreeNodeVO>> getTreeOptions(@NotBlank @Size(max = 50) String typeCode);
}
