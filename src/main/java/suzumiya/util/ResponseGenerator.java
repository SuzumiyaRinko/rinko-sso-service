package suzumiya.util;

import cn.hutool.http.HttpStatus;
import suzumiya.model.vo.BaseResponse;

public class ResponseGenerator {

    public static <T> BaseResponse<T> returnOK(String message, T data) {
        return new BaseResponse<>(HttpStatus.HTTP_OK, message, data);
    }

    public static <T> BaseResponse<T> returnError(Integer code, String message) {
        return new BaseResponse<>(code, message, null);
    }
}
