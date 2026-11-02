package com.fenglin.springboottest.common;

import com.fenglin.springboottest.entity.Review;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.ReviewMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 头像文件的磁盘操作工具：统一「上传目录」口径，提供安全删除与孤儿文件清理。
 *
 * 之所以单独抽出来：更换头像 / 删除用户 / 定时清理 都要操作同一目录，
 * 若各处各自拼路径，容易出现路径不一致或删除越界的问题。
 */
@Component
public class AvatarStorage {

    private static final Logger log = LoggerFactory.getLogger(AvatarStorage.class);

    /** URL 前缀：上传头像形如 /api/avatar/{文件名} */
    public static final String URL_PREFIX = "/api/avatar/";

    @Value("${app.upload.dir:./uploads/avatars}")
    private String uploadDir;

    /** 孤儿文件保留时长（小时）：给"上传后没过多久就保存"留出余量 */
    @Value("${app.upload.orphan-hours:24}")
    private long orphanHours;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ReviewMapper reviewMapper;

    /** 上传目录的绝对规范化路径 */
    public Path dir() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * 从头像 URL 中解析出本地文件名；非上传头像（如默认头像 /img/defaults/…）返回 null。
     * 只取最后一段并做安全校验，避免 ../ 之类穿越。
     */
    public String filenameOf(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith(URL_PREFIX)) return null;
        String name = avatarUrl.substring(URL_PREFIX.length());
        if (name.isEmpty() || name.contains("..") || name.contains("/") || name.contains("\\")) return null;
        return name;
    }

    /**
     * 安全删除一个上传头像文件（仅限上传目录内的普通文件名）。
     * 删除失败不抛异常：清理属于「尽力而为」，不能因为它让换头像/删用户失败。
     *
     * @return 是否真的删掉了一个文件
     */
    public boolean deleteByAvatarUrl(String avatarUrl) {
        String name = filenameOf(avatarUrl);
        if (name == null) return false;
        try {
            Path base = dir();
            Path target = base.resolve(name).normalize();
            if (!target.startsWith(base)) return false;   // 再兜一层前缀校验
            return Files.deleteIfExists(target);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 更换头像时清理旧文件：仅当该文件「不再被任何人引用」时才真正删除。
     *
     * 为什么需要这层判断：用户发了带图头像的评价后再换头像，旧文件虽已不是
     * 他的当前头像，但那条历史评价还在用它 —— 直接删会让评价区出现裂图。
     */
    public boolean deleteIfUnreferenced(String avatarUrl) {
        String name = filenameOf(avatarUrl);
        if (name == null) return false;

        // 仍被某个用户当作当前头像 -> 保留
        if (userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getAvatar, avatarUrl)) > 0) {
            return false;
        }
        // 仍被某条评价引用 -> 保留
        if (reviewMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Review>()
                        .eq(Review::getAvatar, avatarUrl)) > 0) {
            return false;
        }
        return deleteByAvatarUrl(avatarUrl);
    }

    /**
     * 清理孤儿文件：上传了但从未被任何用户采用的文件
     * （例如用户在弹层里上传后点了「取消」，或传完直接关掉页面）。
     *
     * 保护策略：
     *  - 只删「文件未被任何用户 avatar 引用」的；
     *  - 只删「修改时间早于 orphanHours」的，避免误删正在编辑中的上传结果。
     */
    public int cleanOrphans() {
        Path base = dir();
        if (!Files.isDirectory(base)) return 0;

        // 1) 收集所有被引用的文件名（用户当前头像 + 历史评价头像）
        Set<String> referenced = new HashSet<>();
        List<User> users = userMapper.selectList(null);
        for (User u : users) {
            String name = filenameOf(u.getAvatar());
            if (name != null) referenced.add(name);
        }
        // 评价里的头像也是一份引用：用户发完评价后再换头像，旧文件仍被这条评价使用，
        // 若不统计就会被当成孤儿删掉，导致评价区头像变成裂图。
        List<Review> reviews = reviewMapper.selectList(null);
        for (Review r : reviews) {
            String name = filenameOf(r.getAvatar());
            if (name != null) referenced.add(name);
        }

        // 2) 扫描目录，删除无人引用且已过期的文件
        long cutoff = System.currentTimeMillis() - orphanHours * 3600_000L;
        int removed = 0;
        try (Stream<Path> files = Files.list(base)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                String name = p.getFileName().toString();
                if (!Files.isRegularFile(p)) continue;
                if (referenced.contains(name)) continue;               // 有人用，保留
                try {
                    if (Files.getLastModifiedTime(p).toMillis() > cutoff) continue;  // 太新，可能是正在编辑
                    if (Files.deleteIfExists(p)) removed++;
                } catch (Exception ignored) {
                    // 单个文件删除失败不影响其它文件
                }
            }
        } catch (Exception e) {
            log.warn("头像孤儿文件清理失败：{}", e.getMessage());
            return removed;
        }

        if (removed > 0) log.info("已清理 {} 个头像孤儿文件", removed);
        return removed;
    }

    /** 每天凌晨 3:10 自动清理一次（cron：秒 分 时 日 月 周） */
    @Scheduled(cron = "0 10 3 * * *")
    public void scheduledClean() {
        cleanOrphans();
    }
}
