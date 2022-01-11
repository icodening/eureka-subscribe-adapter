package cn.icodening.eureka.ribbon;

import cn.icodening.eureka.common.ApplicationAware;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.loadbalancer.AbstractServerList;
import com.netflix.loadbalancer.Server;
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
public class ModifiableServerList extends AbstractServerList<DiscoveryEnabledServer> implements ApplicationAware {

    private volatile List<DiscoveryEnabledServer> servers;

    private final EurekaClient eurekaClient;

    private IClientConfig clientConfig;

    private String appName;

    private String targetRegion = null;

    private boolean isSecure = false;

    public ModifiableServerList(EurekaClient eurekaClient) {
        this(eurekaClient, new DefaultClientConfigImpl());
    }

    public ModifiableServerList(EurekaClient eurekaClient, IClientConfig clientConfig) {
        this.eurekaClient = eurekaClient;
        this.clientConfig = clientConfig;
    }

    @Override
    public void setApplication(Application application) {
        if (application == null) {
            this.servers = Collections.emptyList();
            return;
        }
        this.servers = convertDiscoveryEnabledServers(application.getInstancesAsIsFromEureka());
    }

    @Override
    public List<DiscoveryEnabledServer> getInitialListOfServers() {
        return getServers();
    }

    @Override
    public List<DiscoveryEnabledServer> getUpdatedListOfServers() {
        return getServers();
    }

    @Override
    public void initWithNiwsConfig(IClientConfig clientConfig) {
        if (clientConfig == null) {
            return;
        }
        this.clientConfig = clientConfig;
        this.appName = clientConfig.getClientName();
        this.targetRegion = (String) clientConfig.getProperty(CommonClientConfigKey.TargetRegion);
        this.isSecure = Boolean.parseBoolean("" + clientConfig.getProperty(CommonClientConfigKey.IsSecure, "false"));

        if (this.servers == null) {
            //force fetch
            this.servers = initServers();
        }

    }

    private List<DiscoveryEnabledServer> getServers() {
        if (servers == null) {
            servers = initServers();
        }
        return servers;
    }

    private List<DiscoveryEnabledServer> initServers() {
        List<InstanceInfo> instancesByVipAddress = eurekaClient.getInstancesByVipAddress(appName, isSecure, targetRegion);
        if (instancesByVipAddress == null) {
            return Collections.emptyList();
        }
        return convertDiscoveryEnabledServers(instancesByVipAddress);
    }

    private List<DiscoveryEnabledServer> convertDiscoveryEnabledServers(List<InstanceInfo> instancesAsIsFromEureka) {
        return instancesAsIsFromEureka.stream()
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
}
