package voss.discovery.iolib.snmp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public abstract class AbstractWalkProcessor<T> implements SerializableWalkProcessor {
    protected static final Logger walkerlog = LoggerFactory.getLogger(AbstractWalkProcessor.class);
    protected final T result;

    public AbstractWalkProcessor(T result) {
        this.result = result;
    }

    public T getResult() {
        return this.result;
    }

    public void close() {
    }
}