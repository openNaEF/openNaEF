package voss.discovery.agent.vmware.collector;

import java.util.LinkedList;
import java.util.Queue;

public class ResponseQueue {
    Queue<MIBResponse> queue;

    public ResponseQueue() {
        queue = new LinkedList<MIBResponse>();
    }

    synchronized public void putResponse(MIBResponse response) {
        queue.add(response);
        notifyAll();
    }

    synchronized public MIBResponse getResponse() throws InterruptedException {
        if (queue.peek() == null) {
            wait();
        }

        return queue.poll();
    }

    synchronized public void release() {
        notifyAll();
    }
}