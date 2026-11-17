package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    /**
     * 根据时间区间统计订单数量
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放从begin到end的日期日期列表
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)) {
            //日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //存放每天的营业额
        List<Double> turnoverList = new ArrayList<>();
        for(LocalDate date : dateList) {
            //查询date日期对应的营业额数据，营业额是指：状态为已完成的订单金额总和
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            //select sum(amount) from order where order_time > ? and order_time < ? and status = 5
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            //当天没有已完成订单时sum返回null，补0避免营业额的值为空
            if (turnover == null) {
                turnover = 0.0;
            }
            turnoverList.add(turnover);

        }


        return TurnoverReportVO
                .builder().dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();

    }
    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */

    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放从begin到end的日期日期列表
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //存放每天的用户总量
        List<Integer> totalUserList = new ArrayList<>();
        //存放每天的新增用户
        List<Integer> newUserList = new ArrayList<>();
        for(LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("end", endTime);
            //总用户数量 select count(id) from user where create_time < ?
            Integer totalUser = orderMapper.countByMap(map);
            map.put("begin", beginTime);
            //新增用户数量 select count(id) from user where create_time > ? and create_time < ?
            Integer newUser = orderMapper.countByMap(map);
            totalUserList.add(totalUser);
            newUserList.add(newUser);

        }
        //封装结果数据
        return UserReportVO
                .builder().dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    /**
     * 根据时间区间统计订单数量
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放从begin到end的日期日期列表
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //每天订单总数集合
        List<Integer> orderCountList = new ArrayList<>();
        //每天有效订单数集合
        List<Integer> validOrderCountList = new ArrayList<>();
        //遍历dateList集合，查询每天的有效订单数和订单总数
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //查询每天的订单总数 select count(id) from orders where order_time>? and order_time<?

            Integer orderCount = getOrderCount(beginTime, endTime, null);

            //查询每天的有效订单数 select count(id) from orders where order_time>? and order_time<? and status=5
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);
            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
                   }
        //计算时间区间内的订单总数
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();

        //计算时间区间内的有效订单总数
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        //计算订单完成率
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount.doubleValue();
        }

        return OrderReportVO
                .builder().dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }
    /**
     * 根据时间区间统计指定状态的订单数量
     * @param beginTime
     * @param endTime
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        Map map = new HashMap();
        map.put("status", status);
        map.put("begin", beginTime);
        map.put("end", endTime);
        return orderMapper.countByStatusMap(map);
    }
}
