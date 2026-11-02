package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private com.fenglin.springboottest.common.AvatarStorage avatarStorage;

    private Map<String, Object> publicUser(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("email", u.getEmail());
        // 权限：1 = 默认权限（普通注册用户），0 = 管理员权限
        m.put("permission", u.getPermission() == null ? 1 : u.getPermission());
        m.put("avatar", u.getAvatar() == null ? "" : u.getAvatar());
        return m;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("time", java.time.Instant.now().toString());
        m.put("users", authService.countUsers());
        return m;
    }

    /**
     * PUT /api/me/avatar —— 登录用户修改自己的头像（无需管理员权限）。
     * 与后台改他人头像的规则完全一致：只接受默认头像或已上传头像的地址。
     */
    @PutMapping("/me/avatar")
    public Map<String, Object> updateMyAvatar(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {
        User me = authService.verifyToken(bearer(auth));
        if (me == null)
            throw new BizException(401, "未登录或登录已过期");

        Object raw = body.get("avatar");
        String av = raw == null ? "" : String.valueOf(raw);
        if (!isValidAvatarUrl(av))
            throw new BizException(400, "头像地址不合法");

        String oldAvatar = me.getAvatar();
        me.setAvatar(av);
        authService.updateAvatar(me.getId(), av);

        // 头像变了才清理旧文件；且仅当它不再被任何用户/评价引用时才真正删除
        if (!java.util.Objects.equals(oldAvatar, av)) {
            avatarStorage.deleteIfUnreferenced(oldAvatar);
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("user", publicUser(me));
        return m;
    }

    private String bearer(String auth) {
        if (auth == null) return null;
        String a = auth.trim();
        return a.startsWith("Bearer ") ? a.substring(7).trim() : a;
    }

    /** 只接受两种合法形态：默认头像 /img/defaults/xxx.jpg，或上传头像 /api/avatar/文件名 */
    private boolean isValidAvatarUrl(String av) {
        if (av == null || av.isEmpty() || av.length() > 255) return false;
        if (av.contains("..") || av.contains("\\")) return false;
        return av.matches("^/img/defaults/[\\w.-]+\\.(jpg|jpeg|png|gif|webp)$")
                || av.matches("^/api/avatar/[\\w-]+\\.(jpg|jpeg|png|gif|webp)$");
    }

    @PostMapping("/send-code")
    public Map<String, Object> sendCode(@RequestBody Map<String, String> body) {
        String code = authService.sendCode(body.get("email"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("expire", 300);
        m.put("demoCode", code);
        return m;
    }

    /**
     * 账号字段统一取 username（用户名登录）；若前端仍传 email 则兼容回退
     */
    private String accountOf(Map<String, String> body) {
        String a = body.get("username");
        if (a == null || a.isBlank()) a = body.get("email");
        return a == null ? "" : a.trim();
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String account = accountOf(body);
        String password = body.get("password");
        String code = body.get("code");

        boolean byCode = code != null && !code.isEmpty();
        User u = byCode
                // 验证码登录仍需靠邮箱接收验证码
                ? authService.loginByCode(account, code)
                : authService.loginByPassword(account, password);

        String token = authService.issueToken(u);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", token);
        m.put("user", publicUser(u));
        m.put("method", byCode ? "code" : "password");
        return m;
    }

    /**
     * POST /api/admin/login —— 后台管理系统登录
     * 与普通登录的区别：额外校验必须是管理员权限（permission == 0），否则 403 拒绝
     */
    @PostMapping("/admin/login")
    public Map<String, Object> adminLogin(@RequestBody Map<String, String> body) {
        User u = authService.adminLogin(accountOf(body), body.get("password"));
        String token = authService.issueToken(u);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", token);
        m.put("user", publicUser(u));
        return m;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        User u = authService.register(body.get("username"), body.get("email"), body.get("password"), body.get("avatar"));
        String token = authService.issueToken(u);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", token);
        m.put("user", publicUser(u));
        return m;
    }

    @GetMapping("/me")
    public Map<String, Object> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        String token = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : "";
        User u = authService.verifyToken(token);
        if (u == null)
            throw new BizException(401, "未登录或登录已过期");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("user", publicUser(u));
        return m;
    }
}
