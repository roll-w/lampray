/*
 * Copyright (C) 2023 RollW
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.lamprism.lampray.web.controller;

import com.google.common.base.Strings;
import com.google.common.base.VerifyException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import tech.lamprism.lampray.authentication.login.LoginTokenException;
import tech.lamprism.lampray.security.authentication.adapter.TokenAuthenticationException;
import tech.lamprism.lampray.web.common.ApiContext;
import tech.lamprism.lampray.web.common.ParameterMissingException;
import tech.lamprism.lampray.web.system.ErrorRecord;
import tech.lamprism.lampray.web.system.ErrorRecordVo;
import tech.rollw.common.web.AuthErrorCode;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.CommonRuntimeException;
import tech.rollw.common.web.ErrorCode;
import tech.rollw.common.web.ErrorCodeFinder;
import tech.rollw.common.web.HttpResponseEntity;
import tech.rollw.common.web.IoErrorCode;
import tech.rollw.common.web.UserErrorCode;
import tech.rollw.common.web.WebCommonErrorCode;
import tech.rollw.common.web.system.ContextThread;
import tech.rollw.common.web.system.ContextThreadAware;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Handle {@link Exception}s
 *
 * @author RollW
 */
@ControllerAdvice
@RestController
public class LampSystemExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(LampSystemExceptionHandler.class);
    private final ErrorCodeFinder errorCodeFinder;
    private final MessageSource messageSource;
    private final ContextThreadAware<ApiContext> apiContextThreadAware;

    public LampSystemExceptionHandler(ErrorCodeFinder errorCodeFinder,
                                      MessageSource messageSource,
                                      ContextThreadAware<ApiContext> apiContextThreadAware) {
        this.errorCodeFinder = errorCodeFinder;
        this.messageSource = messageSource;
        this.apiContextThreadAware = apiContextThreadAware;
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public HttpResponseEntity<Void> handleNoHandlerFoundException(NoHandlerFoundException e) {
        HttpMethod method = HttpMethod.valueOf(e.getHttpMethod());
        if (method == HttpMethod.OPTIONS) {
            return new HttpResponseEntity<>(HttpStatus.OK);
        }
        return HttpResponseEntity.of(
                CommonErrorCode.ERROR_NOT_FOUND,
                e.getMessage()
        );
    }

    @ExceptionHandler(ParameterMissingException.class)
    public HttpResponseEntity<Void> handle(ParameterMissingException e) {
        return HttpResponseEntity.of(
                WebCommonErrorCode.ERROR_PARAM_MISSING,
                e.getMessage()
        );
    }

    @ExceptionHandler({
            BindException.class,
            ConstraintViolationException.class,
            MethodArgumentNotValidException.class,
    })
    public HttpResponseEntity<Void> handleParamException(Exception e) {
        return HttpResponseEntity.of(
                WebCommonErrorCode.ERROR_PARAM_FAILED,
                e.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public HttpResponseEntity<Void> handleMethodTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        return HttpResponseEntity.of(
                WebCommonErrorCode.ERROR_PARAM_FAILED,
                e.getMessage()
        );
    }

    @ExceptionHandler(LoginTokenException.class)
    public HttpResponseEntity<Void> handle(LoginTokenException e) {
        return HttpResponseEntity.of(
                errorCodeFinder.fromThrowable(e)
        );
    }

    @ExceptionHandler(CommonRuntimeException.class)
    public HttpResponseEntity<Void> handle(CommonRuntimeException e) {
        logger.trace("Common runtime exception: {}", e.getMessage(), e);
        return HttpResponseEntity.of(
                errorCodeFinder.fromThrowable(e),
                tryGetMessage(e)
        );
    }

    private String tryGetMessage(CommonRuntimeException e) {
        if (Strings.isNullOrEmpty(e.getMessage())) {
            return null;
        }
        ContextThread<ApiContext> contextThread =
                apiContextThreadAware.getContextThread();
        Locale locale = contextThread.hasContext() ?
                contextThread.getContext().getLocale() :
                Locale.getDefault();
        try {
            return messageSource.getMessage(
                    e.getMessage(),
                    e.getArgs(),
                    locale
            );
        } catch (Exception ex) {
            return tryFormatMessage(e.getMessage(), e.getArgs());
        }
    }

    private String tryFormatMessage(String message, Object[] args) {
        if (args == null || args.length == 0) {
            return message;
        }
        try {
            return MessageFormat.format(message, args);
        } catch (Exception e) {
            return message;
        }
    }

    @ExceptionHandler(NullPointerException.class)
    public HttpResponseEntity<Void> handle(NullPointerException e) {
        logger.error("Null exception : %s".formatted(e.toString()), e);
        recordErrorLog(CommonErrorCode.ERROR_NULL, e);
        return HttpResponseEntity.of(
                CommonErrorCode.ERROR_NULL,
                e.getMessage()
        );
    }

    // treat VerifyException as parameter failed
    @ExceptionHandler(VerifyException.class)
    public HttpResponseEntity<Void> handle(VerifyException e) {
        return HttpResponseEntity.of(
                WebCommonErrorCode.ERROR_PARAM_FAILED,
                e.getMessage()
        );
    }

    @ExceptionHandler(FileNotFoundException.class)
    public HttpResponseEntity<Void> handle(FileNotFoundException e) {
        recordErrorLog(IoErrorCode.ERROR_FILE_NOT_FOUND, e);
        return HttpResponseEntity.of(
                IoErrorCode.ERROR_FILE_NOT_FOUND,
                e.getMessage()
        );
    }

    @ExceptionHandler(IOException.class)
    public HttpResponseEntity<Void> handle(IOException e) {
        logger.error("IO Error: %s".formatted(e.toString()), e);
        ErrorRecord errorRecord = recordErrorLog(e);
        return HttpResponseEntity.of(
                errorRecord.errorCode(),
                e.getMessage()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public HttpResponseEntity<Void> handleAuthException(AccessDeniedException e) {
        return HttpResponseEntity.of(AuthErrorCode.ERROR_UNAUTHORIZED_USE);
    }

    @ExceptionHandler(DisabledException.class)
    public HttpResponseEntity<Void> handleAuthException(DisabledException e) {
        return HttpResponseEntity.of(UserErrorCode.ERROR_USER_DISABLED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public HttpResponseEntity<Void> handleAuthException(AuthenticationException e) {
        if (e instanceof InsufficientAuthenticationException) {
            return HttpResponseEntity.of(AuthErrorCode.ERROR_NOT_HAS_ROLE);
        }
        if (e instanceof TokenAuthenticationException tokenException) {
            return HttpResponseEntity.of(tokenException.getErrorCode());
        }
        logger.warn("Auth Error: {}, type: {}", e.getMessage(), e.getClass().getCanonicalName());
        return HttpResponseEntity.of(AuthErrorCode.ERROR_NOT_HAS_ROLE);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public HttpResponseEntity<Void> handleHttpMessageNotReadableException() {
        return HttpResponseEntity.of(WebCommonErrorCode.ERROR_BODY_MISSING);
    }

    @ExceptionHandler(Exception.class)
    public HttpResponseEntity<Void> handle(Exception e) {
        logger.error("Error: %s".formatted(e.toString()), e);
        ErrorRecord errorRecord = recordErrorLog(e);
        return HttpResponseEntity.of(
                errorRecord.errorCode(),
                e.getMessage()
        );
    }

    private final Deque<ErrorRecord> errorRecords = new LinkedBlockingDeque<>();

    private ErrorRecord recordErrorLog(Throwable throwable) {
        ErrorCode errorCode = errorCodeFinder.fromThrowable(throwable);
        return recordErrorLog(errorCode, throwable);
    }

    private ErrorRecord recordErrorLog(ErrorCode errorCode, Throwable throwable) {
        long time = System.currentTimeMillis();
        ErrorRecord errorRecord = new ErrorRecord(errorCode, throwable, time);
        putErrorRecord(errorRecord);
        return errorRecord;
    }

    @Async
    void putErrorRecord(ErrorRecord errorRecord) {
        errorRecords.addLast(errorRecord);
        if (errorRecords.size() > 100) {
            errorRecords.removeFirst();
        }
    }

    @GetMapping("/api/v1/admin/system/errors")
    public HttpResponseEntity<List<ErrorRecordVo>> getErrorRecords() {
        return HttpResponseEntity.success(
                errorRecords.stream()
                        .map(ErrorRecordVo::from)
                        .toList()
        );
    }

    @GetMapping("/api/v1/common/error/occur")
    public void makeException() {
        throw new RuntimeException("Test exception");
    }
}
