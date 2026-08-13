package io.mango.notice.channel.wecom;

import lombok.AllArgsConstructor;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@AllArgsConstructor
public class WecomInboundRequest {

    @Schema(description = "回调签名")
    @NotBlank
    private final String signature;
    @Schema(description = "时间戳")
    @NotBlank
    private final String timestamp;
    @Schema(description = "随机数")
    @NotBlank
    private final String nonce;
    @Schema(description = "验证回显")
    @Size(max = 10000)
    private final String echoString;
    @Schema(description = "回调正文")
    @Size(max = 2_000_000)
    private final String body;

    public String signature() {
        return signature;
    }

    public String timestamp() {
        return timestamp;
    }

    public String nonce() {
        return nonce;
    }

    public String echoString() {
        return echoString;
    }

    public String body() {
        return body;
    }
}
