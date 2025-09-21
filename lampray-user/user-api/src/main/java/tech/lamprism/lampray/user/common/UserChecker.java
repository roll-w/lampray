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

package tech.lamprism.lampray.user.common;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author RollW
 */
public class UserChecker {
    public static final String USERNAME_REGEX = "^[a-zA-Z_\\-][\\w.\\-]{3,20}$";
    public static final String PASSWORD_REGEX = "^[A-Za-z\\d._\\-~!@#$^&*+=<>%;'\"\\\\/|()\\[\\]{}]{4,20}$";
    public static final String EMAIL_REGEX = "^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$";

    private static final Pattern sUsernamePattern = Pattern.compile(USERNAME_REGEX);
    private static final Pattern sPasswordPattern = Pattern.compile(PASSWORD_REGEX);
    private static final Pattern sEmailPattern = Pattern.compile(EMAIL_REGEX);

    /**
     * Allow only letters, numbers, underscores, dash and dot,
     * length between 3 and 20,
     * no special characters except underscore, dash and dot.
     * Must start with a letter or underscore.
     */
    public static boolean checkUsername(String username) {
        if (StringUtils.isEmpty(username)) {
            return false;
        }
        if (username.length() > 20 || username.length() < 3) {
            return false;
        }
        Matcher matcher = sUsernamePattern.matcher(username);
        return matcher.matches();
    }

    /**
     * Allow alphanumeric and some special characters,
     * minimum 4 characters, maximum 20 characters.
     */
    public static boolean checkPassword(String password) {
        if (StringUtils.isEmpty(password)) {
            return false;
        }
        if (password.length() > 18 || password.length() < 3) {
            return false;
        }
        Matcher matcher = sPasswordPattern.matcher(password);
        return matcher.matches();
    }

    public static boolean checkEmail(String email) {
        if (StringUtils.isEmpty(email)) {
            return false;
        }
        Matcher matcher = sEmailPattern.matcher(email);
        return matcher.matches();
    }
}
