package io.mango.file.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.file.core.entity.FileObjectEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物理文件对象 Mapper。
 */
@Mapper
public interface FileObjectMapper extends BaseMapper<FileObjectEntity> {
}
