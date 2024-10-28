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

package space.lingu.lamp.content.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import space.lingu.lamp.content.permit.ContentPermitCheckProvider;
import space.lingu.lamp.content.permit.ContentPermitChecker;
import space.lingu.lamp.content.permit.ContentPermitCheckerChain;

import java.util.List;

/**
 * @author RollW
 */
@Configuration
public class ContentConfiguration {

    @Bean
    public ContentPermitChecker contentPermitFilterChain(
            List<ContentPermitCheckProvider> contentPermitCheckProviders
    ) {
        return ContentPermitCheckerChain.of(
                contentPermitCheckProviders
        );
    }
}