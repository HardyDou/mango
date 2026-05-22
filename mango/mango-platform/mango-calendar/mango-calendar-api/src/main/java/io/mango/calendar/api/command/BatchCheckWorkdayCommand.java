package io.mango.calendar.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
public class BatchCheckWorkdayCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "日历编码不能为空")
    private String calendarCode;

    @NotEmpty(message = "日期列表不能为空")
    private List<LocalDate> dates;

}
