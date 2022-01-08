package cn.icodening.eureka.server;

import com.netflix.appinfo.InstanceInfo;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author icodening
 * @date 2022.01.07
 */
class EurekaDeferredResultStore {

    private final Map<String, List<DeferredResult<List<InstanceInfo>>>> deferredResults = new LinkedCaseInsensitiveMap<>();

    public void pushDeferredResult(String appName, DeferredResult<List<InstanceInfo>> deferredResult) {
        List<DeferredResult<List<InstanceInfo>>> defs = deferredResults.get(appName);
        if (defs == null) {
            synchronized (deferredResults) {
                defs = deferredResults.get(appName);
                if (defs == null) {
                    List<DeferredResult<List<InstanceInfo>>> contexts = new LinkedList<>();
                    deferredResults.put(appName, contexts);
                    contexts.add(deferredResult);
                    return;
                }
            }
        }
        synchronized (defs) {
            defs.add(deferredResult);
        }
    }

    public List<DeferredResult<List<InstanceInfo>>> getDeferredResults(String appName) {
        List<DeferredResult<List<InstanceInfo>>> defs = deferredResults.get(appName);
        if (defs != null) {
            synchronized (deferredResults) {
                defs = deferredResults.get(appName);
                return defs;
            }
        }
        return Collections.emptyList();
    }
}
