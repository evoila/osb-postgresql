package de.evoila.cf.broker.custom.postgres;

/**
 * @author Johannes Hiemer.
 */
public class PostgreSQLUtils {

    public static String dbName(String uuid) {
        if (uuid != null && uuid.length() > 15)
            return "d" + uuid.replace("-", "").substring(0, 15);
        else
            return null;
    }

}
