package com.vmware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

/**
 * @author Sumit Deo (deosu@vmware.com)
 */
public class ConfigCache {
  private final Properties configProp = new Properties();
  private final static Logger logger = LoggerFactory.getLogger(ConfigCache.class);

  private ConfigCache()
  {
    //Private constructor to restrict new instances
    InputStream in = this.getClass().getClassLoader().getResourceAsStream("config.properties");
    logger.info("Reading all properties from the file");
    try {
      configProp.load(in);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static class LazyHolder
  {
    private static final ConfigCache INSTANCE = new ConfigCache();
  }

  public static ConfigCache getInstance()
  {
    return LazyHolder.INSTANCE;
  }

  public String getProperty(String key){
    return configProp.getProperty(key);
  }
}
