package cn.icodening.eureka.ribbon;

import cn.icodening.eureka.client.EurekaSubscribableHttpClient;
import cn.icodening.eureka.client.KeepSubscribeApplicationTask;
import cn.icodening.eureka.common.ApplicationAware;
import com.netflix.discovery.shared.Application;
import com.netflix.loadbalancer.ServerListUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 可感知服务变化的ServerListUpdater
 *
 * @author icodening
 * @date 2022.01.10
 */
public class ServerListAwareUpdater implements ServerListUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerListAwareUpdater.class);

    private final AtomicBoolean isActive = new AtomicBoolean(false);

    private final String applicationName;
    private final EurekaSubscribableHttpClient eurekaSubscribableHttpClient;
    private final ScheduledExecutorService executor;
    private final List<ApplicationAware> applicationAwareList;

    private volatile KeepSubscribeApplicationTask keepSubscribeApplicationTask;

    private volatile long lastUpdated = System.currentTimeMillis();

    public ServerListAwareUpdater(String applicationName,
                                  EurekaSubscribableHttpClient eurekaSubscribableHttpClient,
                                  ScheduledExecutorService executor,
                                  List<ApplicationAware> applicationAwareList) {
        this.applicationName = applicationName;
        this.eurekaSubscribableHttpClient = eurekaSubscribableHttpClient;
        this.executor = executor;
        this.applicationAwareList = applicationAwareList == null ? Collections.emptyList() : applicationAwareList;
    }

    @Override
    public synchronized void start(UpdateAction updateAction) {
        if (isActive.compareAndSet(false, true)) {
            this.keepSubscribeApplicationTask = new KeepSubscribeApplicationTask(applicationName, eurekaSubscribableHttpClient) {
                @Override
                protected void onApplicationChange(Application application) {
                    for (ApplicationAware applicationAware : applicationAwareList) {
                        applicationAware.setApplication(application);
                    }
                    updateAction.doUpdate();
                    lastUpdated = System.currentTimeMillis();
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("{} is change, {}", applicationName, application.getInstancesAsIsFromEureka());
                    }
                }
            };
            executor.execute(keepSubscribeApplicationTask);
        }
    }

    @Override
    public synchronized void stop() {
        if (isActive.compareAndSet(true, false)) {
            keepSubscribeApplicationTask.stop();
        }
    }

    @Override
    public String getLastUpdate() {
        return new Date(lastUpdated).toString();
    }

    @Override
    public long getDurationSinceLastUpdateMs() {
        return System.currentTimeMillis() - lastUpdated;
    }

    @Override
    public int getNumberMissedCycles() {
        //单线程长轮询不存在错过周期
        return 0;
    }

    @Override
    public int getCoreThreads() {
        if (executor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) executor).getCorePoolSize();
        }
        return -1;
    }
}
