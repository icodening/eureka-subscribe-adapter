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
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.apache4.ApacheHttpClient4;

/**
 * @author icodening
 * @date 2022.01.09
 */
public class RetryableEurekaSubscribeHttpClient extends RetryableEurekaHttpClient implements EurekaSubscribableHttpClient {

    private static final int DEFAULT_RETRY_TIME = 3;

    private static final TransportClientFactory subscribeClientFactory = new TransportClientFactory() {
        @Override
        public EurekaHttpClient newClient(EurekaEndpoint serviceUrl) {
            DefaultClientConfig defaultClientConfig = new DefaultClientConfig();
            EurekaCodec codec = new EurekaCodec();
            DiscoveryJerseyProvider discoveryJerseyProvider = new DiscoveryJerseyProvider(codec, codec);
            defaultClientConfig.getSingletons().add(discoveryJerseyProvider);
            ApacheHttpClient4 apacheHttpClient4 = ApacheHttpClient4.create(defaultClientConfig);
            return new JerseyApplicationSubscribeClient(apacheHttpClient4, serviceUrl.getServiceUrl());
        }

        @Override
        public void shutdown() {

        }
    };

    public RetryableEurekaSubscribeHttpClient(String name, ClusterResolver clusterResolver) {
        this(name, clusterResolver, subscribeClientFactory);
    }

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
