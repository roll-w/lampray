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

package tech.lamprism.lampray.web.common;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import space.lingu.NonNull;
import tech.rollw.common.web.system.ContextThreadAware;

/**
 * @author RollW
 */
@Component
class ApiContextAwareInitializer implements BeanPostProcessor {
    private final ContextThreadAware<ApiContext> contextAware;

    @Lazy
    ApiContextAwareInitializer(ContextThreadAware<ApiContext> contextAware) {
        this.contextAware = contextAware;
    }

    @Override
    public Object postProcessAfterInitialization(
            @NonNull Object bean,
            @NonNull String beanName) throws BeansException {
        if (bean instanceof ApiContextAware aware) {
            aware.setApiContext(contextAware);
        }
        return bean;
    }
}
