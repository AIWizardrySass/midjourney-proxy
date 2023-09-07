package com.github.novicezk.midjourney.config;

import com.github.novicezk.midjourney.result.ResponseObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author yubo.ge
 * @date 2023/9/5 22:33
 **/
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Throwable.class)
    public @ResponseBody
    ResponseObject<Void> handleThrowable(Throwable th) {
        log.error("occur unCatchException:", th);
        return ResponseObject.innerError();
    }
}
