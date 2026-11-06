package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.TicketOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketOrderMapper extends BaseMapper<TicketOrder> {
}
