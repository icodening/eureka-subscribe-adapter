package cn.icodening.eureka.client;

import cn.icodening.eureka.common.ApplicationHashGenerator;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;


/**
 * @author icodening
 * @date 2022.01.10
 */
public class KeepSubscribeApplicationTask extends SubscribeApplicationTask {

    private final Executor executor;

    private volatile boolean stop = false;

    public KeepSubscribeApplicationTask(String appName,
                                        EurekaSubscribableHttpClient eurekaSubscribableHttpClient) {
        this(appName, eurekaSubscribableHttpClient, ApplicationHashGenerator.DEFAULT);
    }

    public KeepSubscribeApplicationTask(String appName,
                                        EurekaSubscribableHttpClient eurekaSubscribableHttpClient,
                                        ApplicationHashGenerator applicationHashGenerator) {
        this(appName, eurekaSubscribableHttpClient, applicationHashGenerator, ForkJoinPool.commonPool());
    }

    public KeepSubscribeApplicationTask(String appName,
                                        EurekaSubscribableHttpClient eurekaSubscribableHttpClient,
                                        ApplicationHashGenerator applicationHashGenerator,
                                        Executor executor) {
        super(appName, eurekaSubscribableHttpClient, applicationHashGenerator);
        this.executor = executor;
    }

    @Override
    protected void onComplete() {
        if (!stop) {
            executor.execute(this);
        }
    }

    public void stop() {
        this.stop = true;
    }


}
