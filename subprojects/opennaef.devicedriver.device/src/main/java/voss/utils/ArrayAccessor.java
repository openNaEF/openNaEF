package voss.utils;

public class ArrayAccessor {
    private final String[] array;

    public ArrayAccessor(String[] array) {
        this.array = array;
    }

    public String get(int position) {
        if (array.length > position) {
            return array[position];
        }
        return null;
    }
}