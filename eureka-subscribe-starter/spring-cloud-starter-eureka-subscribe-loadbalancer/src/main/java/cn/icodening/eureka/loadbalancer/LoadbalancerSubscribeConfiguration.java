package cn.icodening.eureka.loadbalancer;

import cn.icodening.eureka.common.ApplicationHashGenerator;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.resolver.ClusterResolver;
import com.netflix.discovery.shared.resolver.DefaultEndpoint;
import com.netflix.discovery.shared.resolver.EurekaEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author icodening
 * @date 2022.01.12
 */
@LoadBalancerClients(defaultConfiguration = SubscribeDefaultLoadbalancerConfiguration.class)
public class LoadbalancerSubscribeConfiguration {

    @Autowired(required = false)
    private EurekaClientConfig eurekaClientConfig;

    @Autowired(required = false)
    private ApplicationInfoManager applicationInfoManager;

    @Bean
    @ConditionalOnMissingBean
    public ApplicationHashGenerator defaultApplicationHashGenerator() {
        return ApplicationHashGenerator.DEFAULT;
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
}
