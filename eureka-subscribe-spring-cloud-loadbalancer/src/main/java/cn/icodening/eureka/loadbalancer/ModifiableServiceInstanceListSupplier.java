package cn.icodening.eureka.loadbalancer;

import cn.icodening.eureka.common.ApplicationAware;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory.PROPERTY_NAME;

/**
 * @author icodening
 * @date 2022.01.12
 */
public class ModifiableServiceInstanceListSupplier implements ServiceInstanceListSupplier, ApplicationAware {

    private final String serviceId;

    private final DiscoveryClient discoveryClient;

    private final Flux<List<ServiceInstance>> fluxServiceList;

    private volatile List<ServiceInstance> serviceInstanceList;

    public ModifiableServiceInstanceListSupplier(DiscoveryClient discoveryClient, Environment environment) {
        this.discoveryClient = discoveryClient;
        this.serviceId = environment.getProperty(PROPERTY_NAME);
        this.fluxServiceList = Flux.defer(() -> Flux.just(getServiceInstances())).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public Flux<List<ServiceInstance>> get() {
        return fluxServiceList;
    }

    @Override
    public void setApplication(Application application) {
        if (application == null) {
            this.serviceInstanceList = Collections.emptyList();
            return;
        }
        this.serviceInstanceList = convertServiceInstances(application.getInstancesAsIsFromEureka());
    }

    private List<ServiceInstance> convertServiceInstances(List<InstanceInfo> instancesAsIsFromEureka) {
        return instancesAsIsFromEureka.stream()
                .filter(info -> info.getStatus().equals(InstanceInfo.InstanceStatus.UP))
                .filter(info -> !info.getActionType().equals(InstanceInfo.ActionType.DELETED))
                .map(instanceInfo -> {
                    int port = instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE) ? instanceInfo.getSecurePort() : instanceInfo.getPort();
                    return new DefaultServiceInstance(instanceInfo.getId(), instanceInfo.getAppName(), instanceInfo.getHostName(), port, instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE), instanceInfo.getMetadata());
                })
                .collect(Collectors.toList());
    }

    private List<ServiceInstance> getServiceInstances() {
        if (serviceInstanceList == null) {
            synchronized (this) {
                if (serviceInstanceList == null) {
                    serviceInstanceList = discoveryClient.getInstances(this.serviceId);
                }
            }
        }
        return serviceInstanceList;
    }
}
