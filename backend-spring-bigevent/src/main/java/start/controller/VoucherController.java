package start.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import common.constant.JwtConstant;
import common.result.Result;
import common.util.ThreadLocalContextHolder;
import lombok.extern.slf4j.Slf4j;
import model.dto.VoucherDTO;
import model.entity.Voucher;
import model.entity.VoucherOrder;
import model.entity.VoucherSeckill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.VoucherOrderService;
import service.VoucherSeckillService;
import service.VoucherService;
import service.lock.RedisLock;
import service.id.RedisID;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/voucher")
@Slf4j
public class VoucherController {
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private VoucherSeckillService voucherSeckillService;
    @Autowired
    private VoucherOrderService voucherOrderService;
    @Autowired
    private RedisID redisID;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @PostMapping("/create")
    public Result createVoucher(@RequestBody VoucherDTO voucherDTO) {
        Voucher voucher = BeanUtil.toBean(voucherDTO, Voucher.class);
        voucherService.save(voucher);
        List<VoucherSeckill> voucherSeckillList = voucherDTO.getVoucherSeckillList().stream()
                .map((VoucherSeckill voucherSeckill) -> {return VoucherSeckill.builder()
                        .stock(voucherSeckill.getStock())
                        .beginTime(voucherSeckill.getBeginTime())
                        .endTime(voucherSeckill.getEndTime())
                        .voucherId(voucherDTO.getId())
                        .build();})
                .toList();
        for (int i = 0; i < voucherSeckillList.size(); i++) {
            stringRedisTemplate.opsForValue().set("voucherSeckill:stock:"+voucher.getId(),
                    voucherSeckillList.get(i).getStock().toString());
        }
        voucherSeckillService.saveBatch(voucherSeckillList);
        return Result.success("createVoucher");
    }

    @PostMapping("/pay")
    public synchronized Result payVoucher(@RequestBody VoucherOrder voucherOrder) {
        VoucherSeckill voucherSeckill = voucherSeckillService.getById(voucherOrder.getVoucherId());
        if(voucherSeckill == null){
            return Result.error("不存在");
        }
        if (LocalDateTime.now().isBefore(voucherSeckill.getBeginTime()) || LocalDateTime.now().isAfter(voucherSeckill.getEndTime())){
            return Result.error("不在规定时间段");
        }
        if(voucherSeckill.getStock() <= 0){
            return Result.error("结束了");
        }
        //并发
        voucherOrder.setId(redisID.createId("购买"));
        Map<String,Object> claims = ThreadLocalContextHolder.get();
        String currentId = claims.get(JwtConstant.ID).toString();
        Long userId = Long.parseLong(currentId);
        voucherOrder.setUserId(userId);
        RedisLock redisLock = new RedisLock(stringRedisTemplate,"pay"+userId);
        boolean start = redisLock.getLocked(10);
        if (!start){
            throw new RuntimeException("不要重复");
        }
        try {
            Result result = payVoucherSuccess(voucherOrder.getVoucherId());
            if (result.getCode() != 200){
                return Result.error("error");
            }
            voucherOrderService.save(voucherOrder);
        }finally {
            redisLock.unlook();
        }
        return Result.success("payVoucherSuccess");
    }
    @Transactional(rollbackFor = Exception.class)
    public Result payVoucherSuccess(Long voucherId) {
        Map<String,Object> claims = ThreadLocalContextHolder.get();
        String currentId = claims.get(JwtConstant.ID).toString();
        Long userId = Long.parseLong(currentId);
        Long count = voucherOrderService.count(new LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getUserId,userId)
                .eq(VoucherOrder::getVoucherId,voucherId));
        if (count > 0){
            throw new RuntimeException("一人一单");
        }
        //Atomicity
        boolean success = voucherSeckillService.lambdaUpdate()
                .eq(VoucherSeckill::getVoucherId, voucherId)
                .gt(VoucherSeckill::getStock, 0)
                .setSql("stock = stock - 1")
                .update();
        // 必须检查库存扣减是否成功，失败则返回错误，不创建订单
        if (!success) {
            throw new RuntimeException("库存不够");
        }
        return Result.success(voucherId);
    }

}
