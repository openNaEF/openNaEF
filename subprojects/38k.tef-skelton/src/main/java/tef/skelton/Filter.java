package tef.skelton;

public interface Filter<T> extends java.io.Serializable {

    public boolean accept(T obj);
}
