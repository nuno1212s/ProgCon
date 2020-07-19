# ProgCon
2
A work for Concurrent Programming that implements several data structures like Queues and Deques.
3
​
4
This program uses several implementations like Atomic variables, Locks and monitors and Scala STM (https://nbronson.github.io/scala-stm/). All implementations are described in the report described, however the report is in Portuguese.
5
There is also an implementation of a concurrent web crawler that uses ForkJoinPools to be able to download all files concurrently. This implementation uses ConcurrentHashMaps and ConcurrentQueues to be able to control downloaded files and on going tasks.

To execute the tests you must download Cooperari (https://github.com/Cooperari/cooperari). This library allows for cooperative and preemptive testing to assure correct execution of the implementations.
