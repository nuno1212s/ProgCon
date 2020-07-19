package pc.crawler;


import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import java.net.MalformedURLException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Future;
import java.util.LinkedList;
import java.util.concurrent.*;

import pc.util.UnexpectedException;

/**
 * Concurrent crawler.
 *
 */
public class ConcurrentCrawler extends SequentialCrawler {

  //These variables don't have to be static as the TransferTask is not static and therefore
  //All instances of TransferTask will store a reference to it's parent ConcurrentCrawler
  //Which means that these variables are "global" for all TransferTask instances
  private final Set<String> visited = ConcurrentHashMap.newKeySet();

  private final ConcurrentLinkedQueue<Future<Void>> futures = new ConcurrentLinkedQueue<>();

  private final AtomicInteger ridCounter = new AtomicInteger(0);

  public static void main(String[] args) throws IOException {
    int threads = args.length > 0 ?  Integer.parseInt(args[0]) : 4;
    String url = args.length > 1 ? args[1] : "http://localhost:8123";

    ConcurrentCrawler cc = new ConcurrentCrawler(threads);
    cc.setVerboseOutput(false);
    cc.crawl(url);
    cc.stop();
  }

  /**
   * The fork-join pool.
   */
  private final ForkJoinPool pool;

  /**
   * Constructor.
   * @param threads number of threads.
   * @throws IOException if an I/O error occurs
   */
  public ConcurrentCrawler(int threads) throws IOException {
    pool = new ForkJoinPool(threads);

    log("Running with %d threads.", threads);
  }

  @Override
  public void crawl(String root) {
    long t = System.currentTimeMillis();
    log("Starting at %s", root);
    visited.add(root);
    pool.invoke(new TransferTask(ridCounter.getAndIncrement(), root));

    Future<Void> f = null;
    while ((f = futures.poll()) != null) {
      try {
        f.get();
      } catch (Exception e ){
        throw new UnexpectedException(e);
      }
    }

    t = System.currentTimeMillis() - t;
    log("Done %d transfers in %d ms", ridCounter.get(), t);
  }

  /**
   * Stop the crawler.
   */
  public void stop() {
    pool.shutdown();
  }

  @SuppressWarnings("serial")
  private class TransferTask extends RecursiveTask<Void> {

    final int rid;
    final String path;

    TransferTask(int rid, String path) {
      this.rid = rid;
      this.path = path;
    }

    @Override
    protected Void compute() {
      try {

        URL url = new URL(path);

        List<String> links = performTransfer(rid, url);

          links.forEach((link) -> {

            try {
              String newURL = new URL(url, new URL(url, link).getPath()).toString();

              //We use add, as the add from the ConcurrentHashSet is based on the putIfAbsent from ConcurrentHashMap
              //Which is atomic, so we know that this operation will be atomic, so in case of there not being
              //A visited, we won't have the problem of checking, and between checking and adding, another
              //Thread has already come along and registered the visited URL.
              if (visited.add(newURL)) {
                //Add to the visited even before visiting as we don't want to have various tasks created
                //Just because the previous has not started yet.
                Future<Void> future = pool.submit(new TransferTask(ridCounter.getAndIncrement(), newURL));

                futures.add(future);
              }
          } catch (MalformedURLException e) {
            e.printStackTrace();
          }

        });

      } catch (Exception e) {
        throw new UnexpectedException(e);
      }

      return null;
    }

  }
}
