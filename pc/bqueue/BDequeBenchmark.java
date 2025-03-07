package pc.bqueue;

import java.util.concurrent.ThreadLocalRandom;

import pc.util.Benchmark;
import pc.util.Benchmark.BThread;

/**
 * Benchmark program for stack implementations.
 */
public class BDequeBenchmark {

  private static final int DURATION = 5;
  private static final int MAX_THREADS = 32;

  /**
   * Program to run a benchmark over queue implementations.
   * @param args Arguments are ignored.
   */
  public static void main(String[] args) {
    //double serial = runBenchmark(1, new UStack<Integer>());

    for (int t = 2; t <= MAX_THREADS; t = t * 2) {
      runBenchmark("Lock-free backoff=y", t, new LFDeque<Integer>(MAX_THREADS,true));
      runBenchmark("Lock-free backoff=n", t, new LFDeque<Integer>(MAX_THREADS,false));
      runBenchmark("STM", t, new STMDeque<Integer>(MAX_THREADS));
    }
  }

  private static void runBenchmark(String desc, int threads, BDeque<Integer> q) {
    Benchmark b = new Benchmark(threads, DURATION, new BDequeOperation(q));
    System.out.printf("%2d,%20s,%11s -> ", threads, desc, q.getClass().getSimpleName());
    System.out.printf("%10.2f thousand ops/s per thread%n", b.run());
  }

  private static class BDequeOperation implements Benchmark.Operation {
    private final BDeque<Integer> queue;

    BDequeOperation(BDeque<Integer> q) {
      this.queue = q;
    }

    public void teardown() {
      for (int i = 0 ; i <= MAX_THREADS; i++) {
        queue.addLast(i);
      }
      while (queue.size() > 0) {
        queue.removeFirst();
      }
    }

    @Override
    public void step() {
      BThread t = (Benchmark.BThread) Thread.currentThread();
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      int role = t.getTId() % 2;
      if (role == 0) {
        int v = rng.nextInt(100);
        queue.addFirst(v);
      } else if (queue.size() > MAX_THREADS){
        queue.removeLast();
      }
      //Thread.yield();

    }
  }
}
