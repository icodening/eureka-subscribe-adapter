package cn.icodening.eureka.server;

import cn.icodening.eureka.common.ApplicationHashHistory;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.registry.InstanceRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.EurekaConstants;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author icodening
 * @date 2022.01.07
 */
@RequestMapping(EurekaConstants.DEFAULT_PREFIX + "/subscribe")
public class EurekaSubscribeController {

    @Autowired
    private EurekaDeferredResultStore eurekaDeferredResultStore;

    @Autowired
    private InstanceRegistry instanceRegistry;

    @GetMapping("/{appName}")
    @ResponseBody
    public DeferredResult<Application> subscribe(@PathVariable(name = "appName") String appName,
                                                        @RequestHeader(name = "read-timeout", defaultValue = "30") long readTimeout,
                                                        @RequestHeader(name = "app-hash", required = false) String appHash
    ) {
        if (readTimeout <= 1) {
            readTimeout = 30;
        }
        long timeout = TimeUnit.MILLISECONDS.convert(readTimeout - 1, TimeUnit.SECONDS);
        String upperAppName = appName.toUpperCase();
        String lastHash = ApplicationHashHistory.getLastHash(upperAppName);
        DeferredResult<Application> result = buildDeferredResult(appName, timeout);
        if (appHash == null || Objects.equals(appHash, lastHash)) {
            //客户端没有传递服务列表的当前hash或传递了但与服务端上次hash一致，说明服务列表没有改变，则立即订阅
            eurekaDeferredResultStore.pushDeferredResult(appName.toUpperCase(), result);
            //完成后需要移除回调
            result.onCompletion(() -> {
                List<DeferredResult<Application>> deferredResults = eurekaDeferredResultStore.getDeferredResults(upperAppName);
                Iterator<DeferredResult<Application>> iterator = deferredResults.iterator();
                while (iterator.hasNext()) {
                    DeferredResult<Application> tmp = iterator.next();
                    if (result == tmp) {
                        iterator.remove();
                        break;
                    }
                }
            });
            return result;
        }
        //hash不相等则立即返回当前最新的服务列表
        Application nowApplication = instanceRegistry.getApplication(appName);
        result.setResult(nowApplication);
        return result;
    }

    private DeferredResult<Application> buildDeferredResult(String appName, long timeout) {
        return new DeferredResult<>(timeout, () ->
                Optional.ofNullable(instanceRegistry.getApplication(appName.toUpperCase()))
                        .orElse(new Application(appName.toUpperCase())));
    }
}
