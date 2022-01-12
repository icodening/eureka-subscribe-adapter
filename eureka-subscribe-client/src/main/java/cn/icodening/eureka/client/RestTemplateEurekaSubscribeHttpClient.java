package cn.icodening.eureka.client;

import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import org.springframework.cloud.netflix.eureka.http.RestTemplateEurekaHttpClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.netflix.discovery.shared.transport.EurekaHttpResponse.anEurekaHttpResponse;

/**
 * @author icodening
 * @date 2022.01.12
 */
public class RestTemplateEurekaSubscribeHttpClient extends RestTemplateEurekaHttpClient implements EurekaSubscribableHttpClient {

    private final RestTemplate restTemplate;

    public RestTemplateEurekaSubscribeHttpClient(RestTemplate restTemplate, String serviceUrl) {
        super(restTemplate, serviceUrl);
        this.restTemplate = restTemplate;
    }

    @Override
    public EurekaHttpResponse<Application> subscribeApplication(String appName) {
        String urlPath = getServiceUrl() + "subscribe/" + appName;

        ResponseEntity<Application> response = restTemplate.exchange(urlPath,
                HttpMethod.GET, null, Application.class);

        Application application = response.getStatusCodeValue() == HttpStatus.OK.value()
                && response.hasBody() ? response.getBody() : null;

        return anEurekaHttpResponse(response.getStatusCodeValue(), application)
                .headers(headersOf(response)).build();
    }


    private static Map<String, String> headersOf(ResponseEntity<?> response) {
        HttpHeaders httpHeaders = response.getHeaders();
        if (httpHeaders == null || httpHeaders.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> headers = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : httpHeaders.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                headers.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        return headers;
    }
}
