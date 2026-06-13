package io.mango.numgen.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.numgen.core.entity.NumgenGenerator;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NumgenGeneratorMapper extends BaseMapper<NumgenGenerator> {
}
