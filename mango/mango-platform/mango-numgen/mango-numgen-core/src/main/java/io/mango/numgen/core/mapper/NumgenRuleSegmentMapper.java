package io.mango.numgen.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.numgen.core.entity.NumgenRuleSegment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NumgenRuleSegmentMapper extends BaseMapper<NumgenRuleSegment> {

    @Select("""
            SELECT *
            FROM numgen_rule_segment
            WHERE rule_id = #{ruleId}
              AND tenant_id = #{tenantId}
            ORDER BY sort_order ASC, id ASC
            """)
    List<NumgenRuleSegment> selectByRuleId(@Param("ruleId") Long ruleId, @Param("tenantId") Long tenantId);
}
