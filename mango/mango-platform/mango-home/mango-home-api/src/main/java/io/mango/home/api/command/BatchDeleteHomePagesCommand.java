package io.mango.home.api.command;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BatchDeleteHomePagesCommand implements Serializable {

    @NotEmpty(message = "首页ID不能为空")
    private List<@NotNull(message = "首页ID不能为空") Long> ids;
}
