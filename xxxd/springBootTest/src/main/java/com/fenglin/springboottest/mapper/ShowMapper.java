package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Show;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShowMapper extends BaseMapper<Show> {
}
