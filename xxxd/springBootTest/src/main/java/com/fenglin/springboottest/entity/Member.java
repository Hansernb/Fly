package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员档案（对应 boonie_member 表）
 * 会员身份绑定登录账号（user_id）：哪个账号下单，积分就加到该账号的会员卡上。
 * 游客（未登录）下单时仍按手机号兜底建档，之后首次登录下单会自动归并到账号名下。
 */
@Data
@TableName("boonie_member")
public class Member {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 绑定账号 id（boonie_user.id）；游客手机号会员为 null */
    private Long userId;
    /** 手机号（取票人手机号，游客会员的唯一标识；账号会员可为空） */
    private String phone;
    /** 昵称（默认「熊粉」+ 手机尾号四位） */
    private String nickname;
    /** 头像 emoji */
    private String avatar;
    /** 当前可用积分 */
    private Integer totalPoints;
    /** 累计获得积分（决定等级） */
    private Integer cumPoints;
    /** 普通会员 / 白银会员 / 黄金会员 */
    private String levelName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
