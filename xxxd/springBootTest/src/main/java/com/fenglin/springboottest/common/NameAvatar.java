package com.fenglin.springboottest.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * 按「评价人名字」确定性生成 emoji 头像：
 *   - 同一个名字永远得到同一个头像（跨项目、跨天一致）；
 *   - 不同名字尽量不同（见 {@link #assignUnique}，可做到全站不重复）；
 *   - 纯内存计算，不依赖图片资源，游客评论无需上传。
 */
public final class NameAvatar {

    private NameAvatar() {
    }

    /** 头像池：森林 / 动物主题，避开组合 emoji（ZWJ、变体选择符），确保单字符可安全入库 */
    private static final String[] POOL = {
            "🐻", "🦊", "🐱", "🐰", "🐵", "🐿️", "🦉", "🦌",
            "🐷", "🐸", "🐝", "🦋", "🐢", "🐬", "🐳", "🐹",
            "🦔", "🐨", "🐯", "🦁", "🐘", "🦒", "🐓", "🦜"
    };

    /** 单个名字 → 确定性头像（用于新评论落库时的兜底） */
    public static String emojiFor(String name) {
        return emojiFor(name, null);
    }

    /**
     * 名字 → 确定性头像，且避开已被占用（taken）的头像：
     * 从名字 hash 落位开始，向后线性探到第一个未占用的。
     * 头像池被占满时退化为不避让（同名同头像优先保证）。
     */
    public static String emojiFor(String name, Collection<String> takenAvatars) {
        String n = name == null ? "" : name.trim();
        int h = n.isEmpty() ? 0 : n.hashCode();
        h ^= (h >>> 16);                       // 简单扰频，让相近名字散得更开
        int idx = Math.floorMod(h, POOL.length);
        if (takenAvatars != null) {
            java.util.Set<String> taken = new java.util.HashSet<>(takenAvatars);
            if (taken.size() < POOL.length) {  // 池子没满才避让
                while (taken.contains(POOL[idx])) idx = (idx + 1) % POOL.length;
            }
        }
        return POOL[idx];
    }

    /**
     * 给一批名字分配「互不相同」的头像（用于存量评论回填）。
     * 分配规则确定：名字排序后，先按 hash 落位，被占用则线性向后探到空位。
     * 同名同头像、异名异头像（名字数超过头像池大小时才允许重复）。
     */
    public static Map<String, String> assignUnique(Collection<String> names) {
        Map<String, String> out = new HashMap<>();
        if (names == null || names.isEmpty()) return out;
        List<String> sorted = new ArrayList<>(new TreeSet<>(names));   // 排序保证结果稳定可复现
        boolean[] used = new boolean[POOL.length];
        for (String n : sorted) {
            String key = n == null ? "" : n.trim();
            int idx = Math.floorMod(key.isEmpty() ? 0 : key.hashCode(), POOL.length);
            while (used[idx]) idx = (idx + 1) % POOL.length;
            used[idx] = true;
            out.put(key, POOL[idx]);
        }
        return out;
    }

    /** 是否为「纯 emoji 头像」（即非站内图片路径，可以安全地按名字重生成） */
    public static boolean isPlainEmoji(String avatar) {
        return avatar != null && !avatar.isBlank() && !avatar.startsWith("/");
    }
}
