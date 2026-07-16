import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import service.id.RedisID;
import start.BigEventApplication;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest(classes = BigEventApplication.class)
public class RedisTest {
    @Autowired
    private RedisID redisID;

    // 10个线程池
    private final ExecutorService pool = Executors.newFixedThreadPool(10);

    @Test
    public void test() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(10);
        // 记录开始时间
        long start = System.currentTimeMillis();
        // 每个线程循环生成ID
        for (int i = 0; i < 10; i++) {
            pool.submit(() -> {
                // 持续生成直到超过1秒
                while (System.currentTimeMillis() - start < 1000) {
                    long a = redisID.createId("order");
                    System.out.println(a);
                }
                latch.countDown();
            });
        }
        latch.await();
        System.out.println("10线程1秒内生成订单ID完成");
        pool.shutdown();
    }
}

