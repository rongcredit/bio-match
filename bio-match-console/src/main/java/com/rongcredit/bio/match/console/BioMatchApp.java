package com.rongcredit.bio.match.console;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * `bio-match` 控制台应用启动类。
 * <p>
 * 本类作为项目控制台端的统一引导入口，负责启动 Spring Boot 应用上下文、加载运行期配置，
 * 并完成命令组件的注册与装配。应用启动时统一扫描 `com.rongcredit` 包下的业务组件，
 * 同时显式排除数据源自动配置，以适配当前以命令行批处理为主的运行场景。
 * </p>
 */
@SpringBootApplication(scanBasePackages = { "com.rongcredit" }, exclude = { DataSourceAutoConfiguration.class })
public class BioMatchApp {

    /**
     * 应用程序主入口。
     *
     * @param args 启动参数列表
     */
    public static void main(String[] args) {
        SpringApplication.run(BioMatchApp.class, args);
    }

}
