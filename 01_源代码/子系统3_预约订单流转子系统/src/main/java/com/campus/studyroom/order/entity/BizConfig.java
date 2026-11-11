package com.campus.studyroom.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("biz_config")
public class BizConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置键（如:sign_in_deadline_minutes） */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 配置项中文说明，内部维护用 */
    private String description;
}