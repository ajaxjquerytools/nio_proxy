package org.demo.core.examples.obs;

/**
 * Created by vx00418 on 12/1/2014.
 */
public interface Observer {
    void update (float temperature, float humidity, float pressure);
}
