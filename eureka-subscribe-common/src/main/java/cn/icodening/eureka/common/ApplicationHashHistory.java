package cn.icodening.eureka.common;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author icodening
 * @date 2022.01.08
 */
public class ApplicationHashHistory {

    private static final Map<String, String> lastApplicationHashMap = new ConcurrentHashMap<>();

    public static String getLastHash(String applicationName) {
        return Optional.ofNullable(lastApplicationHashMap.get(applicationName))
                .orElse(Constants.NONE_HASH);
    }

    public static void updateHash(String applicationName, String hash) {
        if (applicationName == null) {
            return;
        }
        if (hash == null) {
            hash = Constants.NONE_HASH;
        }
        lastApplicationHashMap.put(applicationName, hash);
    }
}
