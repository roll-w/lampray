/*
 * Copyright (C) 2023-2025 RollW
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

package tech.lamprism.lampray.content.common;

import space.lingu.NonNull;
import tech.rollw.common.web.ErrorCode;
import tech.rollw.common.web.ErrorCodeFinder;
import tech.rollw.common.web.ErrorCodeMessageProvider;

import java.util.List;

/**
 * @author RollW
 */
public enum ContentErrorCode implements ErrorCode, ErrorCodeFinder, ErrorCodeMessageProvider {
    ERROR_CONTENT("A2000", 400),
    ERROR_CONTENT_NOT_FOUND("A2001", 404),
    ERROR_CONTENT_FORBIDDEN("A2002", 401),
    ERROR_CONTENT_HIDE("A2003", 401),
    ERROR_CONTENT_DELETED("A2004", 404),
    ERROR_CONTENT_REVIEWING("A2005", 401),
    ERROR_PASSWORD_REQUIRED("A2006", 401),
    ERROR_CONTENT_EXISTED("A2007", 400),
    ERROR_CONTENT_NOT_DELETED("A2008", 400),
    ERROR_CONTENT_CANNOT_RESTORE("A2009", 400),
    ERROR_CONTENT_STRUCTURE_INVALID("A2010", 400);


    private final String value;
    private final int status;

    ContentErrorCode(String value, int status) {
        this.value = value;
        this.status = status;
    }

    ContentErrorCode(String value) {
        this.value = value;
        this.status = 403;
    }

    @Override
    public String toString() {
        return "ContentError: %s, code: %s".formatted(name(), getCode());
    }

    @Override
    @NonNull
    public String getCode() {
        return value;
    }

    @NonNull
    @Override
    public String getName() {
        return name();
    }

    @Override
    public boolean success() {
        return false;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public ErrorCode fromThrowable(Throwable e, ErrorCode defaultErrorCode) {
        return null;
    }

    @Override
    public ErrorCode findErrorCode(String codeValue) {
        return ErrorCodeFinder.from(values(), codeValue);
    }

    private static final List<ErrorCode> CODES = List.of(values());

    @Override
    public List<ErrorCode> listErrorCodes() {
        return CODES;
    }

    public static ErrorCodeFinder getFinderInstance() {
        return ERROR_CONTENT;
    }
}
