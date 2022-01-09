package cn.icodening.eureka.common;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;

import java.util.List;
import java.util.Optional;

/**
 * @author icodening
 * @date 2022.01.08
 */
public interface ApplicationHashGenerator {

    ApplicationHashGenerator DEFAULT = new DefaultApplicationHashGenerator();

    /**
     * 根据实例信息生成hash码
     *
     * @param instanceInfos eureka实例列表
     * @return hash
     */
    String generate(List<InstanceInfo> instanceInfos);

    default String generate(Application application) {
        return Optional.ofNullable(application)
                .map(Application::getInstancesAsIsFromEureka)
                .map(this::generate)
                .orElse(Constants.NONE_HASH);
    }
}
