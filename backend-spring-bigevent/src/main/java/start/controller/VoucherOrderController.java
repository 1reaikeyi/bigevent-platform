//package start.controller;
//
//import common.ThreadLocalContext.ThreadLocalParam;
//import common.result.Result;
//import jakarta.annotation.PostConstruct;
//import jakarta.annotation.PreDestroy;
//import lombok.extern.slf4j.Slf4j;
//import model.entity.VoucherOrder;
//import org.redisson.api.RLock;
//import org.redisson.api.RedissonClient;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.data.redis.connection.stream.Consumer;
//import org.springframework.data.redis.connection.stream.StreamOffset;
//import org.springframework.data.redis.connection.stream.StreamReadOptions;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.data.redis.core.script.DefaultRedisScript;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import service.VoucherOrderService;
//import service.id.RedisID;
//import start.aspect.Info;
//
//import java.util.List;
//import java.util.concurrent.*;
//
///**
// * 优惠券订单控制器 - 异步处理秒杀订单
// */
//@RestController
//@RequestMapping("/voucherOrder")
//@Slf4j
//public class VoucherOrderController {
//    @Autowired
//    private VoucherOrderService voucherOrderService;
//    @Autowired
//    private RedisID redisID;
//    @Autowired
//    private RedissonClient redissonClient;
//    @Autowired
//    private StringRedisTemplate stringRedisTemplate;
//
//    // Lua脚本：校验库存和重复下单
//    private static final DefaultRedisScript<Long> REDIS_SCRIPT = new DefaultRedisScript<>();
//    static {
//        REDIS_SCRIPT.setLocation(new ClassPathResource("redis-seckill.lua"));
//        REDIS_SCRIPT.setResultType(Long.class);
//    }
//
//    // 秒杀订单处理线程池 - 单线程确保顺序处理
//    private static final ExecutorService SCEKILL_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
//        Thread t = new Thread(r, "seckill-order-handler");
//        t.setDaemon(true);
//        return t;
//    });
//
//    /**
//     * 初始化方法 - 启动异步订单处理线程
//     */
//    @PostConstruct
//    public void init() {
//        SCEKILL_EXECUTOR.submit(new HandleOrderTask());
//    }
//
//    @PreDestroy
//    public void destroy() {
//        SCEKILL_EXECUTOR.shutdown();
//        try {
//            // 等待10秒让未处理的订单完成
//            if (!SCEKILL_EXECUTOR.awaitTermination(10, TimeUnit.SECONDS)) {
//                SCEKILL_EXECUTOR.shutdownNow();
//            }
//        } catch (InterruptedException e) {
//            SCEKILL_EXECUTOR.shutdownNow();
//            Thread.currentThread().interrupt();
//        }
//    }
//
//    @Info(desc = "异步秒杀下单")
//    @PostMapping("/pay")
//    public Result redisproLock(@RequestBody VoucherOrder voucherOrder) {
//        // 获取当前登录用户ID
//        Long userId = ThreadLocalParam.getUserId();
//        // 生成订单ID
//        Long orderId = redisID.createId("order");
//        // 执行Lua脚本：校验库存和重复下单
//        Long result = stringRedisTemplate.execute(REDIS_SCRIPT,
//                List.of(),
//                voucherOrder.getVoucherId().toString(),
//                userId.toString(),
//                orderId.toString());
//
//        // 脚本返回非0表示失败
//        if (result != 0) {
//            return Result.error(result == 1 ? "库存不够" : "重复下单");
//        }
//        return Result.success(orderId);
//    }
//
//    /**
//     * 异步订单处理任务
//     * 从队列中取出订单，完成扣减库存和保存订单操作
//     */
//    private class HandleOrderTask implements Runnable {
//        @Override
//        public void run() {
//            while (true) {
//               stringRedisTemplate.opsForStream().read(
//                       Consumer.from(),
//                       StreamReadOptions.empty().count().block(),
//                       StreamOffset.create());
//            }
//        }
//    }
//
//}
