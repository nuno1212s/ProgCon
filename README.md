# ProgCon

A work for Concurrent Programming that implements several data structures like Queues and Deques.



This program uses several implementations like Atomic variables, Locks and monitors and Scala STM (https://nbronson.github.io/scala-stm/). All implementations are described in the appended report, however the report is in Portuguese.

There is also an implementation of a concurrent web crawler that uses ForkJoinPools to be able to download all files concurrently. This implementation uses ConcurrentHashMaps and ConcurrentQueues to be able to control downloaded files and on going tasks.

To execute the tests you must download Cooperari (https://github.com/Cooperari/cooperari). This library allows for cooperative and preemptive testing to assure correct execution of the implementations.

To run the Web crawler, first compile and run the corresponding classes (WebServer, SequencialCrawler and ConcurrentCrawler). To run the WebServer, you must give it a folder with HTML files as the first argument, the port as the second argument (Default port is 8123) and the threads as the last argument.

The Sequencial Crawler's first argument indicates the ip and port of the server (Default ip and port is http://127.0.0.1:8123/). The Concurrent Crawler's first argument indicates the amount of threads to run it with and the second arguments indicates the ip and port of the server with the same default as the Sequencial Crawler.
