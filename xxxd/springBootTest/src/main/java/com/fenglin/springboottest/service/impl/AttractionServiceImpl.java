package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.common.NameAvatar;
import com.fenglin.springboottest.entity.Attraction;
import com.fenglin.springboottest.entity.Review;
import com.fenglin.springboottest.mapper.AttractionMapper;
import com.fenglin.springboottest.mapper.ReviewMapper;
import com.fenglin.springboottest.service.AttractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AttractionServiceImpl implements AttractionService {

    @Autowired
    private AttractionMapper attractionMapper;
    @Autowired
    private ReviewMapper reviewMapper;

    @Override
    public List<Attraction> listWithReviews() {
        List<Attraction> list = attractionMapper.selectList(null);
        for (Attraction a : list) {
            // 前台只展示审核通过的评价（pending / rejected 不对外）
            List<Review> reviews = reviewMapper.selectList(
                    new LambdaQueryWrapper<Review>()
                            .eq(Review::getAttractionId, a.getId())
                            .eq(Review::getStatus, "approved")
                            .orderByDesc(Review::getReviewDate));
            a.setReviews(reviews);
            a.setRating(avgRating(reviews));
        }
        return list;
    }

    @Override
    public Review addReview(Long attractionId, String name, String avatar, BigDecimal rating, String content) {
        if (attractionMapper.selectById(attractionId) == null)
            throw new BizException(404, "该项目不存在");
        if (rating == null || rating.compareTo(BigDecimal.ZERO) <= 0 || rating.compareTo(BigDecimal.valueOf(5)) > 0)
            throw new BizException(400, "评分需在 1–5 之间");

        Review r = new Review();
        r.setAttractionId(attractionId);
        r.setReviewerName(name == null || name.isBlank() ? "匿名游客" : name);
        r.setAvatar(avatar == null || avatar.isBlank() ? generateAvatar(name) : avatar);
        r.setRating(rating);
        r.setContent(content);
        r.setStatus("pending");           // 新评价需管理员审核通过后才会展示
        r.setReviewDate(LocalDate.now());
        r.setCreatedAt(LocalDateTime.now());
        reviewMapper.insert(r);
        return r;
    }

    /** 按名字生成头像：确定性 + 避开库里已占用的纯 emoji 头像，做到异名不同头像 */
    @Override
    public String generateAvatar(String reviewerName) {
        Set<String> taken = new HashSet<>();
        for (Review r : reviewMapper.selectList(null)) {
            if (NameAvatar.isPlainEmoji(r.getAvatar())) taken.add(r.getAvatar());
        }
        return NameAvatar.emojiFor(reviewerName, taken);
    }

    private BigDecimal avgRating(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty())
            return BigDecimal.ZERO.setScale(1);
        BigDecimal sum = reviews.stream()
                .map(Review::getRating)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(reviews.size()), 1, RoundingMode.HALF_UP);
    }
}
