package cn.icodening.eureka.client;

import cn.icodening.eureka.common.ApplicationHashHistory;
import cn.icodening.eureka.common.Constants;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import com.netflix.discovery.shared.transport.jersey.JerseyApplicationClient;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.netflix.discovery.shared.transport.EurekaHttpResponse.anEurekaHttpResponse;

/**
 * 支持订阅Eureka功能的Jersey客户端
 *
 * @author icodening
 * @date 2022.01.09
 */
public class JerseyApplicationSubscribeClient extends JerseyApplicationClient implements EurekaSubscribableHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(JerseyApplicationSubscribeClient.class);

    public JerseyApplicationSubscribeClient(Client jerseyClient, String serviceUrl) {
        this(jerseyClient, serviceUrl, null, null);
    }

    public JerseyApplicationSubscribeClient(Client jerseyClient, String serviceUrl, Map<String, String> additionalHeaders) {
        this(jerseyClient, serviceUrl, additionalHeaders, null);
    }

    public JerseyApplicationSubscribeClient(Client jerseyClient, String serviceUrl, EurekaClientConfig eurekaClientConfig) {
        this(jerseyClient, serviceUrl, null, eurekaClientConfig);
    }

    public JerseyApplicationSubscribeClient(Client jerseyClient, String serviceUrl, Map<String, String> additionalHeaders, EurekaClientConfig eurekaClientConfig) {
        super(jerseyClient, serviceUrl, additionalHeaders);
        int readTimeout = (int) TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);
        int connectTimeout = (int) TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS);
        if (eurekaClientConfig != null) {
            readTimeout = (int) TimeUnit.MILLISECONDS.convert(eurekaClientConfig.getEurekaServerReadTimeoutSeconds(), TimeUnit.SECONDS);
            connectTimeout = (int) TimeUnit.MILLISECONDS.convert(eurekaClientConfig.getEurekaServerConnectTimeoutSeconds(), TimeUnit.SECONDS);
        }
        jerseyClient.setReadTimeout(readTimeout);
        jerseyClient.setConnectTimeout(connectTimeout);
    }

    @Override
    public EurekaHttpResponse<Application> subscribeApplication(String appName) {
        String urlPath = "subscribe/" + appName;
        ClientResponse response = null;
        try {
            WebResource.Builder requestBuilder = jerseyClient.resource(serviceUrl).path(urlPath).getRequestBuilder();
            addExtraHeadersByAppName(appName, requestBuilder);
            response = requestBuilder.accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

            Application application = null;
            if (response.getStatus() == Response.Status.OK.getStatusCode() && response.hasEntity()) {
                application = response.getEntity(Application.class);
            }
            return anEurekaHttpResponse(response.getStatus(), Application.class)
                    .headers(headersOf(response))
                    .entity(application)
                    .build();
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("Jersey HTTP GET {}/{}; statusCode={}", serviceUrl, urlPath, response == null ? "N/A" : response.getStatus());
            }
            if (response != null) {
                response.close();
            }
        }
    }

    protected void addExtraHeadersByAppName(String appName, WebResource.Builder webResource) {
        super.addExtraHeaders(webResource);
        Integer readTimeoutMilliseconds = (Integer) jerseyClient.getProperties().get(ClientConfig.PROPERTY_READ_TIMEOUT);
        webResource.header(Constants.HEADER_READ_TIMEOUT, TimeUnit.SECONDS.convert(readTimeoutMilliseconds, TimeUnit.MILLISECONDS));
        webResource.header(Constants.HEADER_APP_HASH, ApplicationHashHistory.getLastHash(appName.toUpperCase()));
    }

    private static Map<String, String> headersOf(ClientResponse response) {
        MultivaluedMap<String, String> jerseyHeaders = response.getHeaders();
        if (jerseyHeaders == null || jerseyHeaders.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> headers = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : jerseyHeaders.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                headers.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        return headers;
    }
}
