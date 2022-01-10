package cn.icodening.eureka.ribbon;

import cn.icodening.eureka.common.ApplicationHashGenerator;
import cn.icodening.eureka.common.DefaultApplicationHashGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.context.annotation.Bean;

/**
 * @author icodening
 * @date 2022.01.09
 */
@RibbonClients(defaultConfiguration = ServerListAwareConfiguration.class)
@ConditionalOnProperty(prefix = "ribbon.eureka.subscribe", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RibbonSubscribeConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ApplicationHashGenerator defaultApplicationHashGenerator() {
        return DefaultApplicationHashGenerator.DEFAULT;
    }
}
