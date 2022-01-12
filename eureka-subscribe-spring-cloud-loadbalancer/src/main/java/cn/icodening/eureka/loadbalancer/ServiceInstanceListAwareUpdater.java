package cn.icodening.eureka.loadbalancer;

import cn.icodening.eureka.client.EurekaSubscribableHttpClient;
import cn.icodening.eureka.client.KeepSubscribeApplicationTask;
import cn.icodening.eureka.common.ApplicationAware;
import cn.icodening.eureka.common.ApplicationHashGenerator;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author icodening
 * @date 2022.01.12
 */
public class ServiceInstanceListAwareUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceInstanceListAwareUpdater.class);

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final String applicationName;
    private final EurekaSubscribableHttpClient eurekaSubscribableHttpClient;
    private final ScheduledExecutorService executor;

    private EurekaClientConfig eurekaClientConfig;
    private ApplicationHashGenerator applicationHashGenerator;
    private List<ApplicationAware> applicationAwareList;

    private volatile KeepSubscribeApplicationTask keepSubscribeApplicationTask;

    public ServiceInstanceListAwareUpdater(String applicationName,
                                           EurekaSubscribableHttpClient eurekaSubscribableHttpClient,
                                           ScheduledExecutorService executor) {
        this.applicationName = applicationName;
        this.eurekaSubscribableHttpClient = eurekaSubscribableHttpClient;
        this.executor = executor;
    }

    public void setApplicationAwareList(List<ApplicationAware> applicationAwareList) {
        this.applicationAwareList = applicationAwareList == null ? Collections.emptyList() : applicationAwareList;
    }

    public void setApplicationHashGenerator(ApplicationHashGenerator applicationHashGenerator) {
        this.applicationHashGenerator = applicationHashGenerator == null ? ApplicationHashGenerator.DEFAULT : applicationHashGenerator;
    }

    public void setEurekaClientConfig(EurekaClientConfig eurekaClientConfig) {
        this.eurekaClientConfig = eurekaClientConfig;
    }

    public synchronized void start() {
        if (isRunning.compareAndSet(false, true)) {
            long fetchIntervalMillis = Optional.ofNullable(eurekaClientConfig).map(EurekaClientConfig::getRegistryFetchIntervalSeconds).map(x -> TimeUnit.MILLISECONDS.convert(x, TimeUnit.SECONDS)).orElse(30 * 1000L);
            this.keepSubscribeApplicationTask = new KeepSubscribeApplicationTask(applicationName, eurekaSubscribableHttpClient, applicationHashGenerator, executor) {
                @Override
                protected void onApplicationChange(Application application) {
                    for (ApplicationAware applicationAware : applicationAwareList) {
                        applicationAware.setApplication(application);
                    }
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("{} is change, {}", applicationName, application.getInstancesAsIsFromEureka());
                    }
                }

                @Override
                protected void onException(Exception exception) {
                    super.onException(exception);
                    try {
                        Thread.sleep(fetchIntervalMillis);
                    } catch (InterruptedException e) {
                        //ignore
                    }
                }
            };
            executor.execute(keepSubscribeApplicationTask);
        }
    }

    public synchronized void stop() {
        if (isRunning.compareAndSet(true, false)) {
            keepSubscribeApplicationTask.stop();
        }
    }

    public boolean isRunning() {
        return isRunning.get();
    }
}
