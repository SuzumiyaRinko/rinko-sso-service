package suzumiya;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import suzumiya.constant.RedisConst;

import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@MapperScan(basePackages = "suzumiya.mapper")
@Slf4j
public class TestTest {

    @Autowired
    public RedisTemplate<String, Object> redisTemplate;

    @Test
    void test0() {
        Set<Object> s = redisTemplate.opsForSet().members(RedisConst.APPS_KEY);

//        List<AppDescription> appDescriptions = l.stream().map((map) -> {
//            AppDescription appDescription = new AppDescription();
//            BeanUtil.fillBeanWithMap((Map)map, appDescription, null);
//            return appDescription;
//        }).collect(Collectors.toList());

        for (Object appDescription : s) {
            System.out.println(appDescription);
        }
    }
}
