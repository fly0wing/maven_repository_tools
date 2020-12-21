package com.github.luckygoldfish.maven.mergerepository;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.Map;
import java.util.Properties;

/**
 * @author zhaotianming1
 * @date 2018/4/12
 */
@SpringBootApplication
//@EnableScheduling
//@EnableCaching
@Slf4j
@EnableAspectJAutoProxy(exposeProxy = true, proxyTargetClass = true)
public class Application {
    public static void main(String[] args) {
        try {
            start(args);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static ConfigurableApplicationContext start(String[] args) {
        printSysProps();

        SpringApplication springApplication = new SpringApplication(Application.class);
        springApplication.setBannerMode(Banner.Mode.OFF);
//        springApplication.setWebApplicationType(WebApplicationType.NONE);
        springApplication.addInitializers((ApplicationContextInitializer<ConfigurableApplicationContext>) applicationContext -> {
            //            if (applicationContext instanceof AbstractRefreshableApplicationContext) {
            //                ((AbstractRefreshableApplicationContext) applicationContext).setAllowBeanDefinitionOverriding(false);
            //            } else if (applicationContext instanceof GenericApplicationContext) {
            //                ((GenericApplicationContext) applicationContext).setAllowBeanDefinitionOverriding(false);
            //            }
        });
        return springApplication.run(args);

    }


    private static void printSysProps() {
        if (log.isDebugEnabled()) {
            Properties properties = System.getProperties();
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                log.debug("System.getProperties: key:[{}],val:[{}]", entry.getKey(), entry.getValue());
            }
        }
    }
}
