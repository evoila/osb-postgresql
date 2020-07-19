package de.evoila.cf.broker.utils;

import de.evoila.cf.broker.custom.postgres.PostgresqlCatalogMgmt;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Patrick Weber.
 */

public class PostgresqlMapUtils {
    static public Map<String, Object> manifestProperties(String instanceGroup, Manifest manifest) {
        return manifest
                .getInstanceGroups()
                .stream()
                .filter(i -> {
                    return i.getName().equals(instanceGroup);
                }).findFirst().get().getProperties();
    }

    static public Object getMapProperty(Map<String, Object> map, String... keys) {
        Map<String, Object> nextMap = map;
        Object objectMap = map;
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            map = (Map<String, Object>) objectMap;
            if (!map.containsKey(key)) {
                return null;
            }
            objectMap = map.get(key);
        }
        return objectMap;
    }

    static public void deleteMapProperty(Map<String, Object> map, String... keys) {
        Map<String, Object> nextMap = map;
        Object objectMap = map;
        if (map == null) {
            return;
        }
        for (String key : keys) {
            map = (Map<String, Object>) objectMap;
            if (!map.containsKey(key)) {
                return;
            }
            if (map.size() == 1) {
                map.remove(key);
                return;
            }
        }
        map.remove(objectMap);
    }

    static public void setMapProperty(Map<String, Object> map, Object value, String... keys) {
        Map<String, Object> nextMap = map;
        final Logger log = LoggerFactory.getLogger(PostgresqlMapUtils.class);

        int i;
        for (i = 0; i < keys.length - 1; i++) {
            if (!map.containsKey(keys[i])) {
                map.put(keys[i],new HashMap<String,Object>());
            }
            map = (Map<String, Object>) map.get(keys[i]);
        }
        map.put(keys[i], value);
    }
}
