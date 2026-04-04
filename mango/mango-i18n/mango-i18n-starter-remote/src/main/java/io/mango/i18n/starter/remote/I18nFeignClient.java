package io.mango.i18n.starter.remote;

import io.mango.i18n.api.SysI18nApi;
import io.mango.i18n.api.entity.SysI18n;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * I18n Feign client - implements SysI18nApi for remote calls
 *
 * @author Mango
 */
@FeignClient(name = "i18n-service", path = "/i18n")
public interface I18nFeignClient extends SysI18nApi {

    @Override
    @GetMapping("/public")
    Map<String, List<Map<String, String>>> listMap();

    @Override
    @GetMapping("/public/lang")
    List<Map<String, String>> listByLang(@RequestParam String lang);

    @Override
    @GetMapping("/languages")
    List<String> getSupportedLanguages();

    @Override
    @GetMapping("/public/name")
    SysI18n getByName(@RequestParam String name);
}
