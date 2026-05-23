package io.mango.numgen.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.numgen.core.entity.NumgenRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NumgenRuleMapper extends BaseMapper<NumgenRule> {

    @Select("""
            SELECT *
            FROM numgen_rule
            WHERE gen_key = #{genKey}
              AND tenant_id = #{tenantId}
              AND status = 1
              AND version_state = 'ACTIVE'
              AND del_flag = 0
            ORDER BY version DESC
            LIMIT 1
            """)
    NumgenRule selectActiveByGenKey(@Param("genKey") String genKey, @Param("tenantId") Long tenantId);

    @Select("""
            SELECT *
            FROM numgen_rule
            WHERE gen_key = #{genKey}
              AND tenant_id = #{tenantId}
              AND status = 1
              AND version_state = 'DRAFT'
              AND del_flag = 0
            ORDER BY version DESC
            LIMIT 1
            """)
    NumgenRule selectLatestDraftByGenKey(@Param("genKey") String genKey, @Param("tenantId") Long tenantId);

    @Select("""
            SELECT *
            FROM numgen_rule
            WHERE gen_key = #{genKey}
              AND tenant_id = #{tenantId}
              AND del_flag = 0
            ORDER BY version DESC
            """)
    List<NumgenRule> selectVersionsByGenKey(@Param("genKey") String genKey, @Param("tenantId") Long tenantId);
}
