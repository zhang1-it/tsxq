package com.atguigu.tingshu.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.delay.DelayMsgService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.order.OrderDerate;
import com.atguigu.tingshu.model.order.OrderDetail;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderDerateMapper;
import com.atguigu.tingshu.order.mapper.OrderDetailMapper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private OrderDerateMapper orderDerateMapper;

    @Autowired
    private AccountFeignClient accountFeignClient;
    @Autowired
    private DelayMsgService delayMsgService;

    /**
     * 订单确认
     *
     * @param userId
     * @param tradeVo
     * @return
     */
    @Override
    public OrderInfoVo trade(Long userId, TradeVo tradeVo) {
        OrderInfoVo orderInfoVo = new OrderInfoVo();

        String itemType = tradeVo.getItemType();
        //付款项目类型
        orderInfoVo.setItemType(itemType);
        //定义OrderInfoVo中所需要的信息
        //订单原始金额
        BigDecimal originalAmount = new BigDecimal("0");
        //减免总金额
        BigDecimal derateAmount = new BigDecimal("0");
        //订单总金额
        BigDecimal orderAmount = new BigDecimal("0");
        //订单减免明细列表
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();
        //订单明细列表
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        //
        //会员充值
        if (SystemConstant.ORDER_ITEM_TYPE_VIP.equals(itemType)) {
            Long vipId = tradeVo.getItemId();
            VipServiceConfig vipServiceConfig = userFeignClient.getVipServiceConfig(vipId).getData();
            Assert.notNull(vipServiceConfig, "会员服务配置不存在，会员ID{}", vipId);
            //封装金额信息
            originalAmount = vipServiceConfig.getPrice();
            orderAmount = vipServiceConfig.getDiscountPrice();
            derateAmount = originalAmount.subtract(orderAmount);
            //封装订单明细信息
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(vipServiceConfig.getId());
            orderDetailVo.setItemName(vipServiceConfig.getName());
            orderDetailVo.setItemUrl(vipServiceConfig.getImageUrl());
            orderDetailVo.setItemPrice(orderAmount);
            orderDetailVoList.add(orderDetailVo);
            //封装订单减免明细信息
            OrderDerateVo orderDerateVo = new OrderDerateVo();
            orderDerateVo.setDerateType("1406");
            orderDerateVo.setDerateAmount(derateAmount);
            orderDerateVo.setRemarks("会员服务");
            orderDerateVoList.add(orderDerateVo);

        }
        //专辑充值
        else if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(itemType)) {
            //3.处理订单确认页数据-选择专辑
            //3.1 远程调用"用户服务"-判断当前用户是否重复购买专辑
            Boolean isBuy = userFeignClient.isPaidAlbum(tradeVo.getItemId()).getData();
            if (isBuy) {
                throw new GuiguException(400, "当前用户已购买该专辑！");
            }
            //3.2 远程调用"用户服务"-获取当前用户信息（得到身份）
            UserInfo userInfo = userFeignClient.getUserInfoById(userId).getData();
            Assert.notNull(userInfo, "用户信息为空！");
            Integer isVip = userInfo.getIsVip();
            //3.3 远程调用"专辑服务"-获取欲购买专辑信息
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(tradeVo.getItemId()).getData();
            Assert.notNull(albumInfo, "专辑信息为空");
            //3.4 计算当前用户购买专辑价格
            originalAmount = albumInfo.getPrice();
            orderAmount = originalAmount;
            //3.4.1 判断专辑是否有普通用户折扣
            BigDecimal discount = albumInfo.getDiscount();
            if (discount.intValue() != -1) {
                if (isVip.intValue() == 0) {
                    //普通用户折扣(从0.1-9.9)：原价*折扣   100*8/10 = 80 注意：保留小数位+四舍五入
                    orderAmount = originalAmount.multiply(discount)
                            .divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
                }
                if (isVip.intValue() == 1 && new Date().after(userInfo.getVipExpireTime())) {
                    //普通用户
                    orderAmount = originalAmount.multiply(discount)
                            .divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
                }
            }

            //3.4.2 判断专辑是否有VIP用户折扣
            BigDecimal vipDiscount = albumInfo.getVipDiscount();
            if (vipDiscount.intValue() != -1) {
                if (isVip.intValue() == 1 && new Date().before(userInfo.getVipExpireTime())) {
                    //VIP会员用户
                    orderAmount = originalAmount.multiply(vipDiscount)
                            .divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
                }
            }
            //封装订单细节信息
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
            orderDetailVo.setItemPrice(albumInfo.getPrice());
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemName(albumInfo.getAlbumTitle());
            orderDetailVoList.add(orderDetailVo);
            //封装订单贱减免信息
            OrderDerateVo orderDerateVo = new OrderDerateVo();
            orderDerateVo.setDerateAmount(derateAmount);
            orderDerateVo.setRemarks("专辑服务");
            orderDerateVo.setDerateType("1405");
            orderDerateVoList.add(orderDerateVo);

        }
        //声音充值
        else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(itemType)) {
            //4. 处理订单确认页数据-选择声音
            //4.1 远程调用"专辑服务"-获取待购买声音列表
            List<TrackInfo> waitBuyTrackInfoList = albumFeignClient.getWaitBuyTrackInfoList(tradeVo.getItemId(), tradeVo.getTrackCount()).getData();
            if (CollectionUtil.isEmpty(waitBuyTrackInfoList)) {
                throw new GuiguException(400, "无符合要求声音");
            }
            //4.2 远程调用"专辑服务"获取专辑信息（得到单集价格）
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(waitBuyTrackInfoList.get(0).getAlbumId()).getData();
            BigDecimal price = albumInfo.getPrice();

            //4.3 计算价格 数量*单价 声音没有折扣
            originalAmount = price.multiply(new BigDecimal(waitBuyTrackInfoList.size()));
            orderAmount = originalAmount;

            //4.4 遍历待购买声音列表封装订单明细列表
            orderDetailVoList = waitBuyTrackInfoList.stream()
                    .map(trackInfo -> {
                        OrderDetailVo orderDetailVo = new OrderDetailVo();
                        orderDetailVo.setItemId(trackInfo.getId());
                        orderDetailVo.setItemName(trackInfo.getTrackTitle());
                        orderDetailVo.setItemUrl(trackInfo.getCoverUrl());
                        orderDetailVo.setItemPrice(price);
                        return orderDetailVo;
                    }).collect(Collectors.toList());

        }
//5.所有订单确认都需要属性
        orderInfoVo.setOriginalAmount(originalAmount);
        orderInfoVo.setOrderAmount(orderAmount);
        orderInfoVo.setDerateAmount(derateAmount);
        orderInfoVo.setOrderDetailVoList(orderDetailVoList);
        orderInfoVo.setOrderDerateVoList(orderDerateVoList);
        //5.1 本次结算流水号-防止重复提交
        //5.1.1 构建当前用户本次订单流水号Key
        String tradeNoKey = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        //5.1.2 生成本次订单流水号
        String tradeNo = IdUtil.fastSimpleUUID();
        //5.1.3 将流水号存入Redis
        redisTemplate.opsForValue().set(tradeNoKey, tradeNo, 5, TimeUnit.MINUTES);
        //5.1.4 封装订单VO中流水号
        orderInfoVo.setTradeNo(tradeNo);

        //5.2 本次结算时间戳
        orderInfoVo.setTimestamp(DateUtil.current());
        //5.3 本次结算签名--防止数据篡改
        //5.3.1 将订单VO转为Map-将VO中支付方式null值去掉
        Map<String, Object> paramsMap = BeanUtil.beanToMap(orderInfoVo, false, true);
        //5.3.2 调用签名API对现有订单所有数据进行签名
        String sign = SignHelper.getSign(paramsMap);
        orderInfoVo.setSign(sign);

        //6.返回订单确认页数据VO对象
        return orderInfoVo;
    }

    /**
     * 提交订单
     *
     * @param userId
     * @param orderInfoVo
     * @return
     */
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public Map<String, String> submitOrder(Long userId, OrderInfoVo orderInfoVo) {
        //2 专辑购买

        //2.1 判断专辑订单是否重复提交
        String tradeNoKey = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        //使用lua脚本删除redis中的数据，保证原子性
        String scriptText = "if(redis.call('get', KEYS[1]) == ARGV[1]) " +
                "then " +
                "return redis.call('del', KEYS[1]) " +
                "else " +
                "return 0 end";
        DefaultRedisScript<Boolean> defaultRedisSet = new DefaultRedisScript<>();
        defaultRedisSet.setScriptText(scriptText);
        defaultRedisSet.setResultType(Boolean.class);
        //删除成功没有重复提交
        Boolean flag = (Boolean) redisTemplate.execute(defaultRedisSet, Arrays.asList(tradeNoKey), orderInfoVo.getTradeNo());
        if (!flag) {
            throw new GuiguException(400, "流水号异常！");
        }
        //2.2 校验签名，防止订单被修改
        Map<String, Object> map = BeanUtil.beanToMap(orderInfoVo);
        map.remove("payWay");
        SignHelper.checkSign(map);
        //保存订单
        OrderInfo orderInfo = this.saveOrder(orderInfoVo, userId);
        //余额支付处理
        if (SystemConstant.ORDER_PAY_ACCOUNT.equals(orderInfoVo.getPayWay())) {
            // 4.1 余额支付-远程调用账户服务扣减账户余额
            AccountLockVo accountDeductVo = new AccountLockVo();
            accountDeductVo.setOrderNo(orderInfo.getOrderNo());
            accountDeductVo.setUserId(userId);
            accountDeductVo.setAmount(orderInfo.getOrderAmount());
            accountDeductVo.setContent(orderInfo.getOrderTitle());
            Result deductResult = accountFeignClient.checkAndDeduct(accountDeductVo);
            if (200 != deductResult.getCode()) {
                //扣减余额失败：全局事务都需要回滚
                throw new GuiguException(ResultCodeEnum.ACCOUNT_LESS);
            }
            // 4.2 虚拟物品发货-远程调用用户服务新增购买记录
            UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
            userPaidRecordVo.setOrderNo(orderInfo.getOrderNo());
            userPaidRecordVo.setUserId(userId);
            userPaidRecordVo.setItemType(orderInfo.getItemType());
            List<Long> itemIdList = orderInfoVo.getOrderDetailVoList().stream().map(OrderDetailVo::getItemId).collect(Collectors.toList());
            userPaidRecordVo.setItemIdList(itemIdList);

            Result paidRecordResult = userFeignClient.savePaidRecord(userPaidRecordVo);
            if (200 != paidRecordResult.getCode()) {
                //新增购买记录失败：全局事务都需要回滚
                throw new GuiguException(211, "新增购买记录异常");
            }
            // 4.3 订单状态：已支付
            orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_PAID);
            orderInfoMapper.updateById(orderInfo);
        }


        //5.响应提交成功订单编号
        Map<String, String> mapResult = new HashMap<>();
        mapResult.put("orderNo", orderInfo.getOrderNo());

        //发送延迟消息，确认是否取消订单
        delayMsgService.sendDelayMessage(KafkaConstant.QUEUE_ORDER_CANCEL, orderInfo.getOrderNo(),Long.valueOf(KafkaConstant.DELAY_TIME));

        return mapResult;
    }

    /**
     * 根据订单号获取订单信息
     *
     * @param orderNo
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(String orderNo) {
        //select * from order_info where order_no = ?
        //查询订单详情
        LambdaQueryWrapper<OrderInfo> oilambdaQueryWrapper = new LambdaQueryWrapper<>();
        oilambdaQueryWrapper.eq(OrderInfo::getOrderNo, orderNo);
        OrderInfo orderInfo = orderInfoMapper.selectOne(oilambdaQueryWrapper);
        Assert.notNull(orderInfo, "订单不存在！");
        Long orderId = orderInfo.getId();
        //查询订单明细
        //select * from order_detail where order_id = ?
        LambdaQueryWrapper<OrderDetail> odLambdaQueryWrapper = new LambdaQueryWrapper<>();
        odLambdaQueryWrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(odLambdaQueryWrapper);
        orderInfo.setOrderDetailList(orderDetails);
        //查询订单减免信息
        //select * from order_derate where order_id = ?
        LambdaQueryWrapper<OrderDerate> odrLambdaQueryWrapper = new LambdaQueryWrapper<>();
        odrLambdaQueryWrapper.eq(OrderDerate::getOrderId, orderId);
        List<OrderDerate> orderDerates = orderDerateMapper.selectList(odrLambdaQueryWrapper);
        orderInfo.setOrderDerateList(orderDerates);
        //获取订单状态名字
        String orderStatusName = this.getOrderStatusName(orderInfo.getOrderStatus());
        orderInfo.setOrderStatusName(orderStatusName);
        String payWayName = this.getPayWayName(orderInfo.getPayWay());
        orderInfo.setPayWayName(payWayName);
        return orderInfo;
    }

    /**
     * 获取用户订单分页列表
     * @param iPage
     * @param userId
     * @return
     */
    @Override
    public IPage<OrderInfo> findUserPage(IPage<OrderInfo> iPage, Long userId) {
       Page<OrderInfo> page = orderInfoMapper.selectUserPage(iPage,userId);
        //2.遍历处理订单状态、订单付费方式中文
        page.getRecords().forEach(orderInfo -> {
            orderInfo.setOrderStatusName(getOrderStatusName(orderInfo.getOrderStatus()));
            orderInfo.setPayWayName(getPayWayName(orderInfo.getPayWay()));
        });
        return page;
    }

    /**
     * 查询订单状态，关闭订单
     * @param aLong
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void orderCanncal(String orderNo) {
        LambdaQueryWrapper<OrderInfo> oiLambdaQueryWrapper = new LambdaQueryWrapper<>();
        oiLambdaQueryWrapper.eq(OrderInfo::getOrderNo,orderNo);
        OrderInfo orderInfo = orderInfoMapper.selectOne(oiLambdaQueryWrapper);
        if (orderInfo != null && SystemConstant.ORDER_STATUS_UNPAID.equals(orderInfo.getOrderStatus())) {
            //表示未支付
            orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_CANCEL);
            orderInfoMapper.updateById(orderInfo);
        }
    }

    /**
     * 修改订单支付成功c
     * @param orderNo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void orderPaySuccess(String orderNo) {
        //防重处理
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(orderNo, orderNo, 1, TimeUnit.HOURS);
        if (flag) {
            try {
                QueryWrapper<OrderInfo> orderInfoQueryWrapper = new QueryWrapper<>();
                orderInfoQueryWrapper.eq("order_no", orderNo);
                OrderInfo orderInfo = this.getOrderInfo(orderNo);
                Assert.notNull(orderInfo, "订单不存在,订单号{}", orderNo);
                if (orderInfo.getOrderStatus().equals(SystemConstant.ORDER_STATUS_UNPAID)){
                    orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_PAID);
                    orderInfoMapper.updateById(orderInfo);
                    //3.远程调用“用户服务”新增用户购买记录（VIP，专辑）
                    //3.1 构建用户购买记录VO对象
                    UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
                    userPaidRecordVo.setOrderNo(orderInfo.getOrderNo());
                    userPaidRecordVo.setUserId(orderInfo.getUserId());
                    userPaidRecordVo.setItemType(orderInfo.getItemType());
                    //遍历订单中订单商品明细封装购买项目ID
                    List<Long> itemIdList = orderInfo.getOrderDetailList().stream()
                            .map(OrderDetail::getItemId)
                            .collect(Collectors.toList());
                    userPaidRecordVo.setItemIdList(itemIdList);

                    //3.2 远程调用为用户新增购买记录
                    Result userResult = userFeignClient.savePaidRecord(userPaidRecordVo);
                    if (200 != userResult.getCode()) {
                        throw new GuiguException(500, "新增购买记录异常！");
                    }
                }
            } catch (Exception e) {
                redisTemplate.delete(orderNo);
                e.printStackTrace();
            }
        }
    }

    /**
     * //获取订单状态名字
     *
     * @param orderStatus
     * @return
     */
    private String getOrderStatusName(String orderStatus) {
        if (SystemConstant.ORDER_STATUS_UNPAID.equals(orderStatus)) {
            return "未支付";
        } else if (SystemConstant.ORDER_STATUS_PAID.equals(orderStatus)) {
            return "已支付";
        } else if (SystemConstant.ORDER_STATUS_CANCEL.equals(orderStatus)) {
            return "取消";
        }
        return null;
    }

    /**
     * 根据支付方式编号得到支付名称
     *
     * @param payWay
     * @return
     */
    private String getPayWayName(String payWay) {
        if (SystemConstant.ORDER_PAY_WAY_WEIXIN.equals(payWay)) {
            return "微信";
        } else if (SystemConstant.ORDER_PAY_ACCOUNT.equals(payWay)) {
            return "余额";
        } else if (SystemConstant.ORDER_PAY_WAY_ALIPAY.equals(payWay)) {
            return "支付宝";
        }
        return "";
    }

    /**
     * 保存订单
     *
     * @param orderInfoVo
     * @param userId
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    private OrderInfo saveOrder(OrderInfoVo orderInfoVo, Long userId) {
        OrderInfo orderInfo = BeanUtil.copyProperties(orderInfoVo, OrderInfo.class);
        //1.2 设置用户ID
        orderInfo.setUserId(userId);
        //1.3 为订单设置初始付款状态：未支付
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_UNPAID);
        //1.4 生成全局唯一订单编号 形式：当日日期+雪花算法
        String orderNo = DateUtil.today().replaceAll("-", "") + IdUtil.getSnowflakeNextId();
        orderInfo.setOrderNo(orderNo);
        orderInfo.setOrderTitle(orderInfoVo.getOrderDerateVoList().get(0).getRemarks());
        //1.5 保存订单
        orderInfoMapper.insert(orderInfo);
        Long orderId = orderInfo.getId();

        //2.保存订单商品明细
        List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
        if (CollectionUtil.isNotEmpty(orderDetailVoList)) {
            orderDetailVoList.forEach(orderDetailVo -> {
                OrderDetail orderDetail = BeanUtil.copyProperties(orderDetailVo, OrderDetail.class);
                //关联订单ID
                orderDetail.setOrderId(orderId);
                orderDetailMapper.insert(orderDetail);
            });
        }

        //3.保存订单优惠明细
        List<OrderDerateVo> orderDerateVoList = orderInfoVo.getOrderDerateVoList();
        if (CollectionUtil.isNotEmpty(orderDerateVoList)) {
            orderDerateVoList.forEach(orderDetailVo -> {
                OrderDerate orderDerate = BeanUtil.copyProperties(orderDetailVo, OrderDerate.class);
                //关联订单ID
                orderDerate.setOrderId(orderId);
                orderDerateMapper.insert(orderDerate);
            });
        }
        //4.返回订单对象
        return orderInfo;
    }
}
