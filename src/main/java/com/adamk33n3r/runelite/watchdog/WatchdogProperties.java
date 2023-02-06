package com.adamk33n3r.runelite.watchdog;

import lombok.AccessLevel;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class WatchdogProperties {
    @Getter(AccessLevel.PACKAGE)
    private static final Properties properties = new Properties();

    static {
        try (InputStream in = WatchdogProperties.class.getResourceAsStream("watchdog.properties")) {
            properties.load(in);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        try (InputStream in = WatchdogProperties.class.getResourceAsStream("version.properties")) {
            properties.load(in);
            String pluginVersion = String.format(
                "%s.%s.%s",
                properties.getProperty("VERSION_MAJOR"),
                properties.getProperty("VERSION_MINOR"),
                properties.getProperty("VERSION_PATCH"));
            properties.put("watchdog.pluginVersion", pluginVersion);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
