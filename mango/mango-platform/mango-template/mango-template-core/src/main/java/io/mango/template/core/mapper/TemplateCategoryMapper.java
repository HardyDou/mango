package io.mango.template.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.template.core.entity.TemplateCategoryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模板分类 Mapper。
 */
@Mapper
public interface TemplateCategoryMapper extends BaseMapper<TemplateCategoryEntity> {
}
