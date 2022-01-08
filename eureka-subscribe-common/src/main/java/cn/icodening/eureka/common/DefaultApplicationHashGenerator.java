package cn.icodening.eureka.common;

import com.netflix.appinfo.InstanceInfo;

import java.util.List;

/**
 * @author icodening
 * @date 2022.01.08
 */
public class DefaultApplicationHashGenerator implements ApplicationHashGenerator {

    @Override
    public String generate(List<InstanceInfo> instanceInfos) {
        if (instanceInfos == null) {
            return Constants.NONE_HASH;
        }
        long sumHash = 0;
        for (InstanceInfo instanceInfo : instanceInfos) {
            long idHash = instanceInfo.getId().hashCode();
            long metadataHash = instanceInfo.getMetadata().hashCode();
            long statusHash = instanceInfo.getStatus().hashCode();
            sumHash = sumHash + idHash + metadataHash + statusHash;
        }
        return String.valueOf(sumHash);
    }
}
