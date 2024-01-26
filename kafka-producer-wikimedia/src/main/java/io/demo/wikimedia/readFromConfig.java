package io.demo.wikimedia;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class readFromConfig {
    public String readConfig(String key) throws IOException {
        String filepath = "./config.properties";
        Properties pros = new Properties();
        FileInputStream ip = new FileInputStream(filepath);
        pros.load(ip);
        return pros.getProperty(key);
    }
}
