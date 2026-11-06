package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 小游戏最佳成绩（对应 boonie_game_score 表，每用户每游戏一条） */
@Data
@TableName("boonie_game_score")
public class GameScore {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String userName;
    /** 游戏编码：honey / mole / match */
    private String gameId;
    private String gameName;
    /** 最佳成绩：honey/mole 为分数（越大越好），match 为最少步数（越小越好） */
    private Integer bestScore;
    /** 游玩局数 */
    private Integer plays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
