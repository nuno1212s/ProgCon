package pc.bqueue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;


/**
 * Lock-free implementation of queue.
 *
 *
 * @param <E> Type of elements.
 */
public class LFBQueue<E> implements BQueue<E> {

  protected static final int SIZE_ROOM = 0, ADD_ROOM = 1, REMOVE_ROOM = 2;

  private E[] array;
  private final AtomicInteger head, tail;
  private final Rooms rooms;
  private final boolean useBackoff;

  /**
   * Constructor.
   * @param initialCapacity Initial queue capacity.
   * @param backoff Flag to enable/disable the use of back-off.
   * @throws IllegalArgumentException if {@code capacity <= 0}
   */
  @SuppressWarnings("unchecked")
  public LFBQueue(int initialCapacity, boolean backoff) {
    head = new AtomicInteger(0);
    tail = new AtomicInteger(0);
    array = (E[]) new Object[initialCapacity];
    useBackoff = backoff;
    rooms = new Rooms(3, backoff);
  }

  @Override
  public int capacity() {
    return array.length;
  }

  @Override
  public int size() {

    rooms.enter(SIZE_ROOM);

    int size = tail.get() - head.get();

    rooms.leave(SIZE_ROOM);

    return size;
  }


  @Override
  public void add(E elem) {

    while(true) {
      rooms.enter(ADD_ROOM);

      int p = tail.getAndIncrement();
      if (p - head.get() < array.length) {
        array[p % array.length] = elem;
        break;
      } else {
        // "undo"
        tail.getAndDecrement();
        //We must leave the room to give chance to other threads
        //Of emptying the array or we'll be stuck here forever
        rooms.leave(ADD_ROOM);

        if (useBackoff)
          Backoff.delay();
      }
    }

    rooms.leave(ADD_ROOM);

    if (useBackoff)
      Backoff.reset();
  }

  @Override
  public E remove() {
    E elem = null;
    while(true) {
      rooms.enter(REMOVE_ROOM);

      int p = head.getAndIncrement();
      if (p < tail.get()) {
        int pos = p % array.length;
        elem = array[pos];
        array[pos] = null;
        break;
      } else {
        // "undo"
        head.getAndDecrement();

        //We must give the chance to other threads
        //To fill the array or we'll be stuck here forever
        rooms.leave(REMOVE_ROOM);

        if (useBackoff)
          Backoff.delay();
      }
    }

    rooms.leave(REMOVE_ROOM);

    if (useBackoff)
      Backoff.reset();

    return elem;
  }

  @Override
  public String toString() {
      return Arrays.toString(array);
  }

  /**
   * Test instantiation.
   */
  public static final class Test extends BQueueTest {
    @Override
    <T> BQueue<T> createBQueue(int capacity) {
      return new LFBQueue<>(capacity, false);
    }
  }
}
