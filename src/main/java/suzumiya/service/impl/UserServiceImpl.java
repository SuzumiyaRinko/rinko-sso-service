package suzumiya.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWTUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import suzumiya.constant.CommonConst;
import suzumiya.constant.MQConstant;
import suzumiya.constant.RedisConst;
import suzumiya.mapper.UserMapper;
import suzumiya.model.dto.UserLoginDTO;
import suzumiya.model.dto.UserRegisterDTO;
import suzumiya.model.pojo.User;
import suzumiya.service.IUserService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService, UserDetailsService {

    private static final String TOKEN_KEY = "114514"; // Token密钥
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    @Lazy
    private AuthenticationManager authenticationManager;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        /* 查询用户信息 */
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public String login(UserLoginDTO userLoginDTO) {
        /* 判断注册的账号密码是否符合要求 */
        String username = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();
        if (username != null && !(username.matches(CommonConst.REGEX_EMAIL) && password.matches(CommonConst.REGEX_PASSWORD))) {
            throw new RuntimeException("账号或密码不符合要求");
        }

        /* 生成Authentication对象，让SS做校验 */
        // 获取当前用户的salt
        // username为null不会抛异常
        User existedUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, userLoginDTO.getUsername()));
        // 判断账号不存在
        if (existedUser == null) {
            throw new RuntimeException("该账号不存在");
        }
        // 判断账号是否已激活
        if (existedUser.getActivation() == 0) {
            throw new RuntimeException("该账号未激活");
        }
        userLoginDTO.setSalt(existedUser.getSalt());
        // 验证账号密码
        Authentication authenticationToken = new UsernamePasswordAuthenticationToken(userLoginDTO.getUsername(), userLoginDTO.getPassword() + userLoginDTO.getSalt());
        Authentication authentication;
        try {
            // SS返回UserDetail
            // 这里会调用UserDetail.getAuthorities()方法，但是登录时authoritiesStr为空，所以此时UserDetail.getAuthorities()返回null
            authentication = authenticationManager.authenticate(authenticationToken); // 会调用loadUserByUsername
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("账号或密码错误");
        }

        /* 查询用户权限 */
        User authenticatedUser = (User) authentication.getPrincipal();
        List<String> authoritiesStr = userMapper.getAuthoritiesStrByUserId(authenticatedUser.getId());
        authenticatedUser.setAuthoritiesStr(authoritiesStr);

        /* 查询roles */
        authenticatedUser.setRoles(userMapper.getRolesByUserId(authenticatedUser.getId()));

        /* 把用户信息存放到Redis中，TTL为30mins */
        String key = RedisConst.LOGIN_USER_KEY + authenticatedUser.getId();
        Map<String, Object> value = new HashMap<>();
        BeanUtil.beanToMap(authenticatedUser, value, true, null);
        redisTemplate.opsForHash().putAll(key, value);
        redisTemplate.expire(key, 30, TimeUnit.MINUTES);

        /* 生成并返回Jwt */
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("userId", authenticatedUser.getId());
//        return "Bearer " + JWTUtil.createToken(payload, TOKEN_KEY.getBytes(StandardCharsets.UTF_8));
        return JWTUtil.createToken(payload, TOKEN_KEY.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String loginAnonymously() {
        /* 新增用户 */
        String nickname = "user_" + UUID.randomUUID().toString().substring(0, 6); // [0, 6)

        User newUser = new User();
        newUser.setNickname(nickname);
        newUser.setActivation(2);
        newUser.setAvatar(CommonConst.DEFAULT_AVATAR);
        userMapper.insert(newUser);

        /* 设置角色 */
        userMapper.setRoles4UserId(newUser.getId(), List.of(5));

        /* 查询用户权限 */
        List<String> authoritiesStr = userMapper.getAuthoritiesStrByUserId(newUser.getId());
        newUser.setAuthoritiesStr(authoritiesStr);

        /* 把用户信息存放到Redis中，TTL为30mins */
        String key = RedisConst.LOGIN_USER_KEY + newUser.getId();
        Map<String, Object> value = new HashMap<>();
        BeanUtil.beanToMap(newUser, value, true, null);
        redisTemplate.opsForHash().putAll(key, value);
        redisTemplate.expire(key, 30, TimeUnit.MINUTES);

        /* 生成并返回Jwt */
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("userId", newUser.getId());
        return JWTUtil.createToken(payload, TOKEN_KEY.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void logout(String token) {
        /* 删除Redis中的用户信息 */
        Object userId = JWTUtil.parseToken(token).getPayload("userId");
        redisTemplate.delete(RedisConst.LOGIN_USER_KEY + userId);
    }

    @Override
    public void register(UserRegisterDTO userRegisterDTO) {
        /* 判断验证码是否正确 */
        String uuid = userRegisterDTO.getUuid();
        String correctCode;
        Object o = redisTemplate.opsForValue().get(RedisConst.USER_VERIFY_KEY + uuid);
        if (o != null) {
            correctCode = ((String) o).toLowerCase();
        } else {
            throw new RuntimeException("非法访问");
        }

        String code = userRegisterDTO.getCode().toLowerCase();
        if (!StrUtil.equals(correctCode, code)) {
            throw new RuntimeException("非法访问");
        }

        /* 判断注册的账号密码是否符合要求 */
        String username = userRegisterDTO.getUsername();
        String password = userRegisterDTO.getPassword();
        if (username != null && !(username.matches(CommonConst.REGEX_EMAIL) && password.matches(CommonConst.REGEX_PASSWORD))) {
            throw new RuntimeException("账号或密码不符合要求");
        }

        /* 判断password和confirmPassword是否一致 */
        String confirmPassword = userRegisterDTO.getConfirmPassword();
        if (!StrUtil.equals(password, confirmPassword)) {
            throw new RuntimeException("非法访问");
        }

        /* 判断当前用户名是否已经存在 */
        User existedUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, userRegisterDTO.getUsername()));
        if (existedUser != null) {
            throw new RuntimeException("当前用户名已存在");
        }

        /* 新增用户 */
        String salt = UUID.randomUUID().toString().substring(0, 8); // [0, 8)
        String encodePassword = passwordEncoder.encode(userRegisterDTO.getPassword() + salt);

        User newUser = new User();
        newUser.setUsername(userRegisterDTO.getUsername());
        newUser.setPassword(encodePassword);
        newUser.setSalt(salt);

        uuid = UUID.randomUUID().toString();
        newUser.setActivationUUID(uuid);
        newUser.setNickname("user_" + uuid.substring(0, 8));
        newUser.setAvatar(CommonConst.DEFAULT_AVATAR);
        userMapper.insert(newUser);

        /* 设置角色 */
        userMapper.setRoles4UserId(newUser.getId(), List.of(4));

        /* 30mins激活时间（异步） */
        /* 发送邮件到用户邮箱（异步） */
        rabbitTemplate.convertAndSend(MQConstant.SERVICE_DIRECT, MQConstant.USER_REGISTER_KEY, newUser);
    }

    @Override
    public void activate(String uuid, HttpServletResponse response) throws IOException {
        /* 返回激活结果反馈页面 */
        response.setStatus(200);
        response.setContentType("text/html;charset=utf-8");
        response.setCharacterEncoding("utf-8");

        /* 判断该账号是否存在 */
        User existedUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getActivationUUID, uuid));
        if (existedUser == null) {
            throw new RuntimeException("该账号不存在");
        }
        /* 判断该账号是否已激活 */
        if (existedUser.getActivation() == 1) {
            response.getWriter().print(CommonConst.HTML_ACTIVATION_SUCCESS
                    .replaceAll("<xxxxx>", existedUser.getUsername())
                    .replaceAll("<yyyyy>", CommonConst.USER_LOGIN_URL));
            return; // 账号已激活
        }
        /* 判断该账号是否激活超时 */
        Object t = redisTemplate.opsForValue().get(RedisConst.ACTIVATION_USER_KEY + uuid);
        if (t == null) {
            userMapper.deleteById(existedUser.getId());
            response.getWriter().print(CommonConst.HTML_ACTIVATION_EXPIRED
                    .replaceAll("<yyyyy>", CommonConst.USER_REGISTER_URL));
            return; // 账号激活超时
        }

        Integer userId = (Integer) t; // Redis上没有"Long"这个数据类型

        /* 激活账号 */
        User tt = new User();
        tt.setId((long) userId);
        tt.setActivation(1);
        int result = userMapper.updateById(tt);
        if (result == 1) {
            redisTemplate.delete(RedisConst.ACTIVATION_USER_KEY + uuid);
            response.getWriter().print(CommonConst.HTML_ACTIVATION_SUCCESS
                    .replaceAll("<xxxxx>", existedUser.getUsername())
                    .replaceAll("<yyyyy>", CommonConst.USER_LOGIN_URL));
        }
    }
}
