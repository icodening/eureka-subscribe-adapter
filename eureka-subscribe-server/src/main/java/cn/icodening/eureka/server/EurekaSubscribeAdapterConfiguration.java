package cn.icodening.eureka.server;

import cn.icodening.eureka.common.AfterInitializationBeanProcessor;
import cn.icodening.eureka.common.ApplicationHashGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.converters.EurekaJacksonCodec;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.registry.InstanceRegistry;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.eureka.server.EurekaServerAutoConfiguration;
import org.springframework.cloud.netflix.eureka.server.EurekaServerConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * @author icodening
 * @date 2022.01.08
 */
@Import(EurekaEventListenerConfiguration.class)
@AutoConfigureAfter(EurekaServerAutoConfiguration.class)
@ConditionalOnBean({EurekaServerConfig.class, InstanceRegistry.class})
@ConditionalOnProperty(prefix = EurekaServerConfigBean.PREFIX, name = "subscribe.enabled", havingValue = "true", matchIfMissing = true)
public class EurekaSubscribeAdapterConfiguration {

    @Bean
    public EurekaDeferredResultStore eurekaDeferredResultStore() {
        return new EurekaDeferredResultStore();
    }

    @Bean
    public EurekaSubscribeController eurekaSubscribeController() {
        return new EurekaSubscribeController();
    }

    @Bean
    public AfterInitializationBeanProcessor<ResourceConfig> resourceConfigProcessor() {
        /*
         * Q: 为什么要修改FEATURE_FILTER_FORWARD_ON_404为true ?
         * A: 由于Eureka依赖的jersey版本太低并不支持异步servlet(虽然对应Servlet已经支持但jersey不支持)，所以需要使用外部的Servlet容器来实现异步处理(一般我们使用的SpringBoot版本都不会太低,这些版本是支持Servlet异步的)，便需要jersey放行我们的请求
         *
         * **********由于Eureka依赖的jersey版本太低并不支持异步servlet，所以使用外部的Servlet容器来实现异步处理，便需要jersey放行我们的请求************
         * 由于Eureka Server使用Filter的方式在SpringBoot内嵌Servlet容器中再嵌套了一个jersey servlet容器
         * /eureka/* 的前缀会被 com.sun.jersey.spi.container.servlet.ServletContainer 作为拦截，并且jersey容器中没有subscribe的接口，最终会出现404
         * 为了共用/eureka/*这个前缀， 这里将其改为FEATURE_FILTER_FORWARD_ON_404属性改为true，可使得请求向后传递到EurekaSubscribeController中
         * */
        return new AfterInitializationBeanProcessor<>(ResourceConfig.class, "jerseyApplication")
                .onBean((rc) -> rc.getFeatures().put(ServletContainer.FEATURE_FILTER_FORWARD_ON_404, true));
    }

    @Bean
    @ConditionalOnMissingBean
    public ApplicationHashGenerator applicationHashGenerator() {
        return ApplicationHashGenerator.DEFAULT;
    }

    @Bean
    @ConditionalOnMissingBean
    public ApplicationHistoryUpdater applicationHistoryUpdater(ApplicationHashGenerator applicationHashGenerator) {
        return new ApplicationHistoryUpdater(applicationHashGenerator);
    }

    /**
     * 针对eureka模型配置序列化
     *
     * @param objectMapper Jackson ObjectMapper
     * @return SmartInitializingSingleton
     */
    @Bean
    public SmartInitializingSingleton eurekaObjectMapperInitializingBean(ObjectMapper objectMapper) {
        return () -> {
            SimpleModule module = new SimpleModule("eureka1.x");
            module.addSerializer(Application.class, new EurekaJacksonCodec.ApplicationSerializer());
            module.addSerializer(InstanceInfo.class, new EurekaJacksonCodec.InstanceInfoSerializer());
            module.addSerializer(DataCenterInfo.class, new EurekaJacksonCodec.DataCenterInfoSerializer());
            module.addSerializer(DataCenterInfo.class, new EurekaJacksonCodec.DataCenterInfoSerializer());
            objectMapper.registerModule(module);
        };
    }
}
