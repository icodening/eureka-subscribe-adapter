package cn.icodening.eureka.server;

import cn.icodening.eureka.common.ApplicationHashGenerator;
import cn.icodening.eureka.common.ApplicationHashHistory;
import cn.icodening.eureka.common.Constants;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.registry.InstanceRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.*;

/**
 * @author icodening
 * @date 2022.01.07
 */
public class EurekaEventListenerConfiguration {

    @Autowired
    private InstanceRegistry instanceRegistry;

    @Autowired
    private EurekaDeferredResultStore eurekaDeferredResultStore;

    @Autowired
    private ApplicationHashGenerator applicationHashGenerator;

    @EventListener(EurekaInstanceRegisteredEvent.class)
    public void onRegistered(EurekaInstanceRegisteredEvent event) {
        if (InstanceInfo.InstanceStatus.DOWN.equals(event.getInstanceInfo().getStatus())) {
            //服务下线时也会调用注册接口并且状态为DOWN
            return;
        }
        String appName = event.getInstanceInfo().getAppName();
        Application application = instanceRegistry.getApplication(appName);
        List<InstanceInfo> instances = Optional.ofNullable(application)
                .map(Application::getInstancesAsIsFromEureka)
                .orElse(new ArrayList<>(1));
        instances.add(event.getInstanceInfo());
        complete(appName, instances);
    }

    @EventListener(EurekaInstanceCanceledEvent.class)
    public void onCanceled(EurekaInstanceCanceledEvent event) {
        String appName = event.getAppName();
        String serverId = event.getServerId();
        Application application = instanceRegistry.getApplication(appName);
        List<InstanceInfo> instances;
        if (application != null) {
            instances = application.getInstancesAsIsFromEureka();
            instances.removeIf(info -> serverId.equalsIgnoreCase(info.getId()));
        } else {
            instances = Collections.emptyList();
        }
        complete(appName, instances);

    }

    private void complete(String appName, List<InstanceInfo> instances) {
        //更新hash
        Application app = new Application(appName, instances);
        String upperAppName = appName.toUpperCase();
        Application application = instanceRegistry.getApplication(upperAppName);
        String currentHash = Optional.ofNullable(application)
                .map(applicationHashGenerator::generate)
                .orElse(Constants.NONE_HASH);
        ApplicationHashHistory.updateHash(upperAppName, currentHash);

        List<DeferredResult<Application>> deferredResults = eurekaDeferredResultStore.getDeferredResults(appName);
        if (deferredResults == null || deferredResults.isEmpty()) {
            return;
        }
        Iterator<DeferredResult<Application>> it = deferredResults.iterator();
        while (it.hasNext()) {
            DeferredResult<Application> dr = it.next();
            synchronized (dr) {
                if (!dr.hasResult()) {
                    dr.setResult(app);
                }
            }
            it.remove();
        }
    }

}
