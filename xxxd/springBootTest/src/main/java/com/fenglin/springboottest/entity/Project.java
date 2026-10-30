package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 项目登记表（对应 boonie_project 表，记录工作区下的核心前后端项目） */
@Data
@TableName("boonie_project")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 项目名称 */
    private String name;
    /** 项目类型：Spring Boot / Vue / Node 等 */
    private String type;
    /** 项目目录（相对工作区根目录） */
    @TableField("dir_path")
    private String dirPath;
    /** 主端口 */
    private Integer port;
    /** 健康检查地址 */
    @TableField("health_url")
    private String healthUrl;
    /** 备注说明 */
    private String description;
    private LocalDateTime createdAt;
}
