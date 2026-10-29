package com.fenglin.springboottest.common;

import com.fenglin.springboottest.entity.Attraction;
import com.fenglin.springboottest.entity.Review;
import com.fenglin.springboottest.mapper.AttractionMapper;
import com.fenglin.springboottest.mapper.ReviewMapper;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.mapper.ProjectMapper;
import com.fenglin.springboottest.entity.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 首次启动且表为空时，初始化种子用户与游乐项目数据 */
@Component
public class DataInitializer implements ApplicationRunner {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AttractionMapper attractionMapper;
    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private ProjectMapper projectMapper;

    @Override
    public void run(ApplicationArguments args) {
        if (userMapper.selectCount(null) == 0) {
            // 熊大 = 管理员权限(0)，熊二 = 默认权限(1)；密码明文存储
            insertUser("熊大", "admin@boonie.com", "bear123456", 0);
            insertUser("熊二", "bear2@boonie.com", "bear123456", 1);
            System.out.println("🌲 已初始化种子用户：熊大(管理员) / 熊二（密码 bear123456，明文）");
        }
        if (attractionMapper.selectCount(null) == 0) {
            seedAttractions();
            System.out.println("🎡 已初始化种子游乐项目与游客评价（6 个项目 / 12 条评价）");
        }
        if (projectMapper.selectCount(null) == 0) {
            seedProjects();
            System.out.println("🗂️ 已初始化后端项目登记表（2 个项目）");
        }
    }

    /** 登记本工作区下的后端/前端项目（与 db-init.sql 种子一致，仅作双保险） */
    private void seedProjects() {
        insertProject("森林游乐园后端", "Spring Boot", "springBootTest", 8080,
                "http://localhost:8080/api/health",
                "主后端服务：登录注册、游乐项目、游客评价、用户权限与后台管理接口（MySQL 持久化）。");
        insertProject("森林登录注册前端", "Vue 3 + Vite", "boonie-auth-pro", 5173,
                "http://127.0.0.1:5173/",
                "前端 SPA：登录注册页、游乐园主页与后台管理系统（开发服务器）。");
    }

    private void insertProject(String name, String type, String dirPath, Integer port,
                              String healthUrl, String desc) {
        Project p = new Project();
        p.setName(name);
        p.setType(type);
        p.setDirPath(dirPath);
        p.setPort(port);
        p.setHealthUrl(healthUrl);
        p.setDescription(desc);
        p.setCreatedAt(LocalDateTime.now());
        projectMapper.insert(p);
    }

    /** permission：1 = 默认权限（普通注册用户），0 = 管理员权限 */
    private void insertUser(String name, String email, String pwd, int permission) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword(pwd);            // 明文存储（按需求，不加密）
        u.setPermission(permission);
        u.setCreatedAt(LocalDateTime.now());
        userMapper.insert(u);
    }

    /** 游乐项目 + 评价种子，字段与前端 src/data/attractions.js 保持一致 */
    private void seedAttractions() {
        // 项目：[emoji, name, description, tag, waitMinutes, color]
        Object[][] rows = {
            {"🎢", "树洞过山车", "穿梭在千年古树之间，俯冲、旋转，尖叫声响彻狗熊岭！全程 880 米，最高时速 78km/h，适合追求刺激的勇敢者。", "刺激", 25, "#e4572e"},
            {"🎡", "星空摩天轮", "升至森林之巅，俯瞰整片狗熊岭的日落与星空。每舱可坐 6 人，缓缓转动 12 分钟一圈，浪漫满分。", "浪漫", 15, "#7c5cbf"},
            {"🚂", "森林小火车", "呜——哐当哐当，跟着吉吉国王环游五大主题区。全程 1.6 公里，沿途有动物演员挥手互动，亲子首选。", "亲子", 10, "#2f7d4f"},
            {"🍯", "蜂蜜漂流", "乘上蜂蜜罐小船，顺着金色溪流来一场甜蜜漂流。全长 420 米，中途有喷泉惊喜，清凉解暑。", "清凉", 30, "#ffb703"},
            {"🏰", "光头强迷宫", "在光头强的小木屋迷宫里寻宝，小心别被发现哦！共 3 层木质迷宫，藏着 12 个惊喜彩蛋。", "益智", 8, "#a9743c"},
            {"💦", "激流勇士", "从瀑布顶端一跃而下，夏日限定的透心凉体验。落差 18 米，尖叫与水花齐飞，记得带雨衣。", "刺激", 40, "#2a9d8f"}
        };
        // 评价：[项目序号(0-based), reviewerName, avatar, rating, reviewDate, content]
        Object[][] reviews = {
            {0, "熊大粉丝", "🐻", 5, "2026-10-08", "太刺激了！熊大熊二就在旁边加油，孩子们玩得不想走。"},
            {0, "森林旅人", "🦊", 4, "2026-10-05", "排队略久但值回票价，建议上午来避开高峰。"},
            {1, "仰望星空的猫", "🐱", 4, "2026-10-07", "傍晚坐上去看日落特别美，就是风有点大。"},
            {1, "情侣小白", "💑", 5, "2026-10-02", "求婚成功！摩天轮顶端的那一刻太浪漫了。"},
            {2, "亲子妈妈", "🐰", 5, "2026-10-03", "超适合带娃，吉吉国王还会挥手，女儿玩得超开心！"},
            {2, "吉吉粉丝", "🐵", 4, "2026-09-28", "环游一圈能看遍全园，性价比很高。"},
            {3, "甜筒熊", "🐻", 4, "2026-10-06", "夏天玩太爽了，就是排队有点久。"},
            {3, "漂流达人", "🌊", 5, "2026-09-30", "蜂蜜罐小船超可爱，拍照出片率很高！"},
            {4, "迷宫王", "🧩", 3, "2026-10-01", "有点绕，小朋友偶尔会急，建议大人陪同。"},
            {4, "寻宝少年", "🗺️", 4, "2026-09-26", "找到彩蛋那一刻超有成就感！"},
            {5, "浪里白条", "🐬", 5, "2026-10-04", "落差超带感，下来全身湿透但超爽！"},
            {5, "夏日控", "☀️", 4, "2026-09-29", "夏天必玩项目，排队久但值得。"}
        };

        // 先插项目，拿回自增 id
        List<Long> ids = new java.util.ArrayList<>();
        for (Object[] r : rows) {
            Attraction a = new Attraction();
            a.setEmoji((String) r[0]);
            a.setName((String) r[1]);
            a.setDescription((String) r[2]);
            a.setTag((String) r[3]);
            a.setWaitMinutes((Integer) r[4]);
            a.setColor((String) r[5]);
            a.setCreatedAt(LocalDateTime.now());
            attractionMapper.insert(a);
            ids.add(a.getId());
        }
        // 再插评价
        for (Object[] rv : reviews) {
            int idx = (Integer) rv[0];
            Review rev = new Review();
            rev.setAttractionId(ids.get(idx));
            rev.setReviewerName((String) rv[1]);
            rev.setAvatar((String) rv[2]);
            rev.setRating(BigDecimal.valueOf(((Number) rv[3]).doubleValue()));
            rev.setReviewDate(LocalDate.parse((String) rv[4]));
            rev.setContent((String) rv[5]);
            rev.setCreatedAt(LocalDateTime.now());
            reviewMapper.insert(rev);
        }
    }
}
