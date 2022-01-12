package cn.icodening.eureka.client;

import com.netflix.config.DynamicPropertyFactory;
import com.netflix.discovery.provider.DiscoveryJerseyProvider;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.resolver.ClusterResolver;
import com.netflix.discovery.shared.resolver.EurekaEndpoint;
import com.netflix.discovery.shared.transport.*;
import com.netflix.discovery.shared.transport.decorator.RetryableEurekaHttpClient;
import com.netflix.discovery.shared.transport.decorator.ServerStatusEvaluator;
import com.netflix.discovery.shared.transport.decorator.ServerStatusEvaluators;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import com.sun.jersey.client.apache4.config.DefaultApacheHttpClient4Config;

/**
 * @author icodening
 * @date 2022.01.09
 */
public class RetryableEurekaSubscribeHttpClient extends RetryableEurekaHttpClient implements EurekaSubscribableHttpClient {

    private static final int DEFAULT_RETRY_TIME = 3;

    private static final TransportClientFactory subscribeClientFactory = new TransportClientFactory() {
        @Override
        public EurekaHttpClient newClient(EurekaEndpoint serviceUrl) {
            DiscoveryJerseyProvider discoveryJerseyProvider = new DiscoveryJerseyProvider(EurekaCodec.DEFAULT, EurekaCodec.DEFAULT);
            DefaultApacheHttpClient4Config defaultApacheHttpClient4Config = new DefaultApacheHttpClient4Config();
            defaultApacheHttpClient4Config.getSingletons().add(discoveryJerseyProvider);
            ApacheHttpClient4 apacheHttpClient4 = ApacheHttpClient4.create(defaultApacheHttpClient4Config);
            return new JerseyApplicationSubscribeClient(apacheHttpClient4, serviceUrl.getServiceUrl());
        }

        @Override
        public void shutdown() {

        }
    };

    @Deprecated
    public RetryableEurekaSubscribeHttpClient(String name, ClusterResolver clusterResolver) {
        this(name, clusterResolver, subscribeClientFactory);
    }

    @Deprecated
    public RetryableEurekaSubscribeHttpClient(String name, ClusterResolver clusterResolver, TransportClientFactory clientFactory) {
        this(name, new DefaultEurekaTransportConfig(name, DynamicPropertyFactory.getInstance()), clusterResolver, clientFactory);
    }

    public RetryableEurekaSubscribeHttpClient(String name, EurekaTransportConfig transportConfig, ClusterResolver clusterResolver, TransportClientFactory clientFactory) {
        this(name, transportConfig, clusterResolver, clientFactory, ServerStatusEvaluators.httpSuccessEvaluator());
    }

    public RetryableEurekaSubscribeHttpClient(String name, EurekaTransportConfig transportConfig, ClusterResolver clusterResolver, TransportClientFactory clientFactory, ServerStatusEvaluator serverStatusEvaluator) {
        this(name, transportConfig, clusterResolver, clientFactory, serverStatusEvaluator, DEFAULT_RETRY_TIME);
    }

    public RetryableEurekaSubscribeHttpClient(String name, EurekaTransportConfig transportConfig, ClusterResolver clusterResolver, TransportClientFactory clientFactory, ServerStatusEvaluator serverStatusEvaluator, int numberOfRetries) {
        super(name, transportConfig, clusterResolver, clientFactory, serverStatusEvaluator, numberOfRetries);
    }

    @Override
    public EurekaHttpResponse<Application> subscribeApplication(String appName) {
        return execute(new RequestExecutor<Application>() {
            @Override
            public EurekaHttpResponse<Application> execute(EurekaHttpClient delegate) {
                if (!(delegate instanceof EurekaSubscribableHttpClient)) {
                    return delegate.getApplication(appName);
                }
                return ((EurekaSubscribableHttpClient) delegate).subscribeApplication(appName);
            }

            @Override
            public RequestType getRequestType() {
                return RequestType.GetApplication;
            }
        });
    }
}
