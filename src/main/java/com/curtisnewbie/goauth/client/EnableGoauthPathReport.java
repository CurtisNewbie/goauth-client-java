package com.curtisnewbie.goauth.client;

import org.springframework.context.annotation.*;

import java.lang.annotation.*;

/**
 * Enable Goauth Path Report, REST paths are automatically scanned and reported to goauth service
 *
 * @author yongj.zhuang
 */
@Documented
@Import({RestPathScanner.class, RestPathReporter.class})
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableGoauthPathReport {

}
