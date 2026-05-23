package io.mango.numgen.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.numgen.core.entity.NumgenSequence;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NumgenSequenceMapper extends BaseMapper<NumgenSequence> {

    @Select("""
            SELECT *
            FROM numgen_sequence
            WHERE gen_key = #{genKey}
              AND scope_key = #{scopeKey}
              AND tenant_id = #{tenantId}
            LIMIT 1
            """)
    NumgenSequence selectByScope(@Param("genKey") String genKey,
                                 @Param("scopeKey") String scopeKey,
                                 @Param("tenantId") Long tenantId);

    @Insert("""
            INSERT IGNORE INTO numgen_sequence
            (id, gen_key, rule_version, scope_key, current_value, version, tenant_id, create_time, update_time)
            VALUES
            (#{id}, #{genKey}, #{ruleVersion}, #{scopeKey}, #{currentValue}, 0, #{tenantId}, NOW(), NOW())
            """)
    int insertIgnore(NumgenSequence sequence);

    @Update("""
            UPDATE numgen_sequence
            SET current_value = current_value + #{step},
                version = version + 1,
                update_time = NOW()
            WHERE id = #{id}
              AND version = #{version}
            """)
    int allocateSegment(@Param("id") Long id,
                        @Param("version") Integer version,
                        @Param("step") int step);
}
