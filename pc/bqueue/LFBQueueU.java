package pc.bqueue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.Arrays;

/**
 * Lock-free implementation of queue - unbounded variant.
 *
 *
 * @param <E> Type of elements.
 */
public class LFBQueueU<E>  implements BQueue<E> {

  protected static final int SIZE_ROOM = 0, ADD_ROOM = 1, REMOVE_ROOM = 2;

  protected E[] array;
  protected final AtomicInteger head;
  protected final AtomicInteger tail;
  protected final AtomicBoolean addElementFlag;
  protected final Rooms rooms;
  protected final boolean useBackoff;


  /**
   * Constructor.
   * @param initialCapacity Initial queue capacity.
   * @param backoff Flag to enable/disable the use of back-off.
   * @throws IllegalArgumentException if {@code capacity <= 0}
   */
  @SuppressWarnings("unchecked")
  public LFBQueueU(int initialCapacity, boolean backoff) {
    head = new AtomicInteger(0);
    tail = new AtomicInteger(0);new AtomicMarkableReference<>(0, false);
    addElementFlag = new AtomicBoolean();
    array = (E[]) new Object[initialCapacity];
    useBackoff = backoff;
    rooms = new Rooms(3, backoff);
  }

  @Override
  public int capacity() {
    return UNBOUNDED;
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
          //Try to aquire the lock with and atomic compare and set
          if (addElementFlag.compareAndSet(false, true)) {
            rooms.enter(ADD_ROOM);

            int p = tail.getAndIncrement();
            if (p - head.get() < array.length) {
              array[p % array.length] = elem;

              rooms.leave(ADD_ROOM);
              addElementFlag.set(false);
              break;
            } else {

              E[] newArray = (E[]) new Object[this.array.length * 2];

              for (int i = head.get(); i < p; i++) {
                  newArray[i % newArray.length] = this.array[i % this.array.length];
              }

              newArray[p % newArray.length] = elem;

              //System.out.println(Arrays.toString(this.array) + " " + elem + ". Stats: " + head.get() + " " + tail.get() + " " + newArray.length);

              //System.out.println(Arrays.toString(newArray));

              this.array = newArray;

              //We must leave the room to give chance to other threads
              //Of emptying the array or we'll be stuck here forever
              rooms.leave(ADD_ROOM);
              addElementFlag.set(false);
              break;
            }
        } else {
          if (useBackoff)
            Backoff.delay();
        }
      }

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

        rooms.leave(REMOVE_ROOM);

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
      return new LFBQueueU<>(capacity, false);
    }
  }
}
