package {{basePackage}}.{{modulePackage}}.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import {{basePackage}}.{{modulePackage}}.core.entity.{{aggregatePascal}}Entity;
import org.apache.ibatis.annotations.Mapper;

/**
 * {{aggregatePascal}}数据访问接口。
 */
@Mapper
public interface {{aggregatePascal}}Mapper extends BaseMapper<{{aggregatePascal}}Entity> {
}
