Linked Blocking Multi Queue
===========================

An optionally-bounded blocking "multi-queue" based on linked nodes. A multi-queue is actually a set of queues that are
connected at the heads and have independent tails (the head of the queue is that element that has been on the queue the
longest time, the tail of the queue is that element that has been on the queue the shortest time). New elements are
added at the tail of one of the queues, and the queue retrieval operations obtain elements from the head of some of the
queues, according to a policy that is described below.

This class essentially allows a consumer to efficiently block a single thread on a set of queues, until one becomes
available. The special feature is that individual queues can be enabled or disabled. A disabled queue is not
considered for polling (in the event that all the queue are disabled, any blocking operation would do so trying to
read, as if all the queues were empty). Elements are taken from the set of enabled queues, obeying the established
priority (queues with the same priority are served round robin).

A disabled queue accepts new elements normally until it reaches the maximum capacity (if any).

Individual queues can be added, removed, enabled or disabled at any time.

The optional capacity bound constructor argument serves as a way to prevent excessive queue expansion. The capacity, if
unspecified, is equal to `Int.MaxVaue`. Linked nodes are dynamically created upon each insertion unless this would bring
the queue above capacity.
 
Not being actually a linear queue, this class does not implement the `Collection` or `Queue` interfaces. The traditional
queue interface is split in the traits: `Offerable` and `Pollable`. Sub-queues do however implement Collection.

Example
-------

Sub queues are created from the multi queue:

	LinkedBlockingMultiQueue<Int, String> q = new LinkedBlockingMultiQueue<Int, String>();
	LinkedBlockingMultiQueue<Int, String>.SubQueue sq1 = q.addSubQueue(1 /* key*/, 10 /* priority */);
	LinkedBlockingMultiQueue<Int, String>.SubQueue sq2 = q.addSubQueue(2 /* key*/, 10 /* priority */);

Then it is possible to offer and poll:

	sq1.offer("x1");
	q.poll(); // "x1"
	sq2.offer("x2");
	q.poll(); // "x2"

API
---

Browse [API documentation](http://www.javadoc.io/doc/com.github.marianobarrios/lbmq/) for the most recent release.

Implementation notes
--------------------

This implementation is inspired by the
[LinkedBlockingQueue](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/LinkedBlockingQueue.html), made by
Doug Lea with assistance from members of [JCP JSR-166 Expert Group](https://jcp.org/en/jsr/detail?id=166).
 
Each sub-queue uses, as does the `LinkedBlockingQueue`, a variant of the "two lock queue" algorithm. The `putLock` gates entry 
to `put` (and `offer`), and has an associated condition for waiting puts. The `takeLock`, on the other hand, is unique and 
shared among all the sub-queues.

Each subqueue has a "count" field, that is maintained as an atomic to avoid needing to get both locks in most cases.
Also, to minimize need for puts to get takeLock and vice-versa, cascading notifies are used. When a put notices that it
has enabled at  least one take, it signals taker. That taker in turn signals others if more items have been entered
since the signal. And symmetrically for takes signaling puts.

The possibility of disabling sub-queues introduces the necessity of an additional centralized atomic count field, which
is also updated in every operation and represents, at any time, how many elements can be taken before exhausting the
queue.
     
Operations such as `remove(Object)` and iterators acquire both the corresponding putLock and the takeLock.
     
Visibility between writers and readers is provided as follows:
 
Whenever an element is enqueued, the `putLock` is acquired and count updated. A subsequent reader guarantees visibility 
to the enqueued Node by either acquiring the `putLock` (via `fullyLock`) or by acquiring the `takeLock`, and then reading 
`n = count.get()`; this gives visibility to the first `n` items.
    
To implement weakly consistent iterators, it appears we need to keep all Nodes GC-reachable from a predecessor dequeued 
Node. That would cause two problems:

 - allow a rogue Iterator to cause unbounded memory retention
 
 - cause cross-generational linking of old Nodes to new Nodes if a Node was tenured while live, which generational GCs 
 have a hard time dealing with, causing repeated major collections. However, only non-deleted Nodes need to be reachable 
 from dequeued Nodes, and reachability does not necessarily have to be of the kind understood by the GC. We use the 
 trick of linking a Node that has just been dequeued to itself. Such a self-link implicitly means to advance to 
 `head.next`.
