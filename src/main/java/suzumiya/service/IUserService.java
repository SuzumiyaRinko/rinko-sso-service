package suzumiya.service;

import com.baomidou.mybatisplus.extension.service.IService;
import suzumiya.model.dto.UserLoginDTO;
import suzumiya.model.dto.UserRegisterDTO;
import suzumiya.model.pojo.User;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface IUserService extends IService<User> {

    /* 用户登录 */
    String login(UserLoginDTO userLoginDTO);

    /* 用户匿名登录 */
    String loginAnonymously();

    /* 用户注册 */
    void register(UserRegisterDTO userRegisterDTO);

    /* 用户激活 */
    void activate(String uuid, HttpServletResponse response) throws IOException;
}
