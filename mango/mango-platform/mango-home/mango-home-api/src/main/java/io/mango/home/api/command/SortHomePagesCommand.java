package io.mango.home.api.command;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SortHomePagesCommand implements Serializable {

    @NotEmpty(message = "首页排序不能为空")
    private List<Long> ids;
}
