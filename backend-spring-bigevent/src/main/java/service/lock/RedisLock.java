package service.lock;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
@NoArgsConstructor
public class RedisLock implements ILock{
    private StringRedisTemplate stringRedisTemplate;
    private String name;
    private static final String prefix = "lock:";

    /**
     * 获取锁
     *
     * @param timeoutSec
     * @return
     */
    @Override
    public boolean getLocked(long timeoutSec) {
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(prefix+name, Thread.currentThread().getName(),timeoutSec, TimeUnit.SECONDS);
        return Optional.ofNullable(success).orElse(false);
    }

    /**
     * 释放锁
     */
    @Override
    public void unlook() {
        stringRedisTemplate.delete(prefix+name);
    }
}
