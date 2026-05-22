package io.mango.calendar.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class AddWorkdaysCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "日历编码不能为空")
    private String calendarCode;

    @NotNull(message = "起始日期不能为空")
    private LocalDate sourceDate;

    @NotNull(message = "工作日偏移量不能为空")
    private Integer amount;

    private boolean includeSource;

}
