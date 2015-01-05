package org.demo.core.examples.obs;

/**
 * Created by vx00418 on 12/1/2014.
 */
public interface Observable {
    void registerObserver(Observer o);
    void removeObserver(Observer o);
    void notifyObservers();
}
