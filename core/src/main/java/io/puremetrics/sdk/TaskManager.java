/**
 * Modified MIT License
 * <p/>
 * Copyright 2016 PureMetrics
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * 1. The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * 2. All copies of substantial portions of the Software may only be used in connection
 * with services provided by PureMetrics.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.puremetrics.sdk;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

final class TaskManager {

  private static TaskManager _INSTANCE;
  // A queue of Runnables
  private final BlockingQueue<Runnable> workerQueue;
  private ThreadPoolExecutor workerPool;

  private TaskManager() {
    // Instantiates the queue of Runnables as a LinkedBlockingQueue
    workerQueue = new LinkedBlockingQueue<>();
    //Sets the amount of time an idle thread waits before terminating
    final int KEEP_ALIVE_TIME = 1;
    final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    // Sets the Time Unit to seconds
    TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    // Creates a thread pool manager
    workerPool = new ThreadPoolExecutor(
            NUMBER_OF_CORES,       // Initial pool size
            NUMBER_OF_CORES,       // Max pool size
            KEEP_ALIVE_TIME,
            KEEP_ALIVE_TIME_UNIT,
            workerQueue);
    PureMetrics.log(PureMetrics.LOG_LEVEL.DEBUG, "Initialized workers: " + NUMBER_OF_CORES);
  }

  synchronized static TaskManager getInstance() {
    if (null == _INSTANCE) {
      _INSTANCE = new TaskManager();
    }
    return _INSTANCE;
  }

  /**
   * Executes a task on the internal {@link ThreadPoolExecutor}
   * @param task An instance of {@link Runnable} which represents the task
   */
  void executeTask(Runnable task) {
    try {
      workerPool.submit(task);
    } catch (Throwable e) {
      PureMetrics.log(PureMetrics.LOG_LEVEL.ERROR, "TaskManager:executeTask", e);
    }

  }

  /**
   * Shutsdown the internal {@link ThreadPoolExecutor} immediately
   */
  public void shutdown() {
    PureMetrics.log(PureMetrics.LOG_LEVEL.DEBUG, "Received Shutdown request");
    printManagerStatus();
    workerPool.shutdown();
  }

  /**
   * Prints the Status of the TaskManager
   */
  private void printManagerStatus() {
    PureMetrics.log(PureMetrics.LOG_LEVEL.DEBUG, "Tasks in queue: " + workerQueue.size()
            + " Active Threads: " + workerPool.getActiveCount());
  }

  void warmup() {
    workerPool.prestartCoreThread();
  }
}
