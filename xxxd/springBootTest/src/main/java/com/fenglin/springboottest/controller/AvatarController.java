package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.AvatarStorage;
import com.fenglin.springboottest.common.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 头像上传与访问接口。
 * - POST /api/avatar/upload  上传图片，返回可访问的 URL（/api/avatar/文件名）
 * - GET  /api/avatar/{filename}  读取并返回头像文件
 *
 * 上传目录由 application*.properties 的 app.upload.dir 指定（默认 ./uploads/avatars）
 */
@RestController
@RequestMapping("/api/avatar")
@CrossOrigin(origins = "http://localhost:5173")
public class AvatarController {

    @Autowired
    private AvatarStorage storage;

    /** 允许的文件类型 */
    private static final List<String> ALLOWED_TYPES =
            Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp");
    /** 单文件上限 2MB */
    private static final long MAX_SIZE = 2L * 1024 * 1024;
    /** 上传频率限制：同一 IP 在 WINDOW 内最多 MAX_UPLOAD_PER_WINDOW 次 */
    private static final int MAX_UPLOAD_PER_WINDOW = 20;
    private static final long WINDOW = 60_000L;
    /** ip -> [窗口起始时间, 次数] */
    private final Map<String, long[]> rateBuckets = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 取客户端 IP 用于限流。
     * 部署在 Nginx 等反向代理之后时，getRemoteAddr() 只会拿到代理的地址，
     * 导致所有用户共用一个额度 —— 此时优先读 X-Forwarded-For 的第一段。
     */
    private String clientIp(jakarta.servlet.http.HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String first = xff.split(",")[0].trim();
            if (!first.isEmpty()) return first;
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }

    /**
     * 注册页需要「先传头像、后注册」，此时还没有 token，故本接口不强制登录；
     * 但做了频率限制，防止被匿名批量灌文件占满磁盘。
     */
    private void checkRate(String ip) {
        long now = System.currentTimeMillis();
        long[] bucket = rateBuckets.computeIfAbsent(ip, k -> new long[]{now, 0});
        synchronized (bucket) {
            if (now - bucket[0] > WINDOW) {   // 窗口过期，重置
                bucket[0] = now;
                bucket[1] = 0;
            }
            if (++bucket[1] > MAX_UPLOAD_PER_WINDOW) {
                throw new BizException(429, "上传太频繁啦，请稍后再试");
            }
        }
        // 简单防膨胀：IP 记录过多时清一次过期桶
        if (rateBuckets.size() > 2000) {
            rateBuckets.entrySet().removeIf(e -> now - e.getValue()[0] > WINDOW);
        }
    }

    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                     jakarta.servlet.http.HttpServletRequest request) {
        // 先做格式/大小校验，再计限流次数：
        // 否则用户传错一次文件就白扣一次配额，连续传错几次会被锁住，体验很差。
        if (file == null || file.isEmpty())
            throw new BizException(400, "请选择要上传的图片");
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase()))
            throw new BizException(400, "仅支持 JPG / PNG / GIF / WEBP 图片");
        if (file.getSize() > MAX_SIZE)
            throw new BizException(400, "图片不能超过 2MB");

        checkRate(clientIp(request));

        String ext = extOf(file.getOriginalFilename(), contentType);
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        Path dir = storage.dir();
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(filename).toFile());
        } catch (Exception e) {
            throw new BizException(500, "头像保存失败：" + e.getMessage());
        }

        String url = AvatarStorage.URL_PREFIX + filename;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("url", url);
        return m;
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> serve(@PathVariable String filename) {
        // 基础校验 + 防目录穿越
        if (filename == null || filename.isBlank() || filename.contains("..") || filename.contains("/") || filename.contains("\\"))
            throw new BizException(400, "非法的文件名");

        Path dir = storage.dir();
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir))
            throw new BizException(400, "非法的文件名");

        Resource resource;
        try {
            resource = new UrlResource(target.toUri());
        } catch (Exception e) {
            throw new BizException(404, "头像不存在");
        }
        if (!resource.exists() || !resource.isReadable())
            throw new BizException(404, "头像不存在");

        MediaType type = mediaTypeOf(extOf(filename, null));
        long length;
        try {
            length = resource.contentLength();
        } catch (Exception e) {
            length = -1;
        }

        // 显式声明 Content-Length：否则返回给浏览器/前端的响应可能被截断（图片显示不全）
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok().contentType(type);
        if (length >= 0) builder.contentLength(length);
        return builder.body(resource);
    }

    private String extOf(String originalName, String contentType) {
        if (originalName != null && originalName.indexOf('.') > 0) {
            String e = originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
            if (e.matches("\\.(jpg|jpeg|png|gif|webp)")) return e;
        }
        if (contentType != null) {
            switch (contentType.toLowerCase()) {
                case "image/png": return ".png";
                case "image/gif": return ".gif";
                case "image/webp": return ".webp";
                default: return ".jpg";
            }
        }
        return ".jpg";
    }

    private MediaType mediaTypeOf(String ext) {
        if (ext == null) return MediaType.APPLICATION_OCTET_STREAM;
        switch (ext.toLowerCase()) {
            case ".jpg":
            case ".jpeg": return MediaType.IMAGE_JPEG;
            case ".png": return MediaType.IMAGE_PNG;
            case ".gif": return MediaType.IMAGE_GIF;
            case ".webp": return new MediaType("image", "webp");
            default: return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
