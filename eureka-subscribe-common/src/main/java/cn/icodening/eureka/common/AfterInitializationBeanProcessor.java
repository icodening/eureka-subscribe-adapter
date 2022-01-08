package cn.icodening.eureka.common;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author icodening
 * @date 2022.01.08
 */
public class AfterInitializationBeanProcessor<T> implements BeanPostProcessor {

    private final Class<T> type;

    private final String beanName;

    private Consumer<T> consumer;

    public AfterInitializationBeanProcessor(Class<T> type) {
        this(type, null);
    }

    public AfterInitializationBeanProcessor(String beanName) {
        this(null, beanName);
    }

    public AfterInitializationBeanProcessor(Class<T> type, String beanName) {
        this.type = type;
        this.beanName = beanName;
    }

    @SuppressWarnings("unchecked")
    public Object postProcessAfterInitialization(@Nullable Object bean, @Nullable String beanName) throws BeansException {
        if (bean == null || beanName == null) {
            return bean;
        }
        if (type == null && this.beanName == null) {
            return bean;
        }
        boolean matchType = false;
        if (type != null) {
            matchType = type.isAssignableFrom(bean.getClass());
        }
        if (matchType && Objects.equals(this.beanName, beanName)) {
            if (consumer != null) {
                consumer.accept((T) bean);
            }
        }
        return bean;
    }

    public AfterInitializationBeanProcessor<T> onBean(Consumer<T> consumer) {
        this.consumer = consumer;
        return this;
    }
}
