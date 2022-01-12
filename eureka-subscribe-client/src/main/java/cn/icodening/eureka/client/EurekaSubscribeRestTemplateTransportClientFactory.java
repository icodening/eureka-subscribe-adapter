package cn.icodening.eureka.client;

import com.netflix.discovery.shared.resolver.EurekaEndpoint;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import org.springframework.cloud.netflix.eureka.http.RestTemplateTransportClientFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author icodening
 * @date 2022.01.13
 */
public class EurekaSubscribeRestTemplateTransportClientFactory extends RestTemplateTransportClientFactory {

    @Override
    public EurekaHttpClient newClient(EurekaEndpoint serviceUrl) {
        return new RestTemplateEurekaSubscribeHttpClient(restTemplate(serviceUrl.getServiceUrl()),
                serviceUrl.getServiceUrl());
    }

    private RestTemplate restTemplate(String serviceUrl) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            URI serviceURI = new URI(serviceUrl);
            if (serviceURI.getUserInfo() != null) {
                String[] credentials = serviceURI.getUserInfo().split(":");
                if (credentials.length == 2) {
                    restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(
                            credentials[0], credentials[1]));
                }
            }
        } catch (URISyntaxException ignore) {

        }

        restTemplate.getMessageConverters().add(0, mappingJacksonHttpMessageConverter());
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            protected boolean hasError(HttpStatus statusCode) {
                if (statusCode.is4xxClientError()) {
                    return false;
                }
                return super.hasError(statusCode);
            }
        });
        return restTemplate;
    }
}
