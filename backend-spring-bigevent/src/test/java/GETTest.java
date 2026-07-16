import cn.hutool.core.lang.UUID;
import org.junit.jupiter.api.Test;

public class GETTest {
    @Test
    public void test() {
        System.out.println("UUID.randomUUID() " + UUID.randomUUID().toString());
        System.out.println("UUID.randomUUID() " + UUID.fastUUID().toString());
        System.out.println("UUID.randomUUID() " + UUID.randomUUID(true).toString());
    }
}
