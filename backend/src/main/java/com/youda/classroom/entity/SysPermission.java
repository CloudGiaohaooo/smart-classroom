package com.youda.classroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限实体（对应 sys_permission 表，RBAC 权限模型）
 */
@Data
@TableName("sys_permission")
public class SysPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权限编码，如 course:write、submission:grade */
    private String permCode;

    private String permName;

    private String description;

    private LocalDateTime createTime;
}
