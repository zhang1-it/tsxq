package com.atguigu.tingshu.payment.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.payment.service.WxPayService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "微信支付接口")
@RestController
@RequestMapping("api/payment")
@Slf4j
public class WxPayApiController {

    @Autowired
    private WxPayService wxPayService;
    /**
     * 微信异步回调
     * api/payment/wxPay/notify
     */
    @PostMapping("wxPay/notify")
    public Map<String,String> wxNotify(HttpServletRequest request) {
        return wxPayService.wxNotify(request);
    }

    /**
     * 小程序轮询查询支付结果-根据商户订单编号查询交易状态
     *
     * @param orderNo
     * @return
     */
    @GetMapping("/wxPay/queryPayStatus/{orderNo}")
    public Result<Boolean> queryPayStatus(@PathVariable String orderNo) {
        Boolean isPay = wxPayService.queryPayStatus(orderNo);
        return Result.ok(isPay);
    }
    /**
     * 微信确认下单
     * /api/payment/wxPay/createJsapi/{paymentType}/{orderNo}
     */
    @PostMapping("wxPay/createJsapi/{paymentType}/{orderNo}")
    @GuiGuLogin
    public Result<Map<String,String>> createJsapi(@PathVariable String paymentType,@PathVariable String orderNo) {
        Map<String, String> resultMap = wxPayService.createJsapi(paymentType, orderNo);
        return Result.ok(resultMap);
    }
}
