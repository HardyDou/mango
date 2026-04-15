package io.mango.org.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.org.core.entity.PostEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper extends BaseMapper<PostEntity> {
}
