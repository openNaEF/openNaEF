package tef.skelton;

import tef.MVO;
import tef.MvoHome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Constants extends MVO {

    public static final Home home = new Home();

    public static class Home extends MvoHome<Constants> {

        final F1UniqueIndex nameIndex = new F1UniqueIndex();

        private Home() {
            super(Constants.class);
        }

        public Constants getByName(String name) {
            return nameIndex.get(name);
        }
    }

    private final F1<String> name_ = new F1<String>(home.nameIndex);

    private final M1<String, Integer> values_ = new M1<String, Integer>();

    public Constants(MvoId id) {
        super(id);
    }

    public Constants(String name) {
        if (name == null) {
            throw new IllegalArgumentException();
        }

        setName(name);
    }

    void setName(String name) {
        name_.set(name);
    }

    public String getName() {
        return name_.get();
    }

    public void addValue(String value) {
        if (values_.containsKey(value) && getIndex(value) != null) {
            throw new IllegalStateException();
        }

        int index = getNextIndex();
        values_.put(value, index);
    }

    private int getNextIndex() {
        int result = 0;
        for (Integer index : values_.getValues()) {
            if (index != null) {
                result = Math.max(result, index.intValue() + 1);
            }
        }
        return result;
    }

    public void removeValue(String value) {
        Integer index = getIndex(value);
        if (index == null) {
            return;
        }

        values_.put(value, null);
    }

    public List<String> getValues() {
        List<String> result = new ArrayList<String>();
        for (String value : values_.getKeys()) {
            if (getIndex(value) != null) {
                result.add(value);
            }
        }
        Collections.sort(result, new Comparator<String>() {

            @Override public int compare(String s1, String s2) {
                return getIndex(s1).compareTo(getIndex(s2));
            }
        });
        return result;
    }

    public Integer getIndex(String value) {
        return values_.get(value);
    }
}
