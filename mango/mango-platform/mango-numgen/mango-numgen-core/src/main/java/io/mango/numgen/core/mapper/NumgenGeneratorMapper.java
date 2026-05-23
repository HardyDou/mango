package io.mango.numgen.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.numgen.core.entity.NumgenGenerator;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NumgenGeneratorMapper extends BaseMapper<NumgenGenerator> {

    @Select("""
            SELECT *
            FROM numgen_generator
            WHERE gen_key = #{genKey}
              AND tenant_id = #{tenantId}
              AND del_flag = 0
            LIMIT 1
            """)
    NumgenGenerator selectByKey(@Param("genKey") String genKey, @Param("tenantId") Long tenantId);
}
