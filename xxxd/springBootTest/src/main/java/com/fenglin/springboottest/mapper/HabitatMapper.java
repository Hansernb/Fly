package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Habitat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HabitatMapper extends BaseMapper<Habitat> {
}
