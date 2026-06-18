package {{basePackage}}.{{modulePackage}}.core.service;

import {{basePackage}}.{{modulePackage}}.core.entity.{{aggregatePascal}}Entity;
import io.mango.infra.persistence.api.crud.MangoCrudService;

/**
 * {{aggregatePascal}}内部服务。
 */
public interface I{{aggregatePascal}}Service extends MangoCrudService<{{aggregatePascal}}Entity> {
}
