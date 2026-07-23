package start;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * BigEvent启动类 - Spring Boot应用入口
 */
@SpringBootApplication(scanBasePackages = {"start", "service", "common"})
@MapperScan("mapper")
@EnableTransactionManagement
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableConfigurationProperties
@Slf4j
public class BigEventApplication {
    public static void main(String[] args) {
        SpringApplication.run(BigEventApplication.class, args);
        log.info("BigEvent启动成功");
    }
}