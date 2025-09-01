package org.openoa.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.openoa.base.interf.TenantField;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_dict_data")
public class DictData implements TenantField, Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long Id;
    @TableField("dict_sort")
    private  Integer sort;
    @TableField("dict_label")
    private String label;
    @TableField("dict_value")
    private String value;
    @TableField("dic_value_type")
    private Integer valueType;
    @TableField("dict_type")
    private String dictType;
    @TableField("dict_second_level_type")
    private String dicSecondLevelType;
    @TableField("css_class")
    private String cssClass;
    @TableField("list_class")
    private String listClass;
    @TableField("is_default")
    private String isDefault;
    @TableField("is_del")
    private Integer isDel;
    @TableField("tenant_id")
    private String tenantId;
    @TableField("create_time")
    private Date createTime;
    @TableField("create_user")
    private String createUser;
    @TableField("update_time")
    private Date updateTime;
    @TableField("update_user")
    private String updateUser;
    @TableField("remark")
    private String remark;
}
