import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ClientApp1 {

  private static final int INIT_NUM_REQUESTS = 10;
  private static final int NUM_REQUESTS = 1000;
  private static final int DELAY = 2;


  public static void main(String[] args) throws InterruptedException {

    validateArgs(args);

    int numThreads = Integer.parseInt(args[0]);
    int threadGroups = Integer.parseInt(args[1]);
    String path = args[2];

    CountDownLatch countDownLatchInitial = new CountDownLatch(numThreads);
    ExecutorService executorService = Executors.newFixedThreadPool(threadGroups);

    runInitial(executorService, path, numThreads, countDownLatchInitial);

    System.out.println("---FINISHED INITIAL THREADS---\n");

    System.out.println(
        "threadGroupSize = " + numThreads + ", numThreadGroups = " + threadGroups + ", delay = "
            + DELAY);

    CountDownLatch countdownLatchLoading = new CountDownLatch(numThreads * threadGroups);
    ConcurrentLinkedDeque<ResponseData> data = new ConcurrentLinkedDeque<>();

    long start = System.currentTimeMillis();

    runTrackedThreads(executorService, path, numThreads, threadGroups,
        countdownLatchLoading, data);

    long end = System.currentTimeMillis();

    double wallTime = (double) (end - start) / 1000;
    int totalRequests = numThreads * threadGroups * 1000 * 2;

    System.out.println();
    System.out.println("Total API Requests : " + totalRequests);

    System.out.println("Walltime : " + wallTime + " seconds");

    int throughput = (int) (totalRequests / wallTime);

    System.out.println("Throughput : " + throughput);

  }

  private static void validateArgs(String[] args) {
    if (args.length != 3) {
      System.exit(1);
    }

    int threads;
    int threadGroup;

    try {
      threads = Integer.parseInt(args[0]);
      threadGroup = Integer.parseInt(args[1]);
      if (threads <= 0 || threadGroup <= 0) {
        throw new NumberFormatException();
      }
    } catch (NumberFormatException e) {
      System.out.println("Invalid thread size and group given");
      System.exit(1);
    }
  }

  private static void runInitial(ExecutorService executorService, String path, int numThreads,
      CountDownLatch countDownLatch) throws InterruptedException {
    for (int i = 0; i < numThreads; i++) {
      executorService.execute(new ThreadLogic(path, INIT_NUM_REQUESTS, countDownLatch, null));
    }
    countDownLatch.await();
  }

  private static void runTrackedThreads(ExecutorService executorService, String path,
      int numThreads, int threadGroups, CountDownLatch countDownLatch,
      ConcurrentLinkedDeque<ResponseData> data) throws InterruptedException {

    for (int i = 0; i < threadGroups; i++) {
      executorService.execute(() -> {
        ExecutorService executorService2 = Executors.newFixedThreadPool(numThreads);
        for (int j = 0; j < numThreads; j++) {
          executorService2.execute(new ThreadLogic(path, NUM_REQUESTS, countDownLatch, data));
        }
        executorService2.shutdown();
      });
      Thread.sleep(DELAY * 1000);
    }
    countDownLatch.await();
    executorService.shutdown();
  }
}
