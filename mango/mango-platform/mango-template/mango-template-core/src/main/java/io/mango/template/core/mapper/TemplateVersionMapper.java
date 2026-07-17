package io.mango.template.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.template.core.entity.TemplateVersionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模板版本 Mapper。
 */
@Mapper
public interface TemplateVersionMapper extends BaseMapper<TemplateVersionEntity> {
}
