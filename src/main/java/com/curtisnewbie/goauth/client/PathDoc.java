package com.curtisnewbie.goauth.client;

import java.lang.annotation.*;

/**
 * Path Documentation
 *
 * @author yongj.zhuang
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PathDoc {

    String description() default "";

    PathType type() default PathType.PROTECTED;

    String resourceCode() default "";

    String resourceName() default "";

}
