package suzumiya.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import suzumiya.constant.RedisConst;
import suzumiya.model.pojo.AppDescription;
import suzumiya.model.vo.BaseResponse;
import suzumiya.util.ResponseGenerator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/apps")
public class AppsController {

    @Autowired
    public RedisTemplate<String, Object> redisTemplate;

    @GetMapping
    public BaseResponse<List<AppDescription>> getApps() {
        List<Object> l = redisTemplate.opsForList().range(RedisConst.APPS_KEY, 0, -1);

        if (CollectionUtil.isEmpty(l)) {
            return ResponseGenerator.returnOK("appDescriptions为空", List.of());
        }

        List<AppDescription> appDescriptions = l.stream().map((map) -> {
            AppDescription appDescription = new AppDescription();
            BeanUtil.fillBeanWithMap((Map<?, ?>) map, appDescription, null);
            return appDescription;
        }).collect(Collectors.toList());

        return ResponseGenerator.returnOK("返回appDescriptions成功", appDescriptions);
    }
}
