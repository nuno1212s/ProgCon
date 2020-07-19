package pc.bqueue;

import java.util.Arrays;

/**
 * Monitor-based implementation of queue.
 *
 *
 * @param <E> Type of elements.
 */
public class MBQueueU<E> extends MBQueue<E> {

  /**
   * Constructor.
   * @param initialCapacity Initial queue capacity.
   * @throws IllegalArgumentException if {@code capacity <= 0}
   */
  public MBQueueU(int initialCapacity) {
    super(initialCapacity);
  }

  @Override
  public synchronized int capacity() {
    return UNBOUNDED;
  }

  @Override
  public synchronized void add(E elem) {
      if (size == array.length) {

          E[] newArray = (E[]) new Object[array.length * 2];

          int head = this.head;

          for (int i = 0; i < size; head = (head + 1) % array.length) {

              newArray[i++] = array[head];

          }

          this.head = 0;
          array = newArray;
      }

      array[(head + size) % array.length] = elem;
      size++;
      notifyAll();
  }

  /**
   * Test instantiation.
   */
  public static final class Test extends BQueueTest {
    @Override
    <T> BQueue<T> createBQueue(int initialCapacity) {
      return new MBQueueU<>(initialCapacity);
    }
  }
}
