package io.mango.cms.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SaveCmsAdDeliveryCommand {

    private Long id;

    @NotNull(message = "站点 ID 不能为空")
    private Long siteId;

    @NotNull(message = "广告位 ID 不能为空")
    private Long adId;

    @NotBlank(message = "投放名称不能为空")
    @Size(max = 128, message = "投放名称最多128个字符")
    private String deliveryName;

    @NotBlank(message = "物料类型不能为空")
    @Pattern(regexp = "TEXT|RICH_TEXT|HTML|IMAGE|SINGLE_IMAGE|MULTI_IMAGE|VIDEO", message = "物料类型不合法")
    private String materialType;

    @Size(max = 255, message = "标题最多255个字符")
    private String title;

    @Size(max = 1024, message = "文本内容最多1024个字符")
    private String textContent;

    private String richContent;

    private String htmlContent;

    @Size(max = 128, message = "图片文件 ID 最多128个字符")
    private String imageFileId;

    @Size(max = 1024, message = "多图片文件 ID 最多1024个字符")
    private String imageFileIds;

    @Size(max = 128, message = "视频文件 ID 最多128个字符")
    private String videoFileId;

    @Size(max = 128, message = "封面文件 ID 最多128个字符")
    private String coverFileId;

    @Size(max = 512, message = "跳转地址最多512个字符")
    private String jumpUrl;

    @Pattern(regexp = "SELF|BLANK", message = "打开方式不合法")
    private String openTarget;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer sort;

    @Pattern(regexp = "ENABLED|DISABLED", message = "状态不合法")
    private String status;
}
