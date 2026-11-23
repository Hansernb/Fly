package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fenglin.springboottest.common.AvatarStorage;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.Attraction;
import com.fenglin.springboottest.entity.Banner;
import com.fenglin.springboottest.entity.GameScore;
import com.fenglin.springboottest.entity.Habitat;
import com.fenglin.springboottest.entity.Member;
import com.fenglin.springboottest.entity.Notice;
import com.fenglin.springboottest.entity.Review;
import com.fenglin.springboottest.entity.Show;
import com.fenglin.springboottest.entity.Ticket;
import com.fenglin.springboottest.entity.TicketOrder;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.AttractionMapper;
import com.fenglin.springboottest.mapper.BannerMapper;
import com.fenglin.springboottest.mapper.GameScoreMapper;
import com.fenglin.springboottest.mapper.HabitatMapper;
import com.fenglin.springboottest.mapper.MemberMapper;
import com.fenglin.springboottest.mapper.NoticeMapper;
import com.fenglin.springboottest.mapper.ReviewMapper;
import com.fenglin.springboottest.mapper.ShowMapper;
import com.fenglin.springboottest.mapper.TicketMapper;
import com.fenglin.springboottest.mapper.TicketOrderMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.AuthService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 后台管理系统接口（/api/admin/**）
 * 全部接口都会校验：1) 令牌有效  2) 该用户 permission == 0（管理员）
 * 普通用户（permission == 1）一律 403，无法增删改查任何数据
 */
@RestController
@RequestMapping("/api/admin")
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
    @Autowired
    private TicketOrderMapper ticketOrderMapper;
    @Autowired
    private BannerMapper bannerMapper;
    @Autowired
    private NoticeMapper noticeMapper;
    @Autowired
    private MemberMapper memberMapper;

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

    /** GET /api/admin/reviews?status=pending —— 全部评价（带所属项目名，可按审核状态过滤） */
    @GetMapping("/reviews")
    public Map<String, Object> listReviews(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(value = "status", required = false) String status) {
        requireAdmin(auth);
        Map<Long, String> names = new LinkedHashMap<>();
        for (Attraction a : attractionMapper.selectList(null)) {
            names.put(a.getId(), a.getName());
        }
        LambdaQueryWrapper<Review> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) qw.eq(Review::getStatus, status.trim());
        qw.orderByDesc(Review::getId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Review r : reviewMapper.selectList(qw)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("attractionId", r.getAttractionId());
            m.put("attraction", names.getOrDefault(r.getAttractionId(), "（已删除项目）"));
            m.put("name", r.getReviewerName());
            m.put("avatar", r.getAvatar());
            m.put("rating", r.getRating());
            m.put("date", r.getReviewDate() != null ? r.getReviewDate().toString() : "");
            m.put("text", r.getContent());
            m.put("status", r.getStatus() == null ? "approved" : r.getStatus());
            list.add(m);
        }
        return ok(list);
    }

    /** PUT /api/admin/reviews/{id} —— 审核评价 { status: approved|rejected } */
    @PutMapping("/reviews/{id}")
    public Map<String, Object> reviewReview(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        requireAdmin(auth);
        Review r = reviewMapper.selectById(id);
        if (r == null) throw new BizException(404, "评价不存在");
        String status = str(body.get("status"));
        if (!"approved".equals(status) && !"rejected".equals(status))
            throw new BizException(400, "审核状态只能是 approved 或 rejected");
        r.setStatus(status);
        reviewMapper.updateById(r);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("status", status);
        return ok(m);
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

        if (body.containsKey("name")) {
            String name = str(body.get("name"));
            if (name == null || name.isBlank()) throw new BizException(400, "用户名不能为空");
            Long dup = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getName, name).ne(User::getId, id));
            if (dup != null && dup > 0) throw new BizException(400, "用户名已存在");
            u.setName(name.trim());
        }
        if (body.containsKey("email")) {
            String email = str(body.get("email"));
            if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
                throw new BizException(400, "邮箱格式不正确");
            Long dup = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, email).ne(User::getId, id));
            if (dup != null && dup > 0) throw new BizException(400, "邮箱已存在");
            u.setEmail(email.trim());
        }
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

    /** POST /api/admin/users —— 新增用户（后台创建管理员 / 普通用户） */
    @PostMapping("/users")
    public Map<String, Object> createUser(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {
        requireAdmin(auth);
        String name = str(body.get("name"));
        String email = str(body.get("email"));
        String pwd = str(body.get("password"));
        if (name == null || name.isBlank()) throw new BizException(400, "用户名不能为空");
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new BizException(400, "邮箱格式不正确");
        if (pwd == null || pwd.length() < 6) throw new BizException(400, "密码至少 6 位");

        Long dup = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getName, name.trim()).or().eq(User::getEmail, email.trim()));
        if (dup != null && dup > 0) throw new BizException(400, "用户名或邮箱已存在");

        User u = new User();
        u.setName(name.trim());
        u.setEmail(email.trim());
        u.setPassword(pwd);                       // 明文（与项目现有约定一致）
        u.setPermission(intVal(body.get("permission"), 1));
        String av = str(body.get("avatar"));
        u.setAvatar(av == null || av.isBlank() ? "" : av);
        u.setCreatedAt(LocalDateTime.now());
        userMapper.insert(u);
        return ok(userMap(u));
    }

    /** DELETE /api/admin/users/{id} —— 删除用户（不允许删除自己） */
    @DeleteMapping("/users/{id}")
    public Map<String, Object> deleteUser(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id) {
        User admin = requireAdmin(auth);
        if (admin.getId().equals(id)) throw new BizException(400, "不能删除当前登录的管理员");
        User u = userMapper.selectById(id);
        if (u == null) throw new BizException(404, "用户不存在");
        // 会员档案解绑，避免下次注册同名账号时积分落到旧档案上
        for (Member m : memberMapper.selectList(
                new LambdaQueryWrapper<Member>().eq(Member::getUserId, id))) {
            m.setUserId(null);
            memberMapper.updateById(m);
        }
        userMapper.deleteById(id);
        return ok(id);
    }

    private Map<String, Object> userMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("email", u.getEmail());
        m.put("permission", u.getPermission() == null ? 1 : u.getPermission());
        m.put("avatar", u.getAvatar() == null ? "" : u.getAvatar());
        m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
        return m;
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

    // ==================== 数据看板 ====================

    /**
     * GET /api/admin/dashboard —— 访客数 / 订单量 / 门票销量 / 好评率 + 近 7 天趋势 + 票种销量分布
     * 口径：
     *   访客数 = 注册用户数 + 游客订单去重手机号；订单量 = 非取消订单数；
     *   门票销量 = 非取消订单 quantity 之和；好评率 = 已通过评价中 4 星及以上占比。
     */
    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);

        List<User> users = userMapper.selectList(null);
        List<TicketOrder> orders = ticketOrderMapper.selectList(null);
        List<Review> reviews = reviewMapper.selectList(null);

        List<TicketOrder> active = new ArrayList<>();
        for (TicketOrder o : orders) if (!"已取消".equals(o.getStatus())) active.add(o);

        Set<String> guestPhones = new HashSet<>();
        for (TicketOrder o : active) {
            if (o.getUserId() == null && o.getBuyerPhone() != null && !o.getBuyerPhone().isBlank())
                guestPhones.add(o.getBuyerPhone());
        }

        long ticketsSold = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        for (TicketOrder o : active) {
            ticketsSold += o.getQuantity() == null ? 0 : o.getQuantity();
            if (o.getTotalAmount() != null) revenue = revenue.add(o.getTotalAmount());
        }

        List<Review> approved = new ArrayList<>();
        long pending = 0;
        for (Review r : reviews) {
            if ("pending".equals(r.getStatus())) pending++;
            if (r.getStatus() == null || "approved".equals(r.getStatus())) approved.add(r);
        }
        long good = goodCount(approved);
        double goodRate = approved.isEmpty() ? 0 : Math.round(good * 10000.0 / approved.size()) / 100.0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("visitors", users.size() + guestPhones.size());
        summary.put("orders", active.size());
        summary.put("ticketsSold", ticketsSold);
        summary.put("goodRate", goodRate);
        summary.put("revenue", revenue.setScale(2, java.math.RoundingMode.HALF_UP));
        summary.put("totalReviews", approved.size());
        summary.put("goodReviews", good);
        summary.put("pendingReviews", pending);

        // 近 7 天趋势（含今日）
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            long dayVisitors = users.stream().filter(u -> sameDay(u.getCreatedAt(), day)).count();
            Set<String> dayGuests = new HashSet<>();
            for (TicketOrder o : active) {
                if (o.getUserId() == null && sameDay(o.getCreatedAt(), day)
                        && o.getBuyerPhone() != null && !o.getBuyerPhone().isBlank())
                    dayGuests.add(o.getBuyerPhone());
            }
            long dayOrders = active.stream().filter(o -> sameDay(o.getCreatedAt(), day)).count();
            long dayTickets = 0;
            for (TicketOrder o : active) {
                if (sameDay(o.getCreatedAt(), day)) dayTickets += o.getQuantity() == null ? 0 : o.getQuantity();
            }
            List<Review> dayReviews = new ArrayList<>();
            for (Review r : approved) if (r.getReviewDate() != null && r.getReviewDate().equals(day)) dayReviews.add(r);
            long dayGood = goodCount(dayReviews);
            double dayRate = dayReviews.isEmpty() ? 0 : Math.round(dayGood * 10000.0 / dayReviews.size()) / 100.0;

            Map<String, Object> t = new LinkedHashMap<>();
            t.put("date", day.toString().substring(5));
            t.put("visitors", dayVisitors + dayGuests.size());
            t.put("orders", dayOrders);
            t.put("tickets", dayTickets);
            t.put("goodRate", dayRate);
            trend.add(t);
        }

        // 票种销量分布
        Map<String, Long> byTicket = new LinkedHashMap<>();
        for (TicketOrder o : active) {
            String name = o.getTicketName() == null || o.getTicketName().isBlank() ? "其它" : o.getTicketName();
            byTicket.merge(name, (long) (o.getQuantity() == null ? 0 : o.getQuantity()), Long::sum);
        }
        List<Map.Entry<String, Long>> entries = new ArrayList<>(byTicket.entrySet());
        entries.sort(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed());
        List<Map<String, Object>> ticketSales = new ArrayList<>();
        for (Map.Entry<String, Long> e : entries) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("name", e.getKey());
            t.put("qty", e.getValue());
            ticketSales.add(t);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", summary);
        data.put("trend", trend);
        data.put("ticketSales", ticketSales);
        data.put("updatedAt", LocalDateTime.now().toString().substring(0, 19));
        return ok(data);
    }

    private boolean sameDay(LocalDateTime t, LocalDate day) {
        return t != null && t.toLocalDate().equals(day);
    }

    private long goodCount(List<Review> list) {
        long n = 0;
        for (Review r : list) {
            if (r.getRating() != null && r.getRating().compareTo(BigDecimal.valueOf(4)) >= 0) n++;
        }
        return n;
    }

    // ==================== 轮播图管理 ====================

    private Map<String, Object> bannerMap(Banner b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("title", b.getTitle());
        m.put("subtitle", b.getSubtitle());
        m.put("tag", b.getTag());
        m.put("img", b.getImg());
        m.put("link", b.getLink());
        m.put("color", b.getColor());
        m.put("sortNo", b.getSortNo());
        m.put("enabled", b.getEnabled());
        return m;
    }

    @GetMapping("/banners")
    public Map<String, Object> listBanners(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Banner b : bannerMapper.selectList(new LambdaQueryWrapper<Banner>().orderByAsc(Banner::getSortNo, Banner::getId)))
            list.add(bannerMap(b));
        return ok(list);
    }

    @PostMapping("/banners")
    public Map<String, Object> createBanner(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {
        requireAdmin(auth);
        String title = str(body.get("title"));
        if (title == null || title.isBlank()) throw new BizException(400, "轮播标题不能为空");
        Banner b = new Banner();
        b.setTitle(title.trim());
        b.setSubtitle(def(body.get("subtitle"), ""));
        b.setTag(def(body.get("tag"), ""));
        b.setImg(str(body.get("img")));
        b.setLink(def(body.get("link"), "游乐项目"));
        b.setColor(def(body.get("color"), "#2f7d4f"));
        b.setSortNo(intVal(body.get("sortNo"), 99));
        b.setEnabled(true);
        b.setCreatedAt(LocalDateTime.now());
        bannerMapper.insert(b);
        return ok(bannerMap(b));
    }

    @PutMapping("/banners/{id}")
    public Map<String, Object> updateBanner(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        requireAdmin(auth);
        Banner b = bannerMapper.selectById(id);
        if (b == null) throw new BizException(404, "轮播图不存在");
        if (body.containsKey("title")) b.setTitle(str(body.get("title")));
        if (body.containsKey("subtitle")) b.setSubtitle(str(body.get("subtitle")));
        if (body.containsKey("tag")) b.setTag(str(body.get("tag")));
        if (body.containsKey("img")) b.setImg(str(body.get("img")));
        if (body.containsKey("link")) b.setLink(str(body.get("link")));
        if (body.containsKey("color")) b.setColor(str(body.get("color")));
        if (body.containsKey("sortNo")) b.setSortNo(intVal(body.get("sortNo"), b.getSortNo()));
        if (body.containsKey("enabled")) b.setEnabled(boolVal(body.get("enabled"), b.getEnabled()));
        bannerMapper.updateById(b);
        return ok(bannerMap(b));
    }

    @DeleteMapping("/banners/{id}")
    public Map<String, Object> deleteBanner(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id) {
        requireAdmin(auth);
        if (bannerMapper.selectById(id) == null) throw new BizException(404, "轮播图不存在");
        bannerMapper.deleteById(id);
        return ok(id);
    }

    // ==================== 公告管理 ====================

    private Map<String, Object> noticeMap(Notice n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("content", n.getContent());
        m.put("sortNo", n.getSortNo());
        m.put("enabled", n.getEnabled());
        return m;
    }

    @GetMapping("/notices")
    public Map<String, Object> listNotices(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Notice n : noticeMapper.selectList(new LambdaQueryWrapper<Notice>().orderByAsc(Notice::getSortNo, Notice::getId)))
            list.add(noticeMap(n));
        return ok(list);
    }

    @PostMapping("/notices")
    public Map<String, Object> createNotice(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {
        requireAdmin(auth);
        String content = str(body.get("content"));
        if (content == null || content.isBlank()) throw new BizException(400, "公告内容不能为空");
        Notice n = new Notice();
        n.setContent(content.trim());
        n.setSortNo(intVal(body.get("sortNo"), 99));
        n.setEnabled(true);
        n.setCreatedAt(LocalDateTime.now());
        noticeMapper.insert(n);
        return ok(noticeMap(n));
    }

    @PutMapping("/notices/{id}")
    public Map<String, Object> updateNotice(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        requireAdmin(auth);
        Notice n = noticeMapper.selectById(id);
        if (n == null) throw new BizException(404, "公告不存在");
        if (body.containsKey("content")) n.setContent(str(body.get("content")));
        if (body.containsKey("sortNo")) n.setSortNo(intVal(body.get("sortNo"), n.getSortNo()));
        if (body.containsKey("enabled")) n.setEnabled(boolVal(body.get("enabled"), n.getEnabled()));
        noticeMapper.updateById(n);
        return ok(noticeMap(n));
    }

    @DeleteMapping("/notices/{id}")
    public Map<String, Object> deleteNotice(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id) {
        requireAdmin(auth);
        if (noticeMapper.selectById(id) == null) throw new BizException(404, "公告不存在");
        noticeMapper.deleteById(id);
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

    private Boolean boolVal(Object o, Boolean fallback) {
        if (o == null) return fallback;
        if (o instanceof Boolean) return (Boolean) o;
        return "1".equals(String.valueOf(o)) || "true".equalsIgnoreCase(String.valueOf(o));
    }
}
