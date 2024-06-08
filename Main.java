import core.ThreadPool;

public class Main {
    public static void main(String[] args) {
        int maxThreads = 15;
        int maxIdleTimeInMs = 1 * 1000;
        int sleepTime = 5 * 1000;

        ThreadPool threadPool = new ThreadPool(maxThreads, maxIdleTimeInMs);

        System.out.println("pool configuration: maxThreads=" + maxThreads + ", maxIdleTimeInMs=" + maxIdleTimeInMs);
        System.out.println("Starting 10 tasks of " + sleepTime + "ms each...");
        for (int i = 0; i < 10; i++) {
            final int number = i;
            threadPool.execute(() -> {
                try {
                    System.out.println(java.time.LocalTime.now() + " - Task " + number + " started");
                    Thread.sleep(sleepTime);
                    System.out.println(java.time.LocalTime.now() + " - Task " + number + " finished");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        threadPool.shutdown();
    }
}
