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

package tech.lamprism.lampray.web.command;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

/**
 * @author RollW
 */
@Component
public class CommandPromptProvider implements PromptProvider {
    @Override
    public AttributedString getPrompt() {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() != null) {
            return new AttributedString(
                    context.getAuthentication().getName() + "@lampray> ",
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)
            );
        }

        return new AttributedString(
                "lampray> ",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)
        );
    }
}
