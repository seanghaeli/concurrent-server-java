package cpen221.mp3.fsftbuffer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FSFTBuffer<T extends Bufferable> {

    /* the default buffer size is 32 objects */
    public static final int DSIZE = 32;

    /* the default timeout value is 3600s */
    public static final int DTIMEOUT = 3600;

    private final int size;
    private final int timeout;
    private final ConcurrentHashMap<T, Long> buffer;
    /**
     * Representation Invariant:
     * size, timeout, and buffer are not null
     * size >= buffer.keySet().size() == buffer.values().size()
     * Every key, T, in buffer has a corresponding Long
     * For every key t, buffer.get(t) >= System.currentTimeMillis()
     * For every key t, buffer.get(t) <= System.currentTimeMillis() + timeout
     */
    /**
     * Abstraction Function:
     * For every t, where t is an object of type T and
     * a key in buffer, buffer.get(t) represents the time
     * when t will timeout and be removed from the buffer.
     */
    /**
     * Thread safety argument:
     * size and timeout are final
     * the only mutable datatype used in the representation
     *      of FSFTBuffer, ConcurrentHashMap, is thread safe
     * the only other mutable datatype used in the implementation,
     *      ConcurrentLinkedQueue, is thread safe
     * all methods are synchronized
     */
    /**
     * Create a buffer with a fixed capacity and a timeout value.
     * Objects in the buffer that have not been refreshed within the
     * timeout period are removed from the cache.
     *
     * @param capacity the number of objects the buffer can hold
     * @param timeout  the duration, in seconds, an object should
     *                 be in the buffer before it times out
     */
    public FSFTBuffer(int capacity, int timeout) {
        this.size = capacity;
        this.timeout = timeout;
        buffer = new ConcurrentHashMap<>(capacity);
    }

    /**
     * Create a buffer with default capacity and timeout values.
     */
    public FSFTBuffer() {
        this(DSIZE, DTIMEOUT);
    }

    /**
     * Add a value to the buffer.
     * If the buffer is full then remove the least recently accessed
     * object to make room for the new object.
     *
     * @param t the object T being placed in the buffer
     * @return false if t is already in the buffer and true otherwise
     */
    synchronized public boolean put(T t) {
        checkState();
        for (T element : buffer.keySet()) {
            if (element.id().equals(t.id())) {
                return false;
            }
        }
        if (buffer.size() == size) {
            T nextTimeoutElement =  buffer.keySet().stream().findFirst().get();
            long nextTimeout = buffer.get(nextTimeoutElement);
            for (T element : buffer.keySet()) {
                if (buffer.get(element) < nextTimeout) {
                    nextTimeoutElement = element;
                    nextTimeout = buffer.get(element);
                }
            }
            buffer.remove(nextTimeoutElement);
        }
        long cacheTime = System.currentTimeMillis() + timeout * 1000;
        buffer.put(t, cacheTime);
        return true;
    }

    /**
     * Retrieve from the buffer the object T which corresponds
     * to a given String id if present, otherwise throws
     * a NotFoundException.
     *
     * @param id the identifier of the object to be retrieved
     * @return the object that matches the identifier from the
     * buffer
     * @throws NotFoundException if there is no object T with
     *                           a matching id
     */
    synchronized public T get(String id) throws NotFoundException {
        checkState();
        for (T element : buffer.keySet()) {
            if (element.id().equals(id)) {
                long cacheTime = System.currentTimeMillis() + timeout * 1000;
                buffer.replace(element, cacheTime);
                return element;
            }
        }
        throw new NotFoundException("No object with matching ID in cache.");
    }

    /**
     * Update the last refresh time for the object with the provided id.
     * This method is used to mark an object as "not stale" so that its
     * timeout is delayed.
     *
     * @param id the identifier of the object to "touch"
     * @return true if successful and false otherwise
     */
    synchronized public boolean touch(String id) {
        checkState();
        for (T element : buffer.keySet()) {
            if (element.id().equals(id)) {
                long cacheTime = System.currentTimeMillis() + timeout * 1000;
                buffer.replace(element, cacheTime);
                return true;
            }
        }
        return false;
    }

    /**
     * Update an object in the buffer.
     * This method updates an object and acts like a "touch" to
     * renew the object in the cache.
     *
     * @param t the object to update
     * @return true if successful and false otherwise
     */
    synchronized public boolean update(T t) {
        checkState();
        for (T element : buffer.keySet()) {
            if (element.id().equals(t.id())) {
                long time = buffer.get(element);
                buffer.remove(element);
                buffer.put(t, time);
                return true;
            }
        }
        return false;
    }

    /**
     * Makes sure all the values stored in the buffer have not timed
     * out. Modifies buffer by removing all values that are less
     * than System.currentTimeMillis().
     */
    synchronized private void checkState() {
        ConcurrentLinkedQueue<T> timeoutQueue = new ConcurrentLinkedQueue<T>();
        for (T element : buffer.keySet()) {
            if (buffer.get(element) < System.currentTimeMillis()) {
                timeoutQueue.add(element);
            }
        }
        while (!timeoutQueue.isEmpty()) {
            T element = timeoutQueue.poll();
            buffer.remove(element);
        }
    }

}
