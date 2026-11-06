package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.GameScore;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.GameScoreMapper;
import com.fenglin.springboottest.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 森林小游戏成绩接口（boonie_game_score）
 *   GET  /api/games/scores      —— 我的三款游戏最佳成绩
 *   POST /api/games/scores      —— 上报一局成绩 { gameId, score }
 *   GET  /api/games/leaderboard —— 各游戏排行榜 Top10
 *
 * 计分规则：honey / mole 分数越大越好；match 为配对步数，越小越好。
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class GameScoreController {

    /** 游戏编码 -> 中文名 */
    private static final Map<String, String> GAME_NAMES = new LinkedHashMap<>();

    static {
        GAME_NAMES.put("honey", "接蜂蜜");
        GAME_NAMES.put("mole", "打松鼠");
        GAME_NAMES.put("match", "森林翻牌");
    }

    @Autowired
    private GameScoreMapper scoreMapper;
    @Autowired
    private AuthService authService;

    /** 我的最佳成绩 */
    @GetMapping("/games/scores")
    public Map<String, Object> myScores(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        User me = authService.verifyToken(bearer(auth));
        if (me == null) throw new BizException(401, "未登录或登录已过期");

        QueryWrapper<GameScore> qw = new QueryWrapper<>();
        qw.eq("user_id", me.getId());
        List<Map<String, Object>> list = new ArrayList<>();
        for (GameScore g : scoreMapper.selectList(qw)) list.add(scoreMap(g));
        return ok(list);
    }

    /** 上报成绩：只有破纪录才更新 best_score，plays 每局 +1 */
    @PostMapping("/games/scores")
    public Map<String, Object> submit(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {

        User me = authService.verifyToken(bearer(auth));
        if (me == null) throw new BizException(401, "未登录或登录已过期");

        String gameId = str(body.get("gameId"));
        if (!GAME_NAMES.containsKey(gameId)) throw new BizException(400, "未知的游戏");
        int score = intOf(body.get("score"), -1);
        if (score < 0) throw new BizException(400, "成绩不合法");

        QueryWrapper<GameScore> qw = new QueryWrapper<>();
        qw.eq("user_id", me.getId()).eq("game_id", gameId);
        GameScore g = scoreMapper.selectOne(qw);

        boolean isNew = false;
        if (g == null) {
            g = new GameScore();
            g.setUserId(me.getId());
            g.setUserName(me.getName());
            g.setGameId(gameId);
            g.setGameName(GAME_NAMES.get(gameId));
            g.setBestScore(score);
            g.setPlays(1);
            g.setCreatedAt(LocalDateTime.now());
            g.setUpdatedAt(LocalDateTime.now());
            scoreMapper.insert(g);
            isNew = true;
        } else {
            int old = g.getBestScore() == null ? 0 : g.getBestScore();
            boolean better = "match".equals(gameId) ? (old <= 0 || score < old) : score > old;
            if (better) {
                g.setBestScore(score);
                isNew = true;
            }
            g.setPlays((g.getPlays() == null ? 0 : g.getPlays()) + 1);
            g.setUserName(me.getName());
            g.setUpdatedAt(LocalDateTime.now());
            scoreMapper.updateById(g);
        }

        Map<String, Object> out = ok(scoreMap(g));
        out.put("isNewRecord", isNew);
        return out;
    }

    /** 排行榜：每款游戏 Top10（未登录也可查看） */
    @GetMapping("/games/leaderboard")
    public Map<String, Object> leaderboard() {
        List<Map<String, Object>> blocks = new ArrayList<>();
        for (Map.Entry<String, String> e : GAME_NAMES.entrySet()) {
            QueryWrapper<GameScore> qw = new QueryWrapper<>();
            qw.eq("game_id", e.getKey());
            qw.orderByAsc("match".equals(e.getKey()), "best_score");
            if (!"match".equals(e.getKey())) qw.orderByDesc("best_score");
            qw.last("LIMIT 10");
            List<Map<String, Object>> rows = new ArrayList<>();
            for (GameScore g : scoreMapper.selectList(qw)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("userName", g.getUserName());
                m.put("bestScore", g.getBestScore());
                m.put("plays", g.getPlays());
                rows.add(m);
            }
            Map<String, Object> block = new LinkedHashMap<>();
            block.put("gameId", e.getKey());
            block.put("gameName", e.getValue());
            block.put("rows", rows);
            blocks.add(block);
        }
        return ok(blocks);
    }

    private Map<String, Object> scoreMap(GameScore g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("gameId", g.getGameId());
        m.put("gameName", g.getGameName());
        m.put("bestScore", g.getBestScore());
        m.put("plays", g.getPlays());
        m.put("updatedAt", g.getUpdatedAt() == null ? "" : g.getUpdatedAt().toString());
        return m;
    }

    private String bearer(String auth) {
        if (auth == null) return null;
        String a = auth.trim();
        return a.startsWith("Bearer ") ? a.substring(7).trim() : a;
    }

    private String str(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private int intOf(Object v, int def) {
        try {
            return Integer.parseInt(str(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private Map<String, Object> ok(Object data) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("data", data);
        return m;
    }
}
