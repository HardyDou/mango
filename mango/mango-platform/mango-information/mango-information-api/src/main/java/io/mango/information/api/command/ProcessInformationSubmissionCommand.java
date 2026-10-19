package io.mango.information.api.command;

import io.mango.information.api.enums.InformationSubmissionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/** 采集记录处理命令。 */
@Data
public class ProcessInformationSubmissionCommand implements Serializable {
    @NotNull(message = "采集记录ID不能为空")
    private Long id;
    @NotNull(message = "处理状态不能为空")
    @Schema(description = "处理状态：PENDING、PROCESSING、COMPLETED、CLOSED")
    private InformationSubmissionStatus status;
    private String processRemark;
}
