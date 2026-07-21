package start.controller;

import common.ThreadLocalContext.ThreadLocalParam;
import common.result.Result;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import model.entity.VoucherOrder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.VoucherOrderService;
import service.id.RedisID;
import start.aspect.Info;

import java.util.List;
import java.util.concurrent.*;

/**
 * 优惠券订单控制器 - 异步处理秒杀订单
 */
@RestController
@RequestMapping("/voucherOrder")
@Slf4j
public class VoucherOrderController {
    @Autowired
    private VoucherOrderService voucherOrderService;
    @Autowired
    private RedisID redisID;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // Lua脚本：校验库存和重复下单
    private static final DefaultRedisScript<Long> REDIS_SCRIPT = new DefaultRedisScript<>();
    static {
        REDIS_SCRIPT.setLocation(new ClassPathResource("redis-seckill.lua"));
        REDIS_SCRIPT.setResultType(Long.class);
    }

    // 异步订单处理队列 - 存储完整的VoucherOrder对象（修复：之前存储的是Long类型）
    private BlockingQueue<VoucherOrder> orderQueue = new ArrayBlockingQueue<>(128 * 1024);

    // 秒杀订单处理线程池 - 单线程确保顺序处理
    private static final ExecutorService SCEKILL_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "seckill-order-handler");
        t.setDaemon(true);
        return t;
    });

    /**
     * 初始化方法 - 启动异步订单处理线程
     */
    @PostConstruct
    public void init() {
        SCEKILL_EXECUTOR.submit(new HandleOrderTask());
    }

    @PreDestroy
    public void destroy() {
        SCEKILL_EXECUTOR.shutdown();
        try {
            // 等待10秒让未处理的订单完成
            if (!SCEKILL_EXECUTOR.awaitTermination(10, TimeUnit.SECONDS)) {
                SCEKILL_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            SCEKILL_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Info(desc = "异步秒杀下单")
    @PostMapping("/pay")
    public Result redisproLock(@RequestBody VoucherOrder voucherOrder) {
        // 获取当前登录用户ID
        Long userId = ThreadLocalParam.getUserId();
        if (userId == null) {
            return Result.error("用户未登录");
        }

        // 执行Lua脚本：校验库存和重复下单
        Long result = stringRedisTemplate.execute(REDIS_SCRIPT,
                List.of(),
                voucherOrder.getVoucherId().toString(), userId.toString());

        // 脚本返回非0表示失败
        if (result != 0) {
            return Result.error(result == 1 ? "库存不够" : "重复下单");
        }

        // 生成订单ID
        long orderId = redisID.createId("order");

        // 设置订单信息（在入队前设置所有必要字段）
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setStatus(1L);
        
        // 将订单放入异步队列（异步处理扣库存和保存订单）
        boolean offer = orderQueue.offer(voucherOrder);
        if (!offer) {
            return Result.error("系统繁忙，请稍后重试");
        }

        return Result.success(orderId);
    }

    /**
     * 异步订单处理任务
     * 从队列中取出订单，完成扣减库存和保存订单操作
     */
    private class HandleOrderTask implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    // 从队列中取出订单（阻塞等待）
                    VoucherOrder voucherOrder = orderQueue.take();

                    // 从订单对象中获取userId，不再依赖ThreadLocal
                    Long userId = voucherOrder.getUserId();
                    Long voucherId = voucherOrder.getVoucherId();

                    // 使用Redisson分布式锁防止重复处理
                    RLock redisLock = redissonClient.getLock("redisson:voucherSeckill:" + userId + ":" + voucherId);
                    boolean locked = false;
                    try {
                        // 尝试获取锁，等待5秒，持有10秒
                        locked = redisLock.tryLock(5, 10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        continue;
                    }

                    if (!locked) {
                        continue;
                    }

                    try {
                        // 执行实际的下单逻辑
                        voucherOrderService.paySuccess(voucherOrder);
                    } finally {
                        // 确保锁被释放
                        if (locked && redisLock.isHeldByCurrentThread()) {
                            redisLock.unlock();
                        }
                    }

                } catch (InterruptedException e) {
                    // 线程被中断，退出循环
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("订单处理线程异常: " + e.getMessage());
                }
            }
        }
    }

}
