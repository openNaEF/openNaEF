package voss.discovery.agent.common;

public interface ExtInfoRenderer<T> {
    String getKey();

    void set(T value);

    T get();

    boolean isDefined();
}