package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 森林演出（对应 boonie_show 表，首页「🎪 今日森林演出」区块） */
@Data
@TableName("boonie_show")
public class Show {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 演出时间（如 10:30） */
    private String showTime;
    /** 图标 emoji */
    private String emoji;
    /** 演出名称 */
    private String name;
    /** 地点说明 */
    private String place;
    /** 时长标签（如「约 40 分钟」） */
    private String tag;
    private LocalDateTime createdAt;
}
