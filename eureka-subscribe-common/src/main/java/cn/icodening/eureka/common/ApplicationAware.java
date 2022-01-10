package cn.icodening.eureka.common;

import com.netflix.discovery.shared.Application;

/**
 * 应用感知接口.
 * 感知到应用发生改变时需要回调该接口
 *
 * @author icodening
 * @date 2022.01.11
 */
public interface ApplicationAware {

    void setApplication(Application application);
}
