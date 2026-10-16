package com.fenglin.springboottest.service;

import com.fenglin.springboottest.entity.Attraction;
import com.fenglin.springboottest.entity.Review;

import java.math.BigDecimal;
import java.util.List;

public interface AttractionService {

    /** 查询全部游乐项目，并填充各自评价、计算综合评分 */
    List<Attraction> listWithReviews();

    /** 为某项目新增一条评价，返回写入后的评价（含自增 id 与日期） */
    Review addReview(Long attractionId, String name, String avatar, BigDecimal rating, String content);
}
