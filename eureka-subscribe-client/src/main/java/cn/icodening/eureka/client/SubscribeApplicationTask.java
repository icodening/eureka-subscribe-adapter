package cn.icodening.eureka.client;

import cn.icodening.eureka.common.ApplicationHashGenerator;
import cn.icodening.eureka.common.ApplicationHashHistory;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author icodening
 * @date 2022.01.09
 */
public class SubscribeApplicationTask extends TimerTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscribeApplicationTask.class);

    private final String appName;

    private final EurekaSubscribableHttpClient eurekaSubscribableHttpClient;

    private final ApplicationHashGenerator applicationHashGenerator;

    private final ScheduledExecutorService subscribeApplicationExecutor;

    private volatile boolean stop = false;

    public SubscribeApplicationTask(String appName,
                                    EurekaSubscribableHttpClient eurekaSubscribableHttpClient) {
        this(appName, eurekaSubscribableHttpClient, ApplicationHashGenerator.DEFAULT);
    }

    public SubscribeApplicationTask(String appName,
                                    EurekaSubscribableHttpClient eurekaSubscribableHttpClient,
                                    ApplicationHashGenerator applicationHashGenerator) {
        this.appName = appName;
        this.eurekaSubscribableHttpClient = eurekaSubscribableHttpClient;
        this.applicationHashGenerator = applicationHashGenerator;
        this.subscribeApplicationExecutor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r);
            thread.setName(appName + "-subscribe-thread");
            return thread;
        });
    }

    public String getAppName() {
        return appName;
    }

    public EurekaSubscribableHttpClient getEurekaSubscribableHttpClient() {
        return eurekaSubscribableHttpClient;
    }

    @Override
    public void run() {
        try {
            EurekaHttpResponse<Application> applicationEurekaHttpResponse = eurekaSubscribableHttpClient.subscribeApplication(appName);
            Application application = applicationEurekaHttpResponse.getEntity();
            onSuccess(application);
        } catch (Throwable exception) {
            onException(exception);
        } finally {
            onComplete();
        }

    }

    public synchronized void start(){
        subscribeApplicationExecutor.execute(this);
    }

    public void stop() {
        stop = true;
    }

    protected void onComplete() {
        if (!stop) {
            subscribeApplicationExecutor.execute(this);
        }
    }

    protected void onException(Throwable exception) {
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error("subscribe servers error!!!", exception);
        }
    }

    protected void onSuccess(Application application) {
        String serverApplicationHash = applicationHashGenerator.generate(application);
        if (application == null) {
            onApplicationChange(null);
            ApplicationHashHistory.updateHash(appName.toUpperCase(), serverApplicationHash);
            return;
        }
        String lastHash = ApplicationHashHistory.getLastHash(appName.toUpperCase());
        if (!lastHash.equals(serverApplicationHash)) {
            onApplicationChange(application);
        }
        ApplicationHashHistory.updateHash(appName.toUpperCase(), serverApplicationHash);
    }

    protected void onApplicationChange(Application application) {

    }
}
