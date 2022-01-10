package cn.icodening.eureka.server;

import cn.icodening.eureka.common.ApplicationHashGenerator;
import cn.icodening.eureka.common.ApplicationHashHistory;
import com.netflix.discovery.shared.Application;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * @author icodening
 * @date 2022.01.10
 */
class ApplicationHistoryUpdater implements DeferredResult.DeferredResultHandler {

    private final ApplicationHashGenerator applicationHashGenerator;

    ApplicationHistoryUpdater(ApplicationHashGenerator applicationHashGenerator) {
        this.applicationHashGenerator = applicationHashGenerator;
    }

    @Override
    public void handleResult(Object result) {
        if (!(result instanceof Application)) {
            return;
        }
        Application application = (Application) result;
        String applicationName = application.getName().toUpperCase();
        String appHash = applicationHashGenerator.generate(application);
        if (!ApplicationHashHistory.getLastHash(applicationName).equals(appHash)) {
            ApplicationHashHistory.updateHash(applicationName, appHash);
        }
    }
}
