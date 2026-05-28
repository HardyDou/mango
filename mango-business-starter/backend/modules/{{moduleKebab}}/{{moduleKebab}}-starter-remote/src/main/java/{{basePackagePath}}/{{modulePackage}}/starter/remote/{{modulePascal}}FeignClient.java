package {{basePackage}}.{{modulePackage}}.starter.remote;

import {{basePackage}}.{{modulePackage}}.api.{{modulePascal}}Api;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * {{moduleName}}远程适配器。
 */
@FeignClient(name = "{{moduleKebab}}", path = "/{{moduleKebab}}")
public interface {{modulePascal}}FeignClient extends {{modulePascal}}Api {
}
