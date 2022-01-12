package cn.icodening.eureka.loadbalancer;

import cn.icodening.eureka.client.EurekaSubscribableHttpClient;
import cn.icodening.eureka.client.EurekaSubscribeTransportClientFactory;
import cn.icodening.eureka.client.RetryableEurekaSubscribeHttpClient;
import cn.icodening.eureka.common.ApplicationAware;
import cn.icodening.eureka.common.ApplicationHashGenerator;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.resolver.ClusterResolver;
import com.netflix.discovery.shared.resolver.EurekaEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ConditionalOnBlockingDiscoveryEnabled;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory.PROPERTY_NAME;

/**
 * @author icodening
 * @date 2022.01.12
 */
@ConditionalOnBlockingDiscoveryEnabled
public class SubscribeDefaultLoadbalancerConfiguration {

    @Autowired(required = false)
    private EurekaClientConfig eurekaClientConfig;

    @Bean
    @ConditionalOnBean(DiscoveryClient.class)
    @ConditionalOnMissingBean
    public ServiceInstanceListSupplier subscribeServiceInstanceListSupplier(ConfigurableApplicationContext context, Environment environment) {
        DiscoveryClient discoveryClient = context.getBean(DiscoveryClient.class);
        return new ModifiableServiceInstanceListSupplier(discoveryClient, environment);
    }

    @Bean
    public EurekaSubscribableHttpClient eurekaSubscribableHttpClient(@Qualifier("clusterResolver") ClusterResolver<EurekaEndpoint> clusterResolver, Environment environment) {
        String appName = environment.getProperty(PROPERTY_NAME);
        return new RetryableEurekaSubscribeHttpClient(appName + "-subscribe-client", eurekaClientConfig.getTransportConfig(), clusterResolver, new EurekaSubscribeTransportClientFactory(eurekaClientConfig));
    }

    @Bean
    @DependsOn("subscribeServiceInstanceListSupplier")
    public ServiceInstanceListAwareUpdater serviceInstanceListAwareUpdater(Environment environment,
                                                                           EurekaSubscribableHttpClient eurekaSubscribableHttpClient,
                                                                           @Autowired(required = false) List<ApplicationAware> applicationAwareList,
                                                                           ApplicationHashGenerator defaultApplicationHashGenerator) {
        String appName = environment.getProperty(PROPERTY_NAME);
        ScheduledThreadPoolExecutor subscribeApplicationExecutor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r);
            thread.setName(appName + "-subscribe-thread");
            return thread;
        });
        ServiceInstanceListAwareUpdater updater = new ServiceInstanceListAwareUpdater(appName, eurekaSubscribableHttpClient, subscribeApplicationExecutor);
        updater.setApplicationAwareList(applicationAwareList);
        updater.setApplicationHashGenerator(defaultApplicationHashGenerator);
        updater.setEurekaClientConfig(eurekaClientConfig);
        updater.start();
        return updater;
    }
}
