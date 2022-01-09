package cn.icodening.eureka.client;

import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;

/**
 * @author icodening
 * @date 2022.01.09
 */
public interface EurekaSubscribableHttpClient extends EurekaHttpClient {

    /**
     * 订阅指定应用
     *
     * @param appName 需要订阅的应用名
     * @return Eureka Server上的应用
     */
    EurekaHttpResponse<Application> subscribeApplication(String appName);

}
