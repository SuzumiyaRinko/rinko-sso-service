package suzumiya;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import suzumiya.constant.RedisConst;
import suzumiya.model.pojo.AppDescription;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@MapperScan(basePackages = "suzumiya.mapper")
public class Application {

    public static String[] iconUrls;
    public static String[] appNames;
    public static String[] appUrls;
    public static Boolean[] isAvailables;
    private static RedisTemplate<String, Object> redisTemplate;

    public static void main(String[] args) {
        ConfigurableApplicationContext ioc = SpringApplication.run(Application.class, args);

        // 获取Bean
        redisTemplate = ioc.getBean("redisTemplate", RedisTemplate.class);

        // init
        int len = iconUrls.length - 1;
        List<AppDescription> appDescriptions = new ArrayList<>();
        for (int i = 0; i <= len - 1; i++) {
            AppDescription t = new AppDescription(iconUrls[i], appNames[i], appUrls[i], isAvailables[i]);
            appDescriptions.add(t);
        }
        redisTemplate.delete(RedisConst.APPS_KEY);
        redisTemplate.opsForList().rightPushAll(RedisConst.APPS_KEY, appDescriptions.toArray());

        System.out.println("rinko-sso succeeded.");
    }

    @Value("${app-description.iconUrls}")
    public void setIconUrls(String[] iconUrls) {
        Application.iconUrls = iconUrls;
    }

    @Value("${app-description.appNames}")
    public void setAppNames(String[] appNames) {
        Application.appNames = appNames;
    }

    @Value("${app-description.appUrls}")
    public void setAppUrls(String[] appUrls) {
        Application.appUrls = appUrls;
    }

    @Value("${app-description.isAvailables}")
    public void setIsAvailables(Boolean[] isAvailables) {
        Application.isAvailables = isAvailables;
    }
}
