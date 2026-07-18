import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.junit.jupiter.api.Test;

public class GETTest {
    @Test
    public void test() {
        System.out.println("UUID.randomUUID() " + UUID.randomUUID().toString());
        System.out.println("BooleanUtil.isFalse(1==1) = " + BooleanUtil.isFalse(1==1));
        System.out.println("BooleanUtil.isTrue(1==1) = " + BooleanUtil.isTrue(1==1));
    }
}
