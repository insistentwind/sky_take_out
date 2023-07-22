package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import jdk.nashorn.internal.codegen.types.NumericType;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        //当前集合用于存放从begin到end范围内每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        List<Double> amountList = new ArrayList<>();
        //日期计算，指定日期的后一天对应的日期添加到集合中
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        for (LocalDate date : dateList) {
            //查询date日期对应的营业额数据，营业是指状态为已完成的订单金额合计

            //要体现时分秒
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            //select sum(amount) from orders where order_time > ? and order_time < ? and status = ?
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);

            turnover = turnover == null ? 0.0 : turnover;

            amountList.add(turnover);

        }
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(amountList, ","))
                .build();
    }

    /**
     * 用户数量统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        //存放每天的用户量
        List<Integer> totalUserList = new ArrayList<>();
        //存放每天新增的用户数量
        List<Integer> newUserList = new ArrayList<>();
        //把时间添加到对应集合中
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        for (LocalDate localDate : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            Map map = new HashMap();
            map.put("end", endTime);
            //总用户数量
            Integer totalUser = userMapper.countByMap(map);
            //新增用户数量
            map.put("begin", beginTime);
            Integer newUser = userMapper.countByMap(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }

        //select count(id) from user where create_time < ? and create_time > ?

        //select count(id）from user where create_time < ?

        //封装
        UserReportVO build = UserReportVO.builder()
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .dateList(StringUtils.join(dateList, ","))
                .build();

        return build;
    }

    /**
     * 订单数量统计
     *
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        //日期天数，每天
        List<LocalDate> dateList = new ArrayList<>();
        //每日订单数量
        List<Integer> orderCountList = new ArrayList<>();
        //每日有效的订单数量
        List<Integer> validOrderCountList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        for (LocalDate localDate : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            Map map = new HashMap();

            map.put("begin", beginTime);
            map.put("end", endTime);
            System.out.println("下面是所有的订餐数量:");
            orderCountList.add(orderMapper.countByMap(map));

            map.put("status", Orders.COMPLETED);
            System.out.println("下面是已完成的订餐数量");
            validOrderCountList.add(orderMapper.countByMap(map));

        }
        //计算时间区间内的订单总数量
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();

        //计算时间区间内的有效订单数量
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        //计算百分数
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }


        OrderReportVO orderReportVO = OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();

        System.out.println(orderReportVO);
        return orderReportVO;
    }

    /**
     * 统计指定时间区间内的销量前十
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        List<String> nameList = new ArrayList<>();

        List<Integer> numberList = new ArrayList<>();


        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);


        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

//        for (GoodsSalesDTO goodsSalesDTO : salesTop10) {
//            numberList.add(goodsSalesDTO.getNumber());
//            nameList.add(goodsSalesDTO.getName());
//        }

        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());

        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());


        SalesTop10ReportVO build = SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(names, ","))
                .numberList(StringUtils.join(numbers, ","))
                .build();
        //select od.name,sum(od.number) from order_detail od,orders o where od.order_id = o.id

        //select od.name,sum(od.number) from order_detail od left join orders on (od.order_id = o.id)
        // where o.status = 5 and o.order_time > ? and o.order_time < ?

        return build;
    }
}
