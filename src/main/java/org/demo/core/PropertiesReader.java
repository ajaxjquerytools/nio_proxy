package org.demo.core;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Created by vx00418 on 11/27/2014.
 */
public class PropertiesReader {
    public static final String CONFIG_FILE_NAME="proxy_config.properties";

    public void readConfigFileFromClasspath(){
//        Paths.get(Pro);
//        Files.newBufferedReader();
    }

    public static Properties readFromFile(File file){
        if(file==null){
            throw new NullPointerException("File should be not null");
        }
       // return new Properties(new FileInputStream(file));
        return null;
    }
}
