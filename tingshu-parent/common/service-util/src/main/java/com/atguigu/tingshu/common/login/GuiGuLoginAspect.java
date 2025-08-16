package com.atguigu.tingshu.common.login;

import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author 张
 * @version 1.0
 */

@Slf4j
@Component
@Aspect
public class GuiGuLoginAspect {
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * * com.atguigu.tingshu.*.api.*.*(..)
     * 匹配com.atguigu.tingshu包及其子包下的所有方法
     * @param joinPoint
     * @param guiGuLogin
     * @return
     */

    //"execution(* com.atguigu.tingshu.*.api.*.*(..)) && @annotation(guiGuLogin)"
    @Around("execution(* com.atguigu.tingshu.*.api.*.*(..))&& @annotation(guiGuLogin)")
    public Object loginAspect(ProceedingJoinPoint joinPoint, GuiGuLogin guiGuLogin){
        Object resultObj;

        //获取用户token
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes servletRequestAttributes=(ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = servletRequestAttributes.getRequest();
        String token = request.getHeader("token");

        //拿到用户信息
        UserInfoVo userInfoVo = (UserInfoVo)redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);

        //登录拦截
        if (guiGuLogin.required() && userInfoVo == null){
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }
        //把用户id存入本地线程
        if (userInfoVo!=null){
            AuthContextHolder.setUserId(userInfoVo.getId());
        }
        //执行方法
        try {
            resultObj = joinPoint.proceed();
        } catch (Throwable e) {
            log.error("执行方法异常：{}",e.getMessage());
            throw new RuntimeException(e);
        }
        //删除用户id避免OOM，避免用户信息泄露
        AuthContextHolder.removeUserId();
        return resultObj;
    }
}
