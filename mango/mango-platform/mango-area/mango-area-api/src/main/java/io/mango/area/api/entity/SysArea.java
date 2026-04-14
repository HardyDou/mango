package io.mango.area.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 行政区划实体 - 支持自由区划变化.
 *
 * @author Mango
 */
@Data
@TableName("sys_area")
public class SysArea implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 父级ID (0为根节点)
     */
    private Long pid;

    /**
     * 地区名称
     */
    private String name;

    /**
     * 地区字母(用于拼音排序)
     */
    private String letter;

    /**
     * 地区编码 (支持自由区划变化，可修改)
     * 格式示例: 110000, 110100, 110101
     * 行政区划采用高德adcode标准，自有区划采用自定义编码
     */
    private Long adcode;

    /**
     * 经纬度 (高德坐标系)
     */
    private String location;

    /**
     * 排序值
     */
    private Integer areaSort;

    /**
     * 状态 (0-未生效, 1-生效)
     */
    private String areaStatus;

    /**
     * 地区类型 (0-国家, 1-省/直辖市, 2-城市, 3-区县, 4-街道)
     * 支持自定义区划: 5-自定义区域
     */
    private String areaType;

    /**
     * 是否热门 (0-否, 1-是)
     */
    private String hot;

    /**
     * 城市编码
     */
    private String cityCode;

    /**
     * 租户ID
     */
    private Long tenantId;
}
