package com.agilecontrol.phone;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 生成一个参数为email地址的注解类型Admin
 * @author li.shuhao 
 *
 */
@Admin(mail="li.shuhao@lifecycle.cn")
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Admin {
		String mail() default "";
		
}
