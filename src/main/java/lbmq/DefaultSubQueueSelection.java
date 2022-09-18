package lbmq;

import java.util.ArrayList;

/**
 * Chooses the next queue to be used from the highest priority group. If no queue is found it searches the lower
 * priority groups and so on until it finds a queue.
 */
public class DefaultSubQueueSelection<K, E> implements LinkedBlockingMultiQueue.SubQueueSelection<K, E> {

    private ArrayList<LinkedBlockingMultiQueue<K, E>.PriorityGroup> priorityGroups;

    @Override
    public LinkedBlockingMultiQueue<K, E>.SubQueue getNext() {
        for (LinkedBlockingMultiQueue<K, E>.PriorityGroup priorityGroup : priorityGroups) {
            LinkedBlockingMultiQueue<K, E>.SubQueue subQueue = priorityGroup.getNextSubQueue();
            if (subQueue != null) {
                return subQueue;
            }
        }
        return null;
    }

    @Override
    public E peek() {
        // assert takeLock.isHeldByCurrentThread();
        for (LinkedBlockingMultiQueue<K, E>.PriorityGroup priorityGroup : priorityGroups) {
            E dequed = priorityGroup.peek();
            if (dequed != null) {
                return dequed;
            }
        }
        return null;
    }

    @Override
    public void setPriorityGroups(ArrayList<LinkedBlockingMultiQueue<K, E>.PriorityGroup> priorityGroups) {
        this.priorityGroups = priorityGroups;
    }
}
