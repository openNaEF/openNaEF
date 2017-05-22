package tef;

class List<T> {

    private int size_ = 0;
    private T[] data_ = null;

    List() {
    }

    void add(T datum) {
        ensureCapacity();
        data_[size_++] = datum;
    }

    private void ensureCapacity() {
        if (data_ != null && size_ < data_.length) {
            return;
        }

        int newArrayLength = Math.max((int) (size_ * 1.1), size_ + 1);
        Object[] newData = new Object[newArrayLength];
        if (data_ != null) {
            System.arraycopy(data_, 0, newData, 0, size_);
        }
        data_ = (T[]) newData;
    }

    void removeLast() {
        if (size_ == 0) {
            throw new IllegalStateException();
        }

        data_[--size_] = null;
    }

    T get(int index) {
        if (index >= size_) {
            throw new IllegalArgumentException();
        }

        return data_[index];
    }

    int size() {
        return size_;
    }
}
