package io.mango.numgen.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.numgen.core.entity.NumgenHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NumgenHistoryMapper extends BaseMapper<NumgenHistory> {
}
