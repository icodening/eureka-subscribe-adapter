package cn.icodening.eureka.ribbon;

import cn.icodening.eureka.client.EurekaSubscribableHttpClient;
import cn.icodening.eureka.client.EurekaSubscribeTransportClientFactory;
import cn.icodening.eureka.client.RetryableEurekaSubscribeHttpClient;
import cn.icodening.eureka.common.ApplicationAware;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.resolver.ClusterResolver;
import com.netflix.discovery.shared.resolver.DefaultEndpoint;
import com.netflix.discovery.shared.resolver.EurekaEndpoint;
import com.netflix.loadbalancer.ServerList;
import com.netflix.loadbalancer.ServerListUpdater;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.netflix.ribbon.RibbonClientName;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author icodening
 * @date 2022.01.09
 */
@ConditionalOnBean({EurekaClientConfig.class, ApplicationInfoManager.class})
public class ServerListAwareConfiguration {

    @RibbonClientName
    private String appName;

    @Autowired
    private EurekaClientConfig eurekaClientConfig;

    @Autowired
    private ApplicationInfoManager applicationInfoManager;

    @Bean
    public ScheduledExecutorService subscribeApplicationExecutor() {
        return new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r);
            thread.setName(appName + "-subscribe-thread");
            return thread;
        });
    }

    @Bean
    public ServerList<?> ribbonServerList() {
        return new ModifiableServerList();
    }

    @Bean
    @DependsOn("ribbonServerList")
    public ServerListUpdater serverListAwareUpdater(@Qualifier("eurekaSubscribableHttpClient") EurekaSubscribableHttpClient eurekaSubscribableHttpClient,
                                                    @Qualifier("subscribeApplicationExecutor") ScheduledExecutorService subscribeApplicationExecutor,
                                                    @Autowired(required = false) List<ApplicationAware> applicationAwareList) {
        return new ServerListAwareUpdater(appName, eurekaSubscribableHttpClient, subscribeApplicationExecutor, applicationAwareList);
    }

    @Bean
    public ClusterResolver<EurekaEndpoint> clusterResolver() {
        final String region = eurekaClientConfig.getRegion();
        String[] availabilityZones = eurekaClientConfig.getAvailabilityZones(region);
        String zone = InstanceInfo.getZone(availabilityZones, applicationInfoManager.getInfo());
        List<String> servicesUrl = eurekaClientConfig.getEurekaServerServiceUrls(zone);
        return new ClusterResolver<EurekaEndpoint>() {

            @Override
            public String getRegion() {
                return region;
            }

            @Override
            public List<EurekaEndpoint> getClusterEndpoints() {
                return servicesUrl.stream()
                        .map(DefaultEndpoint::new)
                        .collect(Collectors.toList());
            }
        };
    }

    @Bean
    public EurekaSubscribableHttpClient eurekaSubscribableHttpClient(@Qualifier("clusterResolver") ClusterResolver<EurekaEndpoint> clusterResolver) {
        return new RetryableEurekaSubscribeHttpClient(appName + "-subscribe-client", clusterResolver, new EurekaSubscribeTransportClientFactory(eurekaClientConfig));
    }

}