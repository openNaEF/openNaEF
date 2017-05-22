package tef.skelton;

import java.util.SortedSet;

public interface Model {

    public SortedSet<String> getAttributeNames();
    public <S, T extends Model> void set(Attribute<S, ? super T> attr, S value);
    public <S, T extends Model> S get(Attribute<S, ? super T> attr);
    public void putValue(String key, Object value);
    public Object getValue(String key);
}
