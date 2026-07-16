package io.mango.system.api;

import io.mango.common.result.R;
import io.mango.system.api.command.SaveDictDataCommand;
import io.mango.system.api.command.SaveDictTypeCommand;
import io.mango.system.api.vo.DictDataVO;
import io.mango.system.api.vo.DictOptionVO;
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
}
