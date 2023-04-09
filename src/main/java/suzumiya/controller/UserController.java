package suzumiya.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import suzumiya.model.dto.UserLoginDTO;
import suzumiya.model.dto.UserRegisterDTO;
import suzumiya.model.vo.BaseResponse;
import suzumiya.service.IUserService;
import suzumiya.util.ResponseGenerator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private IUserService userService;

    @PostMapping("/login")
    public BaseResponse<String> login(@RequestBody UserLoginDTO userLoginDTO, HttpServletRequest request) {
        /* 用户登录 */
        String token = userService.login(userLoginDTO);
        return ResponseGenerator.returnOK("用户登录成功", token);
    }

    @PostMapping("/loginAnonymously")
    public BaseResponse<String> loginAnonymously() {
        /* 用户匿名登录 */
        String token = userService.loginAnonymously();
        return ResponseGenerator.returnOK("用户匿名登录成功", token);
    }

    @PostMapping("/logout")
    public BaseResponse<String> logout(HttpServletRequest request) {
        /* 用户登出 */
        userService.logout(request.getHeader("Authorization"));
        return ResponseGenerator.returnOK("用户退出登录成功", null);
    }

    @PostMapping("/register")
    public BaseResponse<String> register(@RequestBody UserRegisterDTO userRegisterDTO) {
        /* 用户注册 */
        userService.register(userRegisterDTO);
        return ResponseGenerator.returnOK("用户注册成功", null);
    }

    @PostMapping("/activation/{uuid}")
    public void activate(HttpServletResponse response, @PathVariable("uuid") String uuid) throws IOException {
        /* 激活用户（激活页面在Service层返回） */
        userService.activate(uuid, response);
    }
}
