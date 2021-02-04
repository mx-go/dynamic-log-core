package com.github.mx.dynamic.core;

import com.alibaba.nacos.api.utils.StringUtils;
import com.github.mx.dynamic.core.annotation.EnableDynamicLog;
import com.github.mx.nacos.config.core.ConfigFactory;
import com.github.mx.nacos.config.core.RemoteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.LogManager;

/**
 * Create by max on 2020/12/15
 **/
public class DynamicLogSelector implements ImportSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicLogSelector.class);

    /**
     * 配置日志的前缀
     */
    private static final String PREFIX = "logging.level.";
    private static final String SPLIT = ".";
    private static final String ROOT = LoggingSystem.ROOT_LOGGER_NAME;
    private static Set<String> LOGGER_NAMES = new CopyOnWriteArraySet<>();
    private final LoggingSystem loggingSystem;

    public DynamicLogSelector(BeanFactory beanFactory) {
        loggingSystem = beanFactory.getBean(LoggingSystem.class);
    }

    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        // 获取注解的属性集合
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(EnableDynamicLog.class.getName(), false));
        if (attributes != null) {
            String dataId = attributes.getString("dataId");
            if (StringUtils.isBlank(dataId)) {
                LOGGER.error("EnableDynamicLog annotation dataId is empty, please check your config.");
                return new String[0];
            }
            ConfigFactory.getInstance().registerListener(dataId, config -> {
                Properties properties = RemoteConfig.convert(config).getAll();
                // 设置logger的level
                Set<String> currentLoggerNames = setAndGetLoggerNames(properties);
                // 将已移除的logger的level修改为父logger的level
                modifyRemovedLoggerLevel(currentLoggerNames);
                LOGGER_NAMES = currentLoggerNames;
            });
        }
        return new String[0];
    }

    /**
     * 将已移除的logger的level修改为父logger的level
     *
     * @param currentLoggerNames 当前配置的logger
     */
    private void modifyRemovedLoggerLevel(Set<String> currentLoggerNames) {
        LOGGER_NAMES.removeAll(currentLoggerNames);
        LOGGER_NAMES.forEach(loggerName -> {
            LogLevel logLevel = LogLevel.valueOf(getParentLoggerName(loggerName));
            loggingSystem.setLogLevel(loggerName, logLevel);
        });
    }

    /**
     * 设置配置的logger的level，返回当前配置的loggerName
     *
     * @param properties 配置中心的配置
     * @return Set<loggerName>
     */
    private Set<String> setAndGetLoggerNames(Properties properties) {
        Set<String> currentLoggerNames = new HashSet<>();
        properties.forEach((key, val) -> {
            if (org.springframework.util.StringUtils.startsWithIgnoreCase(key.toString(), PREFIX)) {
                String loggerName = PREFIX.equalsIgnoreCase(key.toString()) ? ROOT : key.toString().substring(PREFIX.length());
                setLogLevel(loggerName, val.toString());
                log(loggerName, val.toString());
                currentLoggerNames.add(loggerName);
            }
        });
        return currentLoggerNames;
    }

    /**
     * 修改ROOT logger时需要将子logger同时设置为loglevel
     */
    private void setLogLevel(String loggerName, String level) {
        LogLevel logLevel = LogLevel.valueOf(level.toUpperCase());
        if (!loggerName.equalsIgnoreCase(ROOT)) {
            loggingSystem.setLogLevel(loggerName, logLevel);
            return;
        }
        Enumeration<String> names = LogManager.getLogManager().getLoggerNames();
        while (names.hasMoreElements()) {
            loggingSystem.setLogLevel(names.nextElement(), logLevel);
        }
    }

    private String getParentLoggerName(String loggerName) {
        String parentLoggerName = loggerName.contains(SPLIT) ? loggerName.substring(0, loggerName.lastIndexOf(SPLIT)) : ROOT;
        return loggingSystem.getLoggerConfiguration(parentLoggerName).getConfiguredLevel().name();
    }

    /**
     * 获取当前类的Logger对象有效日志级别对应的方法进行日志输出。举例：
     * 如果当前类的EffectiveLevel为WARN，则获取的Method为'org.slf4j.Logger#warn(java.lang.String, java.lang.Object, java.lang.Object)'
     * 目的是为了输出 'changed {} log level to:{}' 这一行日志
     */
    private void log(String loggerName, String level) {
        try {
            LoggerConfiguration loggerConfiguration = loggingSystem.getLoggerConfiguration(LOGGER.getName());
            Method method = LOGGER.getClass().getMethod(loggerConfiguration.getEffectiveLevel().name().toLowerCase(), String.class, Object.class, Object.class);
            method.invoke(LOGGER, "changed {} log level to:{}", loggerName, level);
        } catch (Exception e) {
            LOGGER.error("Changed {} log level to:{} error", loggerName, level, e);
        }
    }
}
