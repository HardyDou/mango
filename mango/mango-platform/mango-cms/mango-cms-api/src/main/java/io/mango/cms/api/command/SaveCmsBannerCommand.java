package io.mango.cms.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SaveCmsBannerCommand {

    private Long id;

    @NotNull(message = "站点 ID 不能为空")
    private Long siteId;

    @NotBlank(message = "展示位置不能为空")
    @Size(max = 64, message = "展示位置最多64个字符")
    private String position;

    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题最多255个字符")
    private String title;

    @Size(max = 255, message = "副标题最多255个字符")
    private String subtitle;

    @NotBlank(message = "媒体类型不能为空")
    @Pattern(regexp = "IMAGE|VIDEO", message = "媒体类型不合法")
    private String mediaType;

    @Size(max = 128, message = "媒体文件 ID 最多128个字符")
    private String mediaFileId;

    @Size(max = 512, message = "跳转地址最多512个字符")
    private String jumpUrl;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer sort;

    @Pattern(regexp = "ENABLED|DISABLED", message = "状态不合法")
    private String status;
}
