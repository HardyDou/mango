package io.mango.file.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.file.core.entity.FileDirectoryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件逻辑目录 Mapper。
 */
@Mapper
public interface FileDirectoryMapper extends BaseMapper<FileDirectoryEntity> {
}
