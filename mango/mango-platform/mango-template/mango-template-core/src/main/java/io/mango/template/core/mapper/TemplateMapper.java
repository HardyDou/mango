package io.mango.template.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.template.core.entity.TemplateEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模板 Mapper。
 */
@Mapper
public interface TemplateMapper extends BaseMapper<TemplateEntity> {
}
