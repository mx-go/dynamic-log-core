## 解决了什么问题

线上日志级别无法做到动态修改，排查问题难。使用该组件后可以通过配置中心动态更改日志级别而无需重启应用，排查问题更简单。

## 使用方法

### 引入Maven坐标

```properties
<dependency>
   <groupId>com.github.mx-go</groupId>
   <artifactId>dynamic-log-core</artifactId>
   <version>1.0.2</version>
</dependency>
```

### 启动类添加注解(例)

```java
@SpringBootApplication
@EnableDynamicLog(dataId = "nacos.properties")
public class SpringBootProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootProviderApplication.class, args);
    }
}
```

> 只需在启动类上面添加注解 @EnableDynamicLog ，其中dataId为 日志配置在nacos中的dataId。

### nacos配置中心配置(例)

```properties
logging.level.root=info
logging.level.com.mx.controller=debug
```

修改日志级别时只需要修改等于(=)后的字符串即可【大小写不敏感】。**注意**：对应的logger需要以 **logger.level.** 开头