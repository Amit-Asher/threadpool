package core;

import java.util.ArrayList;
import java.util.List;

public class ThreadPool {

    /**
     * The list of threads in the thread pool
     */
    private final List<ReusableThread> threads = new ArrayList<>();

    /**
     * The list of tasks in the queue.
     * the threads will take the tasks from the queue (i.e poll, not push)
     */
    private final List<Runnable> queue = new ArrayList<>();

    /**
     * The maximum number of threads in the thread pool
     */
    private final int maxThreads;

    /**
     * If a thread is idle longer than maxIdleTimeInMs, then it will be stopped
     * (making the thread pool to be dynamic)
     */
    private final int maxIdleTimeInMs;

    /**
     * The flag to indicate if the thread pool is shutdown
     */
    private boolean isShutdown = false;

    class ReusableThread extends Thread {
        /**
         * The maximum idle time in milliseconds
         * If the thread is idle for longer than maxIdleTimeInMs, then it will be stopped
         */
        public final int maxIdleTimeInMs;

        /**
         * The flag to indicate if the thread is busy
         */
        public boolean isBusy;

        /**
         * The last time the thread is used
         * If the thread is idle longer then maxIdleTimeInMs, then it will be stopped
         */
        public long lastTimeUsed;

        public ReusableThread(int maxIdleTimeInMs) {
            this.maxIdleTimeInMs = maxIdleTimeInMs;
            this.isBusy = false;
            this.lastTimeUsed = System.currentTimeMillis();
            this.setName("ReusableThread-" + java.util.UUID.randomUUID());
            // the default is already false, but just to make it clear.
            // the thread is not a daemon thread for graceful shutdown.
            this.setDaemon(false);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Runnable task = null;

                    // get task from the queue
                    synchronized (queue) {
                        if (!queue.isEmpty()) {
                            task = queue.remove(0);
                        }
                    }

                    // if there is a task, run it
                    if (task != null) {
                        // set the thread to be busy
                        isBusy = true;
                        // run the task
                        task.run();
                        // update the last timestamp when the task is finished
                        this.lastTimeUsed = System.currentTimeMillis();
                        // set the thread to be idle
                        isBusy = false;
                    }

                    // if the thread is idle for a long time, stop the thread
                    if (System.currentTimeMillis() - lastTimeUsed > maxIdleTimeInMs) {
                        System.out.println(java.time.LocalTime.now() + " " + this.getName() + " is timed out");
                        break;
                    }

                    // if the thread pool was shutdown and the queue is empty, stop the thread
                    if (isShutdown && queue.isEmpty()) {
                        System.out.println(java.time.LocalTime.now() + " " + this.getName() + " is drowned");
                        break;
                    }

                } catch (Exception ignored) {
                }
            }
        }
    }

    public ThreadPool(int maxThreads, int maxIdleTimeInMs) {
        this.maxThreads = maxThreads;
        this.maxIdleTimeInMs = maxIdleTimeInMs;
        // pre-populate the thread pool
        for (int i = 0; i < maxThreads; i++) {
            ReusableThread thread = new ReusableThread(maxIdleTimeInMs);
            threads.add(thread);
            thread.start();
        }
    }

    private boolean allThreadsAreBusy() {
        for (ReusableThread thread : threads) {
            if (!thread.isBusy) {
                return false;
            }
        }
        return true;
    }

    public void execute(Runnable runnable) {
        // if the thread pool is shutdown, do not accept new tasks
        if (isShutdown) {
            return;
        }

        // if all threads are busy and the number of threads is less
        // than the maximum, then create a new thread
        // lock it to prevent from threads exceeded the maximum
        synchronized (threads) {
            if (allThreadsAreBusy() && maxThreads > threads.size()) {
                ReusableThread thread = new ReusableThread(this.maxIdleTimeInMs);
                threads.add(thread);
                thread.start();
            }
        }

        synchronized (queue) {
            queue.add(runnable);
        }
    }

    /**
     * 1. stop accepting new tasks
     * 2. wait for the current tasks to finish
     * 3. stop all threads
     */
    public void shutdown() {
        isShutdown = true;
        for (ReusableThread thread : threads) {
            try {
                thread.join();
            } catch (Exception err) {
            }
        }
        System.out.println(java.time.LocalTime.now() + " All threads are drowned");
    }
}
