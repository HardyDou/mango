package io.mango.i18n.api.vo;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
@Schema(description = "国际化键值条目")
public class I18nEntryVO {

    @JsonIgnore
    @Schema(description = "国际化键")
    private final String name;

    @JsonIgnore
    @Schema(description = "当前语言文案")
    private final String value;

    @JsonAnyGetter
    public Map<String, String> asJson() {
        return Map.of(name, value);
    }
}
