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

package tech.lamprism.lampray.shell.command;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import space.lingu.NonNull;

/**
 * @author RollW
 */
@Component
public class CommandAwarePostProcessor implements BeanPostProcessor {
    private final HelpCommandProvider helpCommandProvider;

    @Lazy
    public CommandAwarePostProcessor(HelpCommandProvider helpCommandProvider) {
        this.helpCommandProvider = helpCommandProvider;
    }

    @Override
    public Object postProcessAfterInitialization(
            @NonNull Object bean,
            @NonNull String beanName) throws BeansException {
        if (bean instanceof HelpCommandProviderAware aware) {
            aware.setHelpCommandProvider(helpCommandProvider);
        }
        return bean;
    }
}
