package cn.icodening.eureka.ribbon;

import cn.icodening.eureka.common.ApplicationAware;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 可修改的ServerList
 *
 * @author icodening
 * @date 2022.01.11
 */
public class ModifiableServerList implements ApplicationAware, ServerList<DiscoveryEnabledServer> {

    private volatile List<DiscoveryEnabledServer> servers = Collections.emptyList();

    @Override
    public void setApplication(Application application) {
        if (application == null) {
            this.servers = Collections.emptyList();
            return;
        }
        this.servers = application.getInstancesAsIsFromEureka()
                .stream()
                .filter(info -> info.getStatus().equals(InstanceInfo.InstanceStatus.UP))
                .filter(info -> !info.getActionType().equals(InstanceInfo.ActionType.DELETED))
                .map(instanceInfo -> {
                    DiscoveryEnabledServer discoveryEnabledServer = new DiscoveryEnabledServer(instanceInfo, instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE));
                    String zone = Optional.ofNullable(instanceInfo.getMetadata().get("zone")).orElse(Server.UNKNOWN_ZONE);
                    discoveryEnabledServer.setZone(zone);
                    return discoveryEnabledServer;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<DiscoveryEnabledServer> getInitialListOfServers() {
        return servers;
    }

    @Override
    public List<DiscoveryEnabledServer> getUpdatedListOfServers() {
        return servers;
    }
}
