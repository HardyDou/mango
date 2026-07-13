package io.mango.numgen.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.numgen.core.entity.NumgenSequence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NumgenSequenceMapper extends BaseMapper<NumgenSequence> {

    NumgenSequence selectByScope(@Param("genKey") String genKey,
                                 @Param("scopeKey") String scopeKey,
                                 @Param("tenantId") Long tenantId);

    /**
     * Atomically creates the scoped sequence or advances its current value.
     *
     * @param sequence initial sequence metadata
     * @param step allocation size
     * @return affected row count
     */
    int upsertAndAllocate(@Param("sequence") NumgenSequence sequence,
                          @Param("step") int step);
}
