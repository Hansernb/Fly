package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.entity.Member;
import com.fenglin.springboottest.entity.PointLog;
import com.fenglin.springboottest.entity.TicketOrder;
import com.fenglin.springboottest.mapper.MemberMapper;
import com.fenglin.springboottest.mapper.PointLogMapper;
import com.fenglin.springboottest.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MemberServiceImpl implements MemberService {

    /** 抵扣：1 积分 = 0.3 元 */
    private static final BigDecimal YUAN_PER_POINT = new BigDecimal("0.3");
    /** 单笔最多抵扣订单金额的 50% */
    private static final BigDecimal MAX_RATIO = new BigDecimal("0.5");
    /** 白银 / 黄金 等级门槛（累计积分） */
    private static final int SILVER_AT = 500;
    private static final int GOLD_AT = 2000;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private PointLogMapper pointLogMapper;

    @Override
    public Member findByPhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        return memberMapper.selectOne(new LambdaQueryWrapper<Member>()
                .eq(Member::getPhone, phone.trim()));
    }

    @Override
    public Member findByUser(Long userId) {
        if (userId == null) return null;
        return memberMapper.selectOne(new LambdaQueryWrapper<Member>()
                .eq(Member::getUserId, userId)
                .orderByAsc(Member::getId)
                .last("LIMIT 1"));
    }

    @Override
    public Member getOrCreate(String phone, String nickname) {
        String p = phone == null ? "" : phone.trim();
        Member m = findByPhone(p);
        String name = nickname == null ? "" : nickname.trim();
        if (m != null) {
            // 同一手机号再次下单：只补齐「默认昵称」，不覆盖用户自己改过的昵称
            String cur = m.getNickname();
            boolean isDefault = cur == null || cur.isBlank() || cur.startsWith("熊粉");
            if (!name.isEmpty() && isDefault && !name.equals(cur)) {
                m.setNickname(name);
                m.setUpdatedAt(LocalDateTime.now());
                memberMapper.updateById(m);
            }
            return m;
        }
        Member created = new Member();
        created.setPhone(p);
        created.setNickname(name.isEmpty() ? defaultNickname(p) : name);
        created.setAvatar("🐻");
        created.setTotalPoints(0);
        created.setCumPoints(0);
        created.setLevelName(levelOf(0));
        created.setCreatedAt(LocalDateTime.now());
        created.setUpdatedAt(LocalDateTime.now());
        memberMapper.insert(created);
        return created;
    }

    @Override
    public Member getOrCreateForUser(Long userId, String phone, String nickname) {
        // 未登录（游客）→ 退回手机号建档
        if (userId == null) return getOrCreate(phone, nickname);

        String p = phone == null ? "" : phone.trim();
        String name = nickname == null ? "" : nickname.trim();

        // 1) 账号已绑定会员 → 直接用（积分跟账号走，换手机号买票也加到同一个号）
        Member m = findByUser(userId);
        if (m != null) {
            boolean dirty = false;
            // 记录最近一次下单手机号，方便会员卡展示（不覆盖已有值）
            if (m.getPhone() == null || m.getPhone().isBlank()) {
                Member owner = p.isEmpty() ? null : findByPhone(p);
                if (owner == null) {
                    m.setPhone(p.isEmpty() ? null : p);
                    dirty = true;
                }
            }
            String cur = m.getNickname();
            boolean isDefault = cur == null || cur.isBlank() || cur.startsWith("熊粉");
            if (!name.isEmpty() && isDefault && !name.equals(cur)) {
                m.setNickname(name);
                dirty = true;
            }
            if (dirty) {
                m.setUpdatedAt(LocalDateTime.now());
                memberMapper.updateById(m);
            }
            return m;
        }

        // 2) 该手机号此前以游客身份建过档且没绑账号 → 归并到当前账号
        Member byPhone = p.isEmpty() ? null : findByPhone(p);
        if (byPhone != null && byPhone.getUserId() == null) {
            byPhone.setUserId(userId);
            if (byPhone.getNickname() == null || byPhone.getNickname().isBlank()) {
                byPhone.setNickname(name.isEmpty() ? defaultNickname(p) : name);
            }
            byPhone.setUpdatedAt(LocalDateTime.now());
            memberMapper.updateById(byPhone);
            return byPhone;
        }

        // 3) 全新账号 → 建档并绑定账号；手机号若已被别的会员占用则留空（避免唯一键冲突）
        Member created = new Member();
        created.setUserId(userId);
        created.setPhone(byPhone == null && !p.isEmpty() ? p : null);
        created.setNickname(name.isEmpty() ? defaultNickname(p.isEmpty() ? "" : p, userId) : name);
        created.setAvatar("🐻");
        created.setTotalPoints(0);
        created.setCumPoints(0);
        created.setLevelName(levelOf(0));
        created.setCreatedAt(LocalDateTime.now());
        created.setUpdatedAt(LocalDateTime.now());
        memberMapper.insert(created);
        return created;
    }

    @Override
    public Member updateProfile(String phone, String nickname, String avatar) {
        Member m = findByPhone(phone);
        if (m == null) return null;
        boolean dirty = false;
        if (nickname != null && !nickname.isBlank() && !nickname.trim().equals(m.getNickname())) {
            m.setNickname(nickname.trim());
            dirty = true;
        }
        if (avatar != null && !avatar.isBlank() && !avatar.trim().equals(m.getAvatar())) {
            m.setAvatar(avatar.trim());
            dirty = true;
        }
        if (dirty) {
            m.setUpdatedAt(LocalDateTime.now());
            memberMapper.updateById(m);
        }
        return m;
    }

    @Override
    public Member updateProfileById(Long memberId, String nickname, String avatar) {
        if (memberId == null) return null;
        Member m = memberMapper.selectById(memberId);
        if (m == null) return null;
        boolean dirty = false;
        if (nickname != null && !nickname.isBlank() && !nickname.trim().equals(m.getNickname())) {
            m.setNickname(nickname.trim());
            dirty = true;
        }
        if (avatar != null && !avatar.isBlank() && !avatar.trim().equals(m.getAvatar())) {
            m.setAvatar(avatar.trim());
            dirty = true;
        }
        if (dirty) {
            m.setUpdatedAt(LocalDateTime.now());
            memberMapper.updateById(m);
        }
        return m;
    }

    @Override
    public int maxDeductible(Member member, BigDecimal amount) {
        if (member == null || amount == null || amount.signum() <= 0) return 0;
        int points = member.getTotalPoints() == null ? 0 : member.getTotalPoints();
        if (points <= 0) return 0;
        // 金额 × 50% ÷ 0.3，向下取整为可抵扣积分数
        int byRatio = amount.multiply(MAX_RATIO)
                .divide(YUAN_PER_POINT, 0, RoundingMode.FLOOR).intValue();
        return Math.max(0, Math.min(points, byRatio));
    }

    @Override
    public BigDecimal discountOf(int points) {
        if (points <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(points).multiply(YUAN_PER_POINT)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public int earnedOf(BigDecimal paid) {
        if (paid == null || paid.signum() <= 0) return 0;
        return paid.setScale(0, RoundingMode.FLOOR).intValue();
    }

    @Override
    public Member record(Member member, int delta, String logType, String orderNo, String remark) {
        // 只有「获取」才涨累计积分；「抵扣」是花积分，不改变累计值
        int cumDelta = delta > 0 && "获取".equals(logType) ? delta : 0;
        return apply(member, delta, logType, orderNo, remark, cumDelta);
    }

    @Override
    public void rollback(TicketOrder order) {
        if (order == null || order.getMemberId() == null) return;
        Member m = memberMapper.selectById(order.getMemberId());
        if (m == null) return;
        int used = order.getPointsUsed() == null ? 0 : order.getPointsUsed();
        int earned = order.getPointsEarned() == null ? 0 : order.getPointsEarned();
        // 退回抵扣积分：只回余额，不动累计积分（那部分不是「赚到的」）
        if (used > 0) apply(m, used, "退款", order.getOrderNo(), "取消订单退回抵扣积分", 0);
        // 扣回赠送积分：余额与累计积分一起扣回，等级随之回落（防刷单刷等级）
        if (earned > 0) apply(m, -earned, "扣回", order.getOrderNo(), "取消订单扣回赠送积分", -earned);
    }

    @Override
    public List<PointLog> logs(Long memberId, int limit) {
        if (memberId == null) return new ArrayList<>();
        return pointLogMapper.selectList(new LambdaQueryWrapper<PointLog>()
                .eq(PointLog::getMemberId, memberId)
                .orderByDesc(PointLog::getId)
                .last("LIMIT " + Math.max(1, limit)));
    }

    @Override
    public String levelOf(int cumPoints) {
        if (cumPoints >= GOLD_AT) return "黄金会员";
        if (cumPoints >= SILVER_AT) return "白银会员";
        return "普通会员";
    }

    @Override
    public Map<String, Object> toMap(Member m) {
        if (m == null) return null;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", m.getId());
        out.put("userId", m.getUserId());
        out.put("phone", m.getPhone());
        out.put("nickname", m.getNickname());
        out.put("avatar", m.getAvatar() == null ? "🐻" : m.getAvatar());
        out.put("totalPoints", m.getTotalPoints() == null ? 0 : m.getTotalPoints());
        out.put("cumPoints", m.getCumPoints() == null ? 0 : m.getCumPoints());
        out.put("levelName", m.getLevelName() == null ? levelOf(0) : m.getLevelName());
        out.put("createdAt", m.getCreatedAt() == null ? "" : m.getCreatedAt().format(STAMP));
        return out;
    }

    @Override
    public Map<String, Object> logMap(PointLog log) {
        if (log == null) return null;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", log.getId());
        out.put("type", log.getLogType());
        out.put("points", log.getPoints() == null ? 0 : log.getPoints());
        out.put("balance", log.getBalance() == null ? 0 : log.getBalance());
        out.put("orderNo", log.getOrderNo() == null ? "" : log.getOrderNo());
        out.put("remark", log.getRemark() == null ? "" : log.getRemark());
        out.put("createdAt", log.getCreatedAt() == null ? "" : log.getCreatedAt().format(STAMP));
        return out;
    }

    /* ===================== 内部实现 ===================== */

    /**
     * 落库一次积分变动。
     *
     * @param cumDelta 累计积分的变动量（获取=+delta、扣回=-delta、抵扣/退款=0）
     */
    private Member apply(Member member, int delta, String logType, String orderNo,
                         String remark, int cumDelta) {
        if (member == null || member.getId() == null || delta == 0) return member;
        Member db = memberMapper.selectById(member.getId());
        if (db == null) return member;

        int total = db.getTotalPoints() == null ? 0 : db.getTotalPoints();
        int cum = db.getCumPoints() == null ? 0 : db.getCumPoints();
        total = Math.max(0, total + delta);
        if (cumDelta != 0) cum = Math.max(0, cum + cumDelta);
        String level = levelOf(cum);

        db.setTotalPoints(total);
        db.setCumPoints(cum);
        db.setLevelName(level);
        db.setUpdatedAt(LocalDateTime.now());
        memberMapper.updateById(db);

        PointLog log = new PointLog();
        log.setMemberId(db.getId());
        log.setLogType(logType);
        log.setPoints(delta);
        log.setBalance(total);
        log.setOrderNo(orderNo);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        pointLogMapper.insert(log);

        // 回写调用方持有的对象，避免后续还用旧余额计算
        member.setTotalPoints(total);
        member.setCumPoints(cum);
        member.setLevelName(level);
        return db;
    }

    private String defaultNickname(String phone) {
        String tail = phone != null && phone.length() >= 4
                ? phone.substring(phone.length() - 4) : "0000";
        return "熊粉" + tail;
    }

    /** 账号会员没有手机号时，用账号 id 后四位生成默认昵称 */
    private String defaultNickname(String phone, Long userId) {
        if (phone != null && phone.length() >= 4) return defaultNickname(phone);
        long tail = userId == null ? 0 : Math.abs(userId % 10000);
        return String.format("熊粉%04d", tail);
    }
}
