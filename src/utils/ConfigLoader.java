package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static final Properties props = new Properties();

    static {
        try (InputStream input = new FileInputStream("config.properties")) {
            props.load(input);
            System.out.println("Config loaded: " + props.size() + " properties found.");
        } catch (IOException e) {
            System.err.println("WARNING: config.properties not found. Using defaults.");
        }
    }

    public static String get(String key) {
        return props.getProperty(key, null);
    }

    public static String get(String key, String defaultValue) {
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        return props.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }
}