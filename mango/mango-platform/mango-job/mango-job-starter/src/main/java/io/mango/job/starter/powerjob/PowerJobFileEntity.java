package io.mango.job.starter.powerjob;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * PowerJob DFS MySQL 文件记录，只读用于读取 PowerJob 原生日志。
 */
@Getter
@Setter
@TableName("powerjob_files")
public class PowerJobFileEntity {

    private Long id;

    private String bucket;

    private String name;

    private Long length;

    private Integer status;

    private byte[] data;
}
