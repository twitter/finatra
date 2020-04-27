package com.twitter.finatra.example;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Queue implements Iterable<String> {

  private final int max;
  private final ConcurrentLinkedQueue<String> underlying;

  public Queue(int max) {
    this.max = max;
    this.underlying = new ConcurrentLinkedQueue<>();
  }

  /**
   * Poll the queue
   * @return The result {@link Optional} result of {@link ConcurrentLinkedQueue#poll()}, where
   *         the {@link Optional} result will be defined if the result of polling is non-null or
   *         {@link Optional#empty()} if the result is null.
   */
  public Optional<String> poll() {
    return Optional.ofNullable(underlying.poll());
  }

  /**
   * Current size of this queue
   * @return The current size of the underlying queue
   */
  public int size() {
    return underlying.size();
  }

  /**
   * Add the value to this queue
   * @param value The String to be added to the queue
   * @return Throws an {@link IllegalArgumentException} if adding the specified value would
   *         cause this {@link Queue} to grow past its max size. Otherwise, the result of
   *         calling {@link ConcurrentLinkedQueue#add(Object)} on the underlying queue.
   */
  public boolean add(String value) {
    if (size() == max - 1) {
      throw new IllegalArgumentException("Queue will overflow max size.");
    } else {
      return underlying.add(value);
    }
  }

  @Override
  public Iterator<String> iterator() {
    return underlying.iterator();
  }

}
