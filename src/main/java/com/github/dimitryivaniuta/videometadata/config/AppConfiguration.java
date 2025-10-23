package com.github.dimitryivaniuta.videometadata.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        FxProps.class,
})
@ConfigurationPropertiesScan(basePackages = {
        "com.github.dimitryivaniuta.videometadata.config"
})
@Import({
//        WebClientConfig.class,
//        SchedulingConfig.class
})
public class AppConfiguration {
}
