package io.mango.file.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.file.core.entity.FileUploadPartEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件分片上传明细 Mapper。
 */
@Mapper
public interface FileUploadPartMapper extends BaseMapper<FileUploadPartEntity> {
}
