package cn.icodening.eureka.client;

import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.provider.DiscoveryJerseyProvider;
import com.netflix.discovery.shared.resolver.EurekaEndpoint;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.TransportClientFactory;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.apache4.ApacheHttpClient4;

import java.util.Hashtable;
import java.util.Map;

/**
 * @author icodening
 * @date 2022.01.09
 */
public class EurekaSubscribeTransportClientFactory implements TransportClientFactory {

    private final EurekaClientConfig eurekaClientConfig;

    public EurekaSubscribeTransportClientFactory(EurekaClientConfig eurekaClientConfig) {
        this.eurekaClientConfig = eurekaClientConfig;
    }

    @Override
    public EurekaHttpClient newClient(EurekaEndpoint serviceUrl) {
        DiscoveryJerseyProvider discoveryJerseyProvider = new DiscoveryJerseyProvider(EurekaCodec.DEFAULT, EurekaCodec.DEFAULT);
        Map<String, String> additionalHeaders = new Hashtable<>();
        DefaultClientConfig defaultClientConfig = new DefaultClientConfig();
        defaultClientConfig.getSingletons().add(discoveryJerseyProvider);
        ApacheHttpClient4 apacheHttpClient4 = ApacheHttpClient4.create(defaultClientConfig);
        return new JerseyApplicationSubscribeClient(apacheHttpClient4, serviceUrl.getServiceUrl(), additionalHeaders, eurekaClientConfig);
    }

    @Override
    public void shutdown() {
    }
}
