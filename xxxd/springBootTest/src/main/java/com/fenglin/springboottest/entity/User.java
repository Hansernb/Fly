package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("boonie_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户名 / 显示名（前端注册字段 username，成功页展示 name） */
    private String name;
    /** 登录邮箱（唯一） */
    private String email;
    /** 登录密码（按需求：明文存储，不做加密） */
    private String password;
    /**
     * 权限：1 = 默认权限（注册用户），0 = 管理员权限
     * 仅管理员可进入后台管理系统；注册用户一律为默认权限 1
     */
    private Integer permission;
    private LocalDateTime createdAt;
}
