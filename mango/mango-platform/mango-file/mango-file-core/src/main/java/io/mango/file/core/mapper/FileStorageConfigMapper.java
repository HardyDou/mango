package io.mango.file.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.file.core.entity.FileStorageConfigEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件存储配置 Mapper。
 */
@Mapper
public interface FileStorageConfigMapper extends BaseMapper<FileStorageConfigEntity> {
}
