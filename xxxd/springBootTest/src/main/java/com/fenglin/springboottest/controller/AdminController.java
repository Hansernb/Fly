package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.AvatarStorage;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.Attraction;
import com.fenglin.springboottest.entity.GameScore;
import com.fenglin.springboottest.entity.Habitat;
import com.fenglin.springboottest.entity.Review;
import com.fenglin.springboottest.entity.Show;
import com.fenglin.springboottest.entity.Ticket;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.AttractionMapper;
import com.fenglin.springboottest.mapper.GameScoreMapper;
import com.fenglin.springboottest.mapper.HabitatMapper;
import com.fenglin.springboottest.mapper.ReviewMapper;
import com.fenglin.springboottest.mapper.ShowMapper;
import com.fenglin.springboottest.mapper.TicketMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.AuthService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理系统接口（/api/admin/**）
 * 全部接口都会校验：1) 令牌有效  2) 该用户 permission == 0（管理员）
 * 普通用户（permission == 1）一律 403，无法增删改查任何数据
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://192.168.68.11:5173")
public class AdminController {

    @Autowired
    private AuthService authService;
    @Autowired
    private AttractionMapper attractionMapper;
    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AvatarStorage avatarStorage;
    @Autowired
    private HabitatMapper habitatMapper;
    @Autowired
    private ShowMapper showMapper;
    @Autowired
    private TicketMapper ticketMapper;
    @Autowired
    private GameScoreMapper gameScoreMapper;
    @Autowired
    private ObjectMapper objectMapper;

    /** 统一鉴权：必须是管理员 */
    private User requireAdmin(String authHeader) {
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7) : "";
        User u = authService.verifyToken(token);
        if (u == null)
            throw new BizException(401, "未登录或登录已过期");
        if (!authService.isAdmin(u))
            throw new BizException(403, "权限不足：仅管理员可操作后台数据");
        return u;
    }

    private Map<String, Object> ok(Object data) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("data", data);
        return m;
    }

    private Map<String, Object> attractionMap(Attraction a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("name", a.getName());
        m.put("emoji", a.getEmoji());
        m.put("description", a.getDescription());
        m.put("tag", a.getTag());
        m.put("waitMinutes", a.getWaitMinutes());
        m.put("color", a.getColor());
        return m;
    }

    // ==================== 游乐项目管理 ====================

    /** GET /api/admin/attractions —— 项目列表（后台用） */
    @GetMapping("/attractions")
    public Map<String, Object> listAttractions(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Attraction a : attractionMapper.selectList(null)) {
            list.add(attractionMap(a));
        }
        return ok(list);
    }

    /** POST /api/admin/attractions —— 新增项目 */
    @PostMapping("/attractions")
    public Map<String, Object> createAttraction(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {
        requireAdmin(auth);

        String name = str(body.get("name"));
        if (name == null || name.isBlank())
            throw new BizException(400, "项目名称不能为空");

        Attraction a = new Attraction();
        a.setName(name);
        a.setEmoji(def(body.get("emoji"), "🎡"));
        a.setDescription(def(body.get("description"), ""));
        a.setTag(def(body.get("tag"), "亲子"));
        a.setWaitMinutes(intVal(body.get("waitMinutes"), 0));
        a.setColor(def(body.get("color"), "#2f7d4f"));
        a.setCreatedAt(LocalDateTime.now());
        attractionMapper.insert(a);
        return ok(attractionMap(a));
    }

    /** PUT /api/admin/attractions/{id} —— 修改项目 */
    @PutMapping("/attractions/{id}")
    public Map<String, Object> updateAttraction(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        requireAdmin(auth);

        Attraction a = attractionMapper.selectById(id);
        if (a == null)
            throw new BizException(404, "项目不存在");

        if (body.containsKey("name")) a.setName(str(body.get("name")));
        if (body.containsKey("emoji")) a.setEmoji(str(body.get("emoji")));
        if (body.containsKey("description")) a.setDescription(str(body.get("description")));
        if (body.containsKey("tag")) a.setTag(str(body.get("tag")));
        if (body.containsKey("waitMinutes")) a.setWaitMinutes(intVal(body.get("waitMinutes"), a.getWaitMinutes()));
        if (body.containsKey("color")) a.setColor(str(body.get("color")));
        attractionMapper.updateById(a);
        return ok(attractionMap(a));
    }

    /** DELETE /api/admin/attractions/{id} —— 删除项目（连带删除其评价） */
    @DeleteMapping("/attractions/{id}")
    public Map<String, Object> deleteAttraction(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id) {
        requireAdmin(auth);
        if (attractionMapper.selectById(id) == null)
            throw new BizException(404, "项目不存在");
        reviewMapper.delete(new LambdaQueryWrapper<Review>().eq(Review::getAttractionId, id));
        attractionMapper.deleteById(id);
        return ok(id);
    }

    // ==================== 游客评价管理 ====================

    /** GET /api/admin/reviews —— 全部评价（带所属项目名） */
    @GetMapping("/reviews")
    public Map<String, Object> listReviews(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        Map<Long, String> names = new LinkedHashMap<>();
        for (Attraction a : attractionMapper.selectList(null)) {
            names.put(a.getId(), a.getName());
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Review r : reviewMapper.selectList(null)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("attractionId", r.getAttractionId());
            m.put("attraction", names.getOrDefault(r.getAttractionId(), "（已删除项目）"));
            m.put("name", r.getReviewerName());
            m.put("avatar", r.getAvatar());
            m.put("rating", r.getRating());
            m.put("date", r.getReviewDate() != null ? r.getReviewDate().toString() : "");
            m.put("text", r.getContent());
            list.add(m);
        }
        return ok(list);
    }

    /** DELETE /api/admin/reviews/{id} —— 删除评价 */
    @DeleteMapping("/reviews/{id}")
    public Map<String, Object> deleteReview(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id) {
        requireAdmin(auth);
        if (reviewMapper.selectById(id) == null)
            throw new BizException(404, "评价不存在");
        reviewMapper.deleteById(id);
        return ok(id);
    }

    /**
     * POST /api/admin/avatars/clean —— 手动清理头像孤儿文件。
     * 定时任务每天 3:10 会自动跑一次；这里提供给管理员按需触发
     * （比如刚发现磁盘占用异常，不想等到凌晨）。
     */
    @PostMapping("/avatars/clean")
    public Map<String, Object> cleanAvatarOrphans(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        int removed = avatarStorage.cleanOrphans();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("removed", removed);
        m.put("message", removed > 0 ? "已清理 " + removed + " 个孤儿文件" : "没有需要清理的文件");
        return m;
    }

    // ==================== 用户与权限管理 ====================

    /** GET /api/admin/users —— 用户列表（不返回密码） */
    @GetMapping("/users")
    public Map<String, Object> listUsers(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        List<Map<String, Object>> list = new ArrayList<>();
        for (User u : userMapper.selectList(null)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("email", u.getEmail());
            m.put("permission", u.getPermission() == null ? 1 : u.getPermission());
            m.put("avatar", u.getAvatar() == null ? "" : u.getAvatar());
            m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
            list.add(m);
        }
        return ok(list);
    }

    /** PUT /api/admin/users/{id} —— 修改权限 { permission } 或重置密码 { password } */
    @PutMapping("/users/{id}")
    public Map<String, Object> updateUser(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        User admin = requireAdmin(auth);
        User u = userMapper.selectById(id);
        if (u == null)
            throw new BizException(404, "用户不存在");

        if (body.containsKey("permission")) {
            Integer p = intVal(body.get("permission"), 1);
            if (p != 0 && p != 1)
                throw new BizException(400, "权限值只能是 0（管理员）或 1（默认）");
            // 不允许把自己降级，避免出现无管理员的死锁
            if (u.getId().equals(admin.getId()) && p != 0)
                throw new BizException(400, "不能取消自己的管理员权限");
            u.setPermission(p);
        }
        if (body.containsKey("password")) {
            String np = str(body.get("password"));
            if (np == null || np.length() < 6)
                throw new BizException(400, "新密码至少 6 位");
            u.setPassword(np);   // 明文
        }
        String oldAvatar = u.getAvatar();
        if (body.containsKey("avatar")) {
            String av = str(body.get("avatar"));
            // 只接受两种合法形态：默认头像 /img/defaults/xxx.jpg，或上传头像 /api/avatar/文件名
            // 含 .. 或反斜杠的一律拒绝，避免把穿越路径写进库
            boolean ok = av == null || av.isEmpty()
                    || (av.startsWith("/img/defaults/") && av.matches("^/img/defaults/[\\w.-]+\\.(jpg|jpeg|png|gif|webp)$"))
                    || (avatarStorage.filenameOf(av) != null);
            if (!ok)
                throw new BizException(400, "头像地址不合法");
            u.setAvatar(av == null ? "" : av);
        }
        userMapper.updateById(u);

        // 头像确实变了才清理旧文件；且仅当它不再被任何用户/评价引用时才真正删除
        String newAvatar = u.getAvatar();
        if (!java.util.Objects.equals(oldAvatar, newAvatar)) {
            avatarStorage.deleteIfUnreferenced(oldAvatar);
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("email", u.getEmail());
        m.put("permission", u.getPermission());
        m.put("avatar", u.getAvatar() == null ? "" : u.getAvatar());
        return ok(m);
    }

    // ==================== 栖息地管理 ====================

    private Map<String, Object> habitatMap(Habitat h) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", h.getId());
        m.put("emoji", h.getEmoji());
        m.put("img", h.getImg());
        m.put("name", h.getName());
        m.put("description", h.getDescription());
        m.put("residents", h.getResidents());
        m.put("mood", h.getMood());
        m.put("color", h.getColor());
        m.put("rating", h.getRating());
        return m;
    }

    /** GET /api/admin/habitats */
    @GetMapping("/habitats")
    public Map<String, Object> listHabitats(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Habitat h : habitatMapper.selectList(null)) list.add(habitatMap(h));
        return ok(list);
    }

    /** POST /api/admin/habitats */
    @PostMapping("/habitats")
    public Map<String, Object> createHabitat(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {
        requireAdmin(auth);
        String name = str(body.get("name"));
        if (name == null || name.isBlank()) throw new BizException(400, "栖息地名称不能为空");
        Habitat h = new Habitat();
        h.setName(name);
        h.setEmoji(def(body.get("emoji"), "🌲"));
        h.setImg(str(body.get("img")));
        h.setDescription(def(body.get("description"), ""));
        h.setResidents(def(body.get("residents"), ""));
        h.setMood(def(body.get("mood"), ""));
        h.setColor(def(body.get("color"), "#2f7d4f"));
        h.setCreatedAt(LocalDateTime.now());
        habitatMapper.insert(h);
        return ok(habitatMap(h));
    }

    /** PUT /api/admin/habitats/{id} */
    @PutMapping("/habitats/{id}")
    public Map<String, Object> updateHabitat(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        requireAdmin(auth);
        Habitat h = habitatMapper.selectById(id);
        if (h == null) throw new BizException(404, "栖息地不存在");
        if (body.containsKey("name")) h.setName(str(body.get("name")));
        if (body.containsKey("emoji")) h.setEmoji(str(body.get("emoji")));
        if (body.containsKey("img")) h.setImg(str(body.get("img")));
        if (body.containsKey("description")) h.setDescription(str(body.get("description")));
        if (body.containsKey("residents")) h.setResidents(str(body.get("residents")));
        if (body.containsKey("mood")) h.setMood(str(body.get("mood")));
        if (body.containsKey("color")) h.setColor(str(body.get("color")));
        habitatMapper.updateById(h);
        return ok(habitatMap(h));
    }

    /** DELETE /api/admin/habitats/{id} */
    @DeleteMapping("/habitats/{id}")
    public Map<String, Object> deleteHabitat(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id) {
        requireAdmin(auth);
        if (habitatMapper.selectById(id) == null) throw new BizException(404, "栖息地不存在");
        habitatMapper.deleteById(id);
        return ok(id);
    }

    // ==================== 演出管理 ====================

    private Map<String, Object> showMap(Show s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("showTime", s.getShowTime());
        m.put("emoji", s.getEmoji());
        m.put("name", s.getName());
        m.put("place", s.getPlace());
        m.put("tag", s.getTag());
        return m;
    }

    /** GET /api/admin/shows */
    @GetMapping("/shows")
    public Map<String, Object> listShows(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Show s : showMapper.selectList(null)) list.add(showMap(s));
        return ok(list);
    }

    /** POST /api/admin/shows */
    @PostMapping("/shows")
    public Map<String, Object> createShow(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {
        requireAdmin(auth);
        String name = str(body.get("name"));
        if (name == null || name.isBlank()) throw new BizException(400, "演出名称不能为空");
        Show s = new Show();
        s.setName(name);
        s.setShowTime(def(body.get("showTime"), "10:00"));
        s.setEmoji(def(body.get("emoji"), "🎪"));
        s.setPlace(def(body.get("place"), "中心广场"));
        s.setTag(def(body.get("tag"), "约 20 分钟"));
        s.setCreatedAt(LocalDateTime.now());
        showMapper.insert(s);
        return ok(showMap(s));
    }

    /** PUT /api/admin/shows/{id} */
    @PutMapping("/shows/{id}")
    public Map<String, Object> updateShow(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        requireAdmin(auth);
        Show s = showMapper.selectById(id);
        if (s == null) throw new BizException(404, "演出不存在");
        if (body.containsKey("showTime")) s.setShowTime(str(body.get("showTime")));
        if (body.containsKey("emoji")) s.setEmoji(str(body.get("emoji")));
        if (body.containsKey("name")) s.setName(str(body.get("name")));
        if (body.containsKey("place")) s.setPlace(str(body.get("place")));
        if (body.containsKey("tag")) s.setTag(str(body.get("tag")));
        showMapper.updateById(s);
        return ok(showMap(s));
    }

    /** DELETE /api/admin/shows/{id} */
    @DeleteMapping("/shows/{id}")
    public Map<String, Object> deleteShow(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id) {
        requireAdmin(auth);
        if (showMapper.selectById(id) == null) throw new BizException(404, "演出不存在");
        showMapper.deleteById(id);
        return ok(id);
    }

    // ==================== 门票管理 ====================

    private Map<String, Object> ticketMap(Ticket t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("typeCode", t.getTypeCode());
        m.put("emoji", t.getEmoji());
        m.put("name", t.getName());
        m.put("price", t.getPrice());
        m.put("originalPrice", t.getOriginalPrice());
        m.put("discountLabel", t.getDiscountLabel());
        m.put("tagLabel", t.getTagLabel());
        m.put("descText", t.getDescText());
        m.put("color", t.getColor());
        m.put("features", t.getFeatures());
        m.put("enabled", t.getEnabled());
        m.put("sortNo", t.getSortNo());
        return m;
    }

    /** GET /api/admin/tickets */
    @GetMapping("/tickets")
    public Map<String, Object> listTickets(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Ticket t : ticketMapper.selectList(null)) list.add(ticketMap(t));
        return ok(list);
    }

    /** POST /api/admin/tickets */
    @PostMapping("/tickets")
    public Map<String, Object> createTicket(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {
        requireAdmin(auth);
        String name = str(body.get("name"));
        if (name == null || name.isBlank()) throw new BizException(400, "票种名称不能为空");
        String typeCode = str(body.get("typeCode"));
        if (typeCode == null || !typeCode.matches("[a-z]{3,20}"))
            throw new BizException(400, "票种编码需为 3–20 位小写字母（如 adult / student）");

        Ticket t = new Ticket();
        t.setName(name);
        t.setTypeCode(typeCode);
        t.setEmoji(def(body.get("emoji"), "🎫"));
        t.setOriginalPrice(new java.math.BigDecimal(intVal(body.get("originalPrice"), 0)));
        t.setPrice(new java.math.BigDecimal(intVal(body.get("price"), 0)));
        t.setDiscountLabel(def(body.get("discountLabel"), ""));
        t.setTagLabel(def(body.get("tagLabel"), "标准"));
        t.setDescText(def(body.get("descText"), ""));
        t.setColor(def(body.get("color"), "#2f7d4f"));
        t.setFeatures(str(body.get("features")));
        t.setHot(false);
        t.setEnabled(true);
        t.setSortNo(99);
        t.setCreatedAt(LocalDateTime.now());
        ticketMapper.insert(t);
        return ok(ticketMap(t));
    }

    /** PUT /api/admin/tickets/{id} */
    @PutMapping("/tickets/{id}")
    public Map<String, Object> updateTicket(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        requireAdmin(auth);
        Ticket t = ticketMapper.selectById(id);
        if (t == null) throw new BizException(404, "票种不存在");
        if (body.containsKey("typeCode")) t.setTypeCode(str(body.get("typeCode")));
        if (body.containsKey("emoji")) t.setEmoji(str(body.get("emoji")));
        if (body.containsKey("name")) t.setName(str(body.get("name")));
        if (body.containsKey("price")) t.setPrice(new java.math.BigDecimal(intVal(body.get("price"), 0)));
        if (body.containsKey("originalPrice")) t.setOriginalPrice(new java.math.BigDecimal(intVal(body.get("originalPrice"), 0)));
        if (body.containsKey("discountLabel")) t.setDiscountLabel(str(body.get("discountLabel")));
        if (body.containsKey("tagLabel")) t.setTagLabel(str(body.get("tagLabel")));
        if (body.containsKey("descText")) t.setDescText(str(body.get("descText")));
        if (body.containsKey("color")) t.setColor(str(body.get("color")));
        if (body.containsKey("features")) t.setFeatures(str(body.get("features")));
        ticketMapper.updateById(t);
        return ok(ticketMap(t));
    }

    /** DELETE /api/admin/tickets/{id} */
    @DeleteMapping("/tickets/{id}")
    public Map<String, Object> deleteTicket(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id) {
        requireAdmin(auth);
        if (ticketMapper.selectById(id) == null) throw new BizException(404, "票种不存在");
        ticketMapper.deleteById(id);
        return ok(id);
    }

    // ==================== 小游戏成绩管理 ====================

    private Map<String, Object> gameMap(GameScore g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", g.getId());
        m.put("userId", g.getUserId());
        m.put("userName", g.getUserName());
        m.put("gameId", g.getGameId());
        m.put("gameName", g.getGameName());
        m.put("bestScore", g.getBestScore());
        m.put("plays", g.getPlays());
        m.put("updatedAt", g.getUpdatedAt() != null ? g.getUpdatedAt().toString() : "");
        return m;
    }

    /** GET /api/admin/games?gameId=honey&q=张 —— 成绩查询（可按游戏/玩家名过滤） */
    @GetMapping("/games")
    public Map<String, Object> listGames(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(value = "gameId", required = false) String gameId,
            @RequestParam(value = "q", required = false) String q) {
        requireAdmin(auth);
        LambdaQueryWrapper<GameScore> qw = new LambdaQueryWrapper<>();
        if (gameId != null && !gameId.isBlank()) qw.eq(GameScore::getGameId, gameId.trim());
        if (q != null && !q.isBlank()) qw.like(GameScore::getUserName, q.trim());
        qw.orderByDesc(GameScore::getBestScore);
        List<Map<String, Object>> list = new ArrayList<>();
        for (GameScore g : gameScoreMapper.selectList(qw)) list.add(gameMap(g));
        return ok(list);
    }

    /** POST /api/admin/games —— 手工补录一条成绩（如线下活动录入） */
    @PostMapping("/games")
    public Map<String, Object> createGame(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {
        requireAdmin(auth);
        String gameId = str(body.get("gameId"));
        if (gameId == null || !gameId.matches("honey|mole|match"))
            throw new BizException(400, "游戏编码只能是 honey / mole / match / run");
        String userName = str(body.get("userName"));
        if (userName == null || userName.isBlank()) throw new BizException(400, "玩家名不能为空");

        GameScore g = new GameScore();
        g.setGameId(gameId);
        g.setGameName(def(body.get("gameName"), gameId));
        g.setUserName(userName);
        g.setBestScore(intVal(body.get("bestScore"), 0));
        g.setPlays(intVal(body.get("plays"), 0));
        g.setCreatedAt(LocalDateTime.now());
        g.setUpdatedAt(LocalDateTime.now());
        gameScoreMapper.insert(g);
        return ok(gameMap(g));
    }

    /** PUT /api/admin/games/{id} —— 修改成绩（bestScore / plays / gameName） */
    @PutMapping("/games/{id}")
    public Map<String, Object> updateGame(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        requireAdmin(auth);
        GameScore g = gameScoreMapper.selectById(id);
        if (g == null) throw new BizException(404, "成绩记录不存在");
        if (body.containsKey("bestScore")) {
            int v = intVal(body.get("bestScore"), 0);
            if (v < 0) throw new BizException(400, "最佳成绩不能为负数");
            g.setBestScore(v);
        }
        if (body.containsKey("plays")) {
            int v = intVal(body.get("plays"), 0);
            if (v < 0) throw new BizException(400, "游玩局数不能为负数");
            g.setPlays(v);
        }
        if (body.containsKey("gameName")) g.setGameName(str(body.get("gameName")));
        g.setUpdatedAt(LocalDateTime.now());
        gameScoreMapper.updateById(g);
        return ok(gameMap(g));
    }

    /** DELETE /api/admin/games/{id} —— 删除成绩记录（清作弊/重置榜单用） */
    @DeleteMapping("/games/{id}")
    public Map<String, Object> deleteGame(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id) {
        requireAdmin(auth);
        if (gameScoreMapper.selectById(id) == null) throw new BizException(404, "成绩记录不存在");
        gameScoreMapper.deleteById(id);
        return ok(id);
    }

    // ==================== 小工具 ====================

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private String def(Object o, String fallback) {
        return o == null ? fallback : String.valueOf(o);
    }

    private Integer intVal(Object o, Integer fallback) {
        if (o == null) return fallback;
        try {
            return Integer.valueOf(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
