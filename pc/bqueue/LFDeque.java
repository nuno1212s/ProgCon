package pc.bqueue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicMarkableReference;

import java.util.Arrays;

public class LFDeque<E> extends LFBQueueU<E> implements BDeque<E> {

  public LFDeque(int initialCapacity, boolean useBackoff) {
    super(initialCapacity, useBackoff);
  }

  @Override
  public void addFirst(E elem) {

    while (true) {

      if (addElementFlag.compareAndSet(false, true)) {

        rooms.enter(ADD_ROOM);

        int p = head.updateAndGet(i -> {
          if (i <= 0) {
              //Because we have increased the head, we have to move the tail accordingly
              //With this operation we basically move the origin on the number "line" to the array length
              //Allowing for use to add as many numbers as we want to the front, as when we reach 0 again, we will have ran out of space
              //In the array, and it will be expanded

              tail.getAndAccumulate(array.length, Integer::sum);

              return array.length - 1;
          }

          return i - 1;
        });

        //System.out.println(p + " " + head.get() + " " + tail.get() + " " + array.length + " . First.");

        if (tail.get() - p < array.length) {

          array[p % array.length] = elem;

          rooms.leave(ADD_ROOM);
          addElementFlag.set(false);

          //System.out.println("Added in pos " + (p % array.length) + " new array: " + Arrays.toString(array));
          break;
        } else {

          //Resize array to have space in the back part
          E[] newArray = (E[]) new Object[this.array.length * 2];

          for (int i = p + 1; i <= tail.get(); i++) {
            newArray[i % newArray.length] = this.array[i % this.array.length];
          }

          newArray[p % newArray.length] = elem;

          //System.out.println("New array :" + Arrays.toString(newArray));

          this.array = newArray;

          addElementFlag.set(false);
          rooms.leave(ADD_ROOM);
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
  public void addLast(E elem) {
    add(elem);
  }

  @Override
  public E removeFirst() {
    return remove();
  }

  @Override
  public E removeLast() {

    E elem = null;

    while (true) {
      rooms.enter(REMOVE_ROOM);

      int p = tail.decrementAndGet();
      //System.out.println("Removing last: " + p + ". " + array.length + " " + Arrays.toString(array));

      if (p >= head.get()) {

        int pos = p % array.length;
        elem = array[pos];
        array[pos] = null;

        rooms.leave(REMOVE_ROOM);
        break;

      } else {

        tail.getAndIncrement();

        rooms.leave(REMOVE_ROOM);

        if (useBackoff)
            Backoff.delay();
      }
    }

    if (useBackoff)
      Backoff.reset();

    return elem;
  }

  public static final class Test extends BDequeTest {
    @Override
    <T> BDeque<T> createBDeque(int capacity) {
      return new LFDeque(capacity, false);
    }
  }

}
