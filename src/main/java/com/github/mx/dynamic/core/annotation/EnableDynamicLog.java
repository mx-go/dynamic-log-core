package com.github.mx.dynamic.core.annotation;

import com.github.mx.dynamic.core.DynamicLogSelector;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Create by max on 2020/12/15
 **/
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
@Import(DynamicLogSelector.class)
public @interface EnableDynamicLog {
    /**
     * nacos中的dataId
     */
    String dataId();
}
