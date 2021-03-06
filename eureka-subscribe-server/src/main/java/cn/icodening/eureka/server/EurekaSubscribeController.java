package cn.icodening.eureka.server;

import cn.icodening.eureka.common.ApplicationHashHistory;
import cn.icodening.eureka.common.Constants;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.registry.InstanceRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.netflix.eureka.EurekaConstants;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author icodening
 * @date 2022.01.07
 */
@RequestMapping(EurekaConstants.DEFAULT_PREFIX + "/subscribe")
public class EurekaSubscribeController {

    @Autowired
    @Qualifier("timeoutTaskExecutor")
    private ScheduledExecutorService scheduledExecutorService;

    @Autowired
    private EurekaDeferredResultStore eurekaDeferredResultStore;

    @Autowired
    private InstanceRegistry instanceRegistry;

    @Autowired
    @Qualifier("applicationHistoryUpdater")
    private DeferredResult.DeferredResultHandler applicationHistoryUpdater;

    @GetMapping("/{appName}")
    @ResponseBody
    public DeferredResult<Application> subscribe(@PathVariable(name = "appName") String appName,
                                                 @RequestHeader(name = Constants.HEADER_READ_TIMEOUT, defaultValue = "30") long readTimeout,
                                                 @RequestHeader(name = Constants.HEADER_APP_HASH, required = false) String appHash
    ) {
        if (readTimeout <= 1) {
            readTimeout = 30;
        }
        long timeout = TimeUnit.MILLISECONDS.convert(readTimeout - 1, TimeUnit.SECONDS);
        String upperAppName = appName.toUpperCase();
        String lastHash = ApplicationHashHistory.getLastHash(upperAppName);
        DeferredResult<Application> result = buildDeferredResult();
        if (appHash == null || Objects.equals(appHash, lastHash)) {
            //??????????????????????????????????????????hash?????????????????????????????????hash?????????????????????????????????????????????????????????
            eurekaDeferredResultStore.pushDeferredResult(appName.toUpperCase(), result);
            //???????????????????????????
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
            scheduledExecutorService.schedule(() -> {
                result.setResult(instanceRegistry.getApplication(upperAppName));
            }, timeout, TimeUnit.MILLISECONDS);
            return result;
        }
        //hash???????????????????????????????????????????????????
        Application nowApplication = Optional.ofNullable(instanceRegistry.getApplication(upperAppName)).orElse(new Application(upperAppName));
        result.setResult(nowApplication);
        return result;
    }

    private DeferredResult<Application> buildDeferredResult() {
        DeferredResult<Application> ret = new DeferredResult<>();
        ret.setResultHandler(applicationHistoryUpdater);
        return ret;
    }
}
