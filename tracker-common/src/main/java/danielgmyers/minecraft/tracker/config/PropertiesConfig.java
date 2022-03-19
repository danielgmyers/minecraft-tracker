package danielgmyers.minecraft.tracker.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

public class PropertiesConfig implements Config {

    private static final Logger LOG = LogManager.getLogger();

    private final Properties properties;

    private PropertiesConfig() {
        this.properties = new Properties();
    }

    public static PropertiesConfig create(Path configPath) {
        File configFile = configPath.toFile();
        PropertiesConfig config = new PropertiesConfig();

        if (!configFile.exists()) {
            config.saveDefaults(configFile);
        } else {
            config.load(configFile);
        }

        return config;
    }

    private void saveDefaults(File configFile) {
        properties.clear();
        properties.setProperty(REPORTER_TYPE, REPORTER_TYPE_DEFAULT.toString());
        properties.setProperty(CLOUDWATCH_METRIC_NAMESPACE, CLOUDWATCH_METRIC_NAMESPACE_DEFAULT);
        try (FileWriter writer = new FileWriter(configFile)) {
            properties.store(writer, "Default configuration for tracker.");
        } catch (IOException e) {
            LOG.warn("Failed to write configuration to {}.", configFile.getPath(), e);
        }
    }

    private void load(File configFile) {
        try (FileReader reader = new FileReader(configFile)) {
            properties.load(reader);
        } catch (FileNotFoundException e) {
            LOG.info("Config file not found at {}, using defaults.", configFile.getPath());
        } catch (IOException e) {
            LOG.warn("Failed to load configuration from {}, using defaults.", configFile.getPath(), e);
        }
    }

    @Override
    public String retrieveConfig(String propertyName, String defaultValue) {
        return properties.getProperty(propertyName, defaultValue);
    }
}
