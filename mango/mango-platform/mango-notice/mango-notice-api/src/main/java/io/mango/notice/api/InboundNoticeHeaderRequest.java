package io.mango.notice.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/** A single sanitized message header transferred by an inbound adapter. */
@Getter
public class InboundNoticeHeaderRequest {

    @Schema(description = "消息头名称")
    @NotBlank
    @Size(max = 200)
    private final String name;

    @Schema(description = "消息头值")
    @NotBlank
    @Size(max = 4000)
    private final String value;

    public InboundNoticeHeaderRequest(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String name() { return name; }
    public String value() { return value; }
}
