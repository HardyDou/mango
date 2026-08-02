package io.mango.identity.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateCurrentUserProfileCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 100)
    @Schema(description = "用户昵称")
    private String nickname;

    @Size(max = 500)
    @Schema(description = "头像文件引用")
    private String avatar;

    @Size(max = 100)
    @Schema(description = "实名姓名")
    private String realName;

    @Size(max = 32)
    @Schema(description = "实名证件类型")
    private String documentType;

    @Size(max = 128)
    @Schema(description = "实名证件号码")
    private String documentNumber;
}
