package pc.bqueue;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.cooperari.CSystem;
import org.cooperari.config.CMaxTrials;
import org.cooperari.config.CRaceDetection;
import org.cooperari.config.CScheduling;
import org.cooperari.core.scheduling.CProgramStateFactory;
import org.cooperari.core.scheduling.CSchedulerFactory;
import org.cooperari.junit.CJUnitRunner;

import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.Random;

@SuppressWarnings("javadoc")
@RunWith(CJUnitRunner.class)
@CMaxTrials(25)
@CRaceDetection(false)
@CScheduling(schedulerFactory=CSchedulerFactory.MEMINI, stateFactory=CProgramStateFactory.RAW)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class BDequeTest {

    abstract <T> BDeque<T> createBDeque(int capacity);

    private static final Random random = new Random();

    private int populateRandom(BDeque<Integer> q, int quantity) {
      int sum = 0;
      for (int i = 0; i < quantity; i++) {
        int number = random.nextInt();
        if (random.nextBoolean()) {
          q.addFirst(number);
        } else {
          q.addLast(number);
        }

        sum += number;
      }

      return sum;
    }

    private int removeRandom(BDeque<Integer> q, int quantity) {
      int sum = 0;

      for (int i = 0; i < quantity; i++) {
        if (random.nextBoolean()) {
          sum += q.removeFirst();
        } else {
          sum += q.removeLast();
        }
      }

      return sum;
    }

    @Test
    public void test1() {
      BDeque<Integer> q = createBDeque(2);

      AtomicInteger a = new AtomicInteger(), b = new AtomicInteger(), c = new AtomicInteger(), d = new AtomicInteger();

      CSystem.forkAndJoin(
        () -> q.addFirst(1),
        () -> q.addLast(10),
        () -> q.addLast(100),
        () -> q.addFirst(1000)
      );

      assertEquals(4, q.size());

      CSystem.forkAndJoin(
        () -> a.set(q.size()),
        () -> b.set(q.size()),
        () -> c.set(q.size()),
        () -> d.set(q.size())
      );

      assertEquals(4, a.get());
      assertEquals(4, b.get());
      assertEquals(4, c.get());
      assertEquals(4, d.get());

      CSystem.forkAndJoin(
          () -> a.set(q.removeLast()),
          () -> b.set(q.removeLast()),
          () -> c.set(q.removeLast()),
          () -> d.set(q.removeLast())
        );

      assertEquals(0, q.size());
      assertEquals(1111, a.get() + b.get() + c.get() + d.get());
    }

    @Test
    public void test2() {

        BDeque<Integer> q = createBDeque(2);
        AtomicInteger a = new AtomicInteger(), b = new AtomicInteger(), c = new AtomicInteger(), d = new AtomicInteger();

        CSystem.forkAndJoin(
          () -> q.addFirst(1),
          () -> q.addLast(10),
          () -> q.addLast(100),
          () -> q.addFirst(1000)
        );

        assertEquals(4, q.size());

        CSystem.forkAndJoin(
          () -> a.set(q.size()),
          () -> b.set(q.size()),
          () -> c.set(q.size()),
          () -> d.set(q.size())
        );

        assertEquals(4, a.get());
        assertEquals(4, b.get());
        assertEquals(4, c.get());
        assertEquals(4, d.get());

        CSystem.forkAndJoin(
            () -> a.set(q.removeFirst()),
            () -> b.set(q.removeFirst()),
            () -> c.set(q.removeFirst()),
            () -> d.set(q.removeFirst())
          );

        assertEquals(0, q.size());
        assertEquals(1111, a.get() + b.get() + c.get() + d.get());
    }

    private void test3(int cap) {

      BDeque<Integer> q = createBDeque(cap);

      AtomicInteger a = new AtomicInteger(), b = new AtomicInteger();

      CSystem.forkAndJoin(
        () -> a.getAndAccumulate(populateRandom(q, cap), Integer::sum),
        () -> a.getAndAccumulate(populateRandom(q, cap), Integer::sum),
        () -> a.getAndAccumulate(populateRandom(q, cap), Integer::sum),
        () -> a.getAndAccumulate(populateRandom(q, cap), Integer::sum)
      );

      assertEquals(q.size(), cap * 4);

      CSystem.forkAndJoin(
        () -> b.getAndAccumulate(removeRandom(q, cap), Integer::sum),
        () -> b.getAndAccumulate(removeRandom(q, cap), Integer::sum),
        () -> b.getAndAccumulate(removeRandom(q, cap), Integer::sum),
        () -> b.getAndAccumulate(removeRandom(q, cap), Integer::sum)
      );

      assertEquals(a.get(), b.get());
      assertEquals(0, q.size());

    }

    @Test
    public void test3_1() {
      test3(1);
    }

    @Test
    public void test3_2() {
      test3(2);
    }
    @Test
    public void test3_3() {
      test3(3);
    }

    private void test4(int cap) {
      BDeque<Integer> q = createBDeque(cap);

      CSystem.forkAndJoin(
        () -> q.addFirst(1),
        () -> q.addFirst(10),
        () -> {
          assertEquals(11, q.removeFirst() + q.removeFirst());

          assertEquals(0, q.size());
        }
      );

      assertEquals(0, q.size());
    }

    @Test
    public void test4_1() {
      test4(1);
    }

    @Test
    public void test4_2() {
      test4(2);
    }

    private void test5(int cap) {

      BDeque<Integer> q = createBDeque(cap);

      CSystem.forkAndJoin(
        () -> q.addFirst(1),
        () -> q.addLast(10),
        () -> q.addLast(100),
        () -> {

          assertEquals(111, q.removeFirst() + q.removeLast() + q.removeLast());

          assertEquals(0, q.size());
        }
      );

      assertEquals(0, q.size());
    }

    @Test
    public void test5_1() {
      test5(1);
    }

    @Test
    public void test5_2() {
      test5(2);
    }

    private void test6(int capacity) {
      BDeque<Integer> q = createBDeque(capacity);

      AtomicInteger a = new AtomicInteger(), b = new AtomicInteger(), c = new AtomicInteger(), d = new AtomicInteger();
      CSystem.forkAndJoin(
        () -> a.set(populateRandom(q, capacity)),
        () -> b.set(removeRandom(q, capacity)),
        () -> c.set(populateRandom(q, capacity)),
        () -> d.set(removeRandom(q, capacity))
      );

      assertEquals(a.get() + c.get(), b.get() + d.get());
      assertEquals(0, q.size());
    }

    @Test
    public void test6_1() {
      test6(1);
    }

    @Test
    public void test6_2() {
      test6(2);
    }

    @Test
    public void test6_3() {
      test6(3);
    }

    private void test7(int capacity) {
      BDeque<Integer> q = createBDeque(capacity);

      AtomicInteger a = new AtomicInteger(),
        b = new AtomicInteger(),
        c = new AtomicInteger(),
        d = new AtomicInteger();

      CSystem.forkAndJoin(
        () -> {q.addFirst(1); q.addLast(10); q.addLast(100); q.addLast(1000); a.set(q.size()); },
        () -> {b.set(q.removeFirst()); c.set(q.removeLast()); d.set(q.size());}
      );

      assertEquals(1, b.get());
      assertTrue(a.get() >= 2 && a.get() <= 4);

      assertEquals(2, q.size());

      CSystem.forkAndJoin(
        () -> {q.addFirst(10000);},
        () -> {a.set(q.removeFirst()); b.set(q.removeLast()); c.set(q.size());}
      );

      assertEquals(1, q.size());

      CSystem.forkAndJoin(() -> {q.removeLast();});

      assertEquals(0, q.size());
    }

    @Test
    public void test7_1() {
      test7(1);
    }

    @Test
    public void test7_2() {
      test7(2);
    }

    @Test
    public void test7_5() {
      test7(5);
    }

    @Test
    public void test8() {
        BDeque<Integer> q = createBDeque(3);
        AtomicInteger a = new AtomicInteger();
        AtomicInteger b = new AtomicInteger();
        AtomicInteger c = new AtomicInteger();
        AtomicInteger d = new AtomicInteger();
        CSystem.forkAndJoin(
            () -> q.addFirst(1),
            () -> q.addFirst(10),
            () -> q.addFirst(100),
            () -> q.addFirst(1000),
            () -> a.set(q.removeFirst()),
            () -> b.set(q.removeLast()),
            () -> c.set(q.removeFirst()),
            () -> d.set(q.removeLast())
            );
        assertEquals(0, q.size());
        assertEquals(1111, a.get() + b.get() + c.get() + d.get());
    }

    @Test
    public void test9() {

      BDeque<Integer> q = createBDeque(3);

      AtomicInteger a = new AtomicInteger(), b = new AtomicInteger(),
        c = new AtomicInteger(), d = new AtomicInteger();

      CSystem.forkAndJoin(
        () -> {q.addLast(1); q.addLast(2); q.addLast(3);},
        () -> {a.set(q.removeFirst()); b.set(q.removeFirst()); c.set(q.removeFirst());}
      );

      assertEquals(0, q.size());

      assertEquals(1, a.get());
      assertEquals(2, b.get());
      assertEquals(3, c.get());

      CSystem.forkAndJoin(
        () -> {q.addFirst(1); q.addFirst(2); q.addFirst(3); q.addFirst(4); a.set(q.size());}
      );

      assertEquals(4, q.size());
      assertEquals(4, a.get());

      CSystem.forkAndJoin(
        () -> {a.set(q.removeLast()); b.set(q.removeLast()); c.set(q.removeLast()); d.set(q.removeLast());}
      );

      assertEquals(1, a.get());
      assertEquals(2, b.get());
      assertEquals(3, c.get());
      assertEquals(4, d.get());
      assertEquals(0, q.size());

    }

    @Test
    public void test10() {

      BDeque<Integer> q = createBDeque(3);

      AtomicInteger a = new AtomicInteger(), b = new AtomicInteger(),
        c = new AtomicInteger(), d = new AtomicInteger();

      CSystem.forkAndJoin(
        () -> {q.addLast(1); q.addLast(2); q.addLast(3);},
        () -> {a.set(q.removeLast()); b.set(q.removeLast()); c.set(q.removeLast());}
      );

      assertEquals(0, q.size());

      CSystem.forkAndJoin(
        () -> {q.addFirst(1); q.addFirst(2); q.addFirst(3); q.addFirst(4); a.set(q.size());}
      );

      assertEquals(4, q.size());
      assertEquals(4, a.get());

      CSystem.forkAndJoin(
        () -> {a.set(q.removeFirst()); b.set(q.removeFirst()); c.set(q.removeFirst()); d.set(q.removeFirst());}
      );

      assertEquals(4, a.get());
      assertEquals(3, b.get());
      assertEquals(2, c.get());
      assertEquals(1, d.get());
      assertEquals(0, q.size());

    }

    @Test
    public void test11() {
      BDeque<Integer> q = createBDeque(4);
      AtomicInteger a = new AtomicInteger();
      AtomicInteger b = new AtomicInteger();
      AtomicInteger c = new AtomicInteger();
      AtomicInteger d = new AtomicInteger();
      CSystem.forkAndJoin(
          () -> { q.addFirst(3); a.set(q.removeLast()); },
          () -> { b.set(q.removeLast()); },
          () -> { int n = q.size(); q.addFirst(n); q.addFirst(n + 1); }
          );
      assertEquals(1, q.size());

      int [][] abPossibilities = {
          { 3, 0},
          { 3, 3},
          { 0, 3},
          { 3, 1},
          { 1, 3},
          { 0, 1},
          { 1, 0}
      };
      boolean found = false;
      for (int[] p: abPossibilities) {
        if (a.get() == p[0] && b.get() == p[1]) {
          found = true;
          break;
        }
      }
      if (!found) {
        fail("Did you consider a = " + a.get() + " and  b=" + b.get() + " ?");
      }

      CSystem.forkAndJoin(
          () -> { c.set(q.removeLast());  },
          () -> { q.addFirst(a.get()); },
          () -> { int n = q.size(); q.addFirst(n); d.set(q.removeLast()); }
          );

      assertEquals(1, q.size());

      abPossibilities = new int[][]{
          { 0, 1},
          { 0, 3},
          { 1, 3},
          { 1, 0},
          { 1, 1},
          { 2, 3},
          { 2, 0},
          { 2, 1},
          { 3, 0},
          { 3, 1},
          { 3, 2}
      };

      found = false;
      for (int[] p: abPossibilities) {
        if (c.get() == p[0] && d.get() == p[1]) {
          found = true;
          break;
        }
      }

      if (!found) {
        fail("Did you consider c = " + c.get() + " and  d=" + d.get() + " ?");
      }
    }
}
