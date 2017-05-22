package tef;

class Mutex {

    private boolean locked_ = true;

    synchronized void lock() {
        while (locked_) {
            try {
                wait();
            } catch (InterruptedException ie) {
                throw new RuntimeException(ie);
            }
        }

        locked_ = true;
    }

    synchronized void unlock() {
        locked_ = false;
        notifyAll();
    }
}
