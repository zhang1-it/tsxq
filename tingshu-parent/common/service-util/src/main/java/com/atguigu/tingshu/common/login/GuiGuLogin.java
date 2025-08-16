package com.atguigu.tingshu.common.login;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author 张
 * @version 1.0
 */

/**
 * 登录校验
 */
@Target(ElementType.METHOD)
@Retention(RUNTIME)
public @interface GuiGuLogin {
    /**
     * true  登录验证
     * false 登录不验证
     * @return
     */

    boolean required() default true;
}
