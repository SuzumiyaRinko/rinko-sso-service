package suzumiya.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import suzumiya.constant.CommonConst;
import suzumiya.constant.MQConstant;
import suzumiya.constant.RedisConst;
import suzumiya.mapper.UserMapper;
import suzumiya.model.pojo.User;
import suzumiya.util.MailUtils;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class MQConsumer {

    @Resource
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /* 监听用户注册接口 */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstant.USER_REGISTER_QUEUE),
            exchange = @Exchange(name = MQConstant.SERVICE_DIRECT, type = ExchangeTypes.DIRECT, delayed = "true"),
            key = {MQConstant.USER_REGISTER_KEY}
    ))
    public void listenUserRegisterQueue(User newUser) throws MessagingException {
        /* 发送邮件到用户邮箱 */
        String toMail = newUser.getUsername();
        String activationURL = CommonConst.PREFIX_ACTIVATION_URL + newUser.getActivationUUID();
        String text = CommonConst.HTML_ACTIVATION.replaceAll("<xxxxx>", toMail).replaceAll("<yyyyy>", activationURL);

        MailUtils.sendMail(CommonConst.MAIL_FROM, List.of(toMail), "Rinko-Community | 账号激活", null, text, null);

        /* 30mins激活时间 */
        redisTemplate.opsForValue().set(RedisConst.ACTIVATION_USER_KEY + newUser.getActivationUUID(), newUser.getId(), 30L, TimeUnit.MINUTES); // 30mins

        log.debug("正在注册 username={} ", newUser.getUsername());
    }
}
