package io.mango.file.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.file.core.entity.FileHashMappingEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件秒传哈希映射 Mapper。
 */
@Mapper
public interface FileHashMappingMapper extends BaseMapper<FileHashMappingEntity> {
}
