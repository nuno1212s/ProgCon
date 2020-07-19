package pc.bqueue;

import scala.concurrent.stm.Ref;
import scala.concurrent.stm.TArray;
import scala.concurrent.stm.japi.STM;

/**
 * STM implementation of queue
 *
 * @param <E> Type of elements.
 */
public class STMDeque<E> implements BDeque<E> {

  private final Ref.View<Integer> head;
  private final Ref.View<Integer> tail;
  private final Ref.View<TArray.View<E>> arrayRef;

  /**
   * Constructor.
   * @param capacity Initial queue capacity.
   * @throws IllegalArgumentException if {@code capacity <= 0}
   */
  public STMDeque(int capacity) {
    if (capacity <= 0)
      throw new IllegalArgumentException();
    head = STM.newRef(0);
    tail = STM.newRef(0);
    arrayRef = STM.newRef(STM.newTArray(capacity));

  }

  @Override
  public int size() {
    return STM.atomic(() ->
      tail.get() - head.get()
    );
  }

  @Override
  public void addFirst(E elem) {
      STM.atomic(() -> {

        int p = this.head.transformAndGet(i -> {

            if (i <= 0) {
              //Because we have increased the head, we have to move the tail accordingly
              //With this operation we basically move the origin on the number "line" to the array length
              //Allowing for use to add as many numbers as we want to the front, as when we reach 0 again, we will have ran out of space
              //In the array, and it will be expanded

              this.tail.transform(t -> t + this.arrayRef.get().length());

              return this.arrayRef.get().length() - 1;
            }

            return i - 1;
        });

        if (tail.get() - p < this.arrayRef.get().length()) {
            this.arrayRef.get().update(p % this.arrayRef.get().length(), elem);
        } else {

          TArray.View<E> newArray = STM.newTArray(this.arrayRef.get().length() * 2);

          for (int i = p + 1; i < tail.get(); i++) {

            newArray.update(i % newArray.length(), this.arrayRef.get().apply(i % this.arrayRef.get().length()));

          }

          newArray.update(p % newArray.length(), elem);

          this.arrayRef.set(newArray);
        }

      });
  }

  @Override
  public void addLast(E elem) {
    STM.atomic(() -> {

      int p = this.tail.get();

      if (p - head.get() < this.arrayRef.get().length()) {
          this.arrayRef.get().update(p % this.arrayRef.get().length(), elem);
      } else {

        TArray.View<E> newArray = STM.newTArray(this.arrayRef.get().length() * 2);

        for (int i = head.get(); i < p; i++) {

          newArray.update(i % newArray.length(), this.arrayRef.get().apply(i % this.arrayRef.get().length()));

        }

        newArray.update(p % newArray.length(), elem);

        this.arrayRef.set(newArray);
      }

      STM.increment(this.tail, 1);

    });
  }

  @Override
  public E removeFirst() {
    return STM.atomic(() -> {

        E elem = null;

        int p = head.get();

        STM.increment(head, 1);

        if (p < tail.get()) {
          int pos = p % this.arrayRef.get().length();

          elem = this.arrayRef.get().apply(pos);

          this.arrayRef.get().update(pos, null);

        } else {
          STM.retry();
        }

        return elem;

    });
  }

  @Override
  public E removeLast() {
    return STM.atomic(() -> {

      E elem = null;

      int p = this.tail.get() - 1;

      if (p >= head.get()) {
          int pos = p % this.arrayRef.get().length();

          elem = this.arrayRef.get().apply(pos);

          this.arrayRef.get().update(pos, null);

      } else {
        STM.retry();
      }

      STM.increment(this.tail, -1);

      return elem;
    });
  }

  /**
   * Test instantiation (do not run in cooperative mode).
   */
  public static final class Test extends BDequeTest {
    @Override
    <T> BDeque<T> createBDeque(int capacity) {
      return new STMDeque<>(capacity);
    }
  }


}
