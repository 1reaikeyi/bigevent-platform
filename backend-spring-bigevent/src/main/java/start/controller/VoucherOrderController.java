package start.controller;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.VoucherOrderService;
import service.VoucherSeckillService;
import service.id.RedisID;

/**
 * 异步处理
 */
@RestController
@RequestMapping("/voucherOrder")
public class VoucherOrderController {
    @Autowired
    private VoucherSeckillService voucherSeckillService;
    @Autowired
    private VoucherOrderService voucherOrderService;
    @Autowired
    private RedisID redisID;
    @Autowired
    private RedissonClient redissonClient;
}
