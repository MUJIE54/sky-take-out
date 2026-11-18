package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 自定义定时任务，实现订单状态定时处理
 */
@Component
@Slf4j
public class OrderTask implements ApplicationRunner {
    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理支付超时订单
     */
    @Scheduled(cron = "0 */1 * * * ?")//每分钟触发一次
    public void processTimeoutOrder() {
        log.info("处理支付超时订单: {}", new Date());
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        // select * from orders where status = 1 and order_time < 当前时间-15分钟
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT,time);
        if (ordersList != null && ordersList.size() > 0) {
            ordersList.forEach(order -> {
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("支付超时，自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            });
        }
    }

    /**
     * 处理"派送中"状态的订单
     * 每天晚上22点触发一次，自动完成下单超过1小时仍处于派送中的订单
     */
    @Scheduled(cron = "0 0 22 * * ?")//每天晚上22点触发一次
    public void processDeliveryOrder() {
        log.info("处理派送中订单：{}", new Date());
        // select * from orders where status = 4 and order_time < 当前时间-1小时
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS,time);
        if (ordersList != null && ordersList.size() > 0) {
            ordersList.forEach(order -> {
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            });
        }
    }

    /**
     * 项目启动时立即执行一次派送中订单处理，
     * 防止错过定时触发时间导致积压的订单永远无法被处理
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("启动时处理一次派送中订单");
        processDeliveryOrder();
    }
}
