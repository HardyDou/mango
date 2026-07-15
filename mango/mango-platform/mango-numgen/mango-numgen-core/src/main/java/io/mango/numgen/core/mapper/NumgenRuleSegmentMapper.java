package io.mango.numgen.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.numgen.core.entity.NumgenRuleSegmentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NumgenRuleSegmentMapper extends BaseMapper<NumgenRuleSegmentEntity> {

    List<NumgenRuleSegmentEntity> selectByRuleId(@Param("ruleId") Long ruleId,
                                                 @Param("tenantId") String tenantId);

    int physicalDeleteByRuleId(@Param("ruleId") Long ruleId);
}
