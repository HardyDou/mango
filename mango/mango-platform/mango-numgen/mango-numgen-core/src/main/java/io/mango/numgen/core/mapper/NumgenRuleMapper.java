package io.mango.numgen.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.numgen.core.entity.NumgenRuleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NumgenRuleMapper extends BaseMapper<NumgenRuleEntity> {

    NumgenRuleEntity selectActiveByGenKey(@Param("genKey") String genKey, @Param("tenantId") String tenantId);

    NumgenRuleEntity selectLatestDraftByGenKey(@Param("genKey") String genKey, @Param("tenantId") String tenantId);

    List<NumgenRuleEntity> selectVersionsByGenKey(@Param("genKey") String genKey, @Param("tenantId") String tenantId);

    NumgenRuleEntity selectVersionIncludingDeleted(@Param("tenantId") String tenantId,
                                             @Param("genKey") String genKey,
                                             @Param("version") Integer version);

    int physicalDeleteById(@Param("id") Long id);
}
