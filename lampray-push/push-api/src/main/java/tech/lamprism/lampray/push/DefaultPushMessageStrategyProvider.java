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

package tech.lamprism.lampray.push;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author RollW
 */
@Service
public class DefaultPushMessageStrategyProvider implements PushMessageStrategyProvider {
    private final List<PushMessageStrategy> pushMessageStrategies;

    public DefaultPushMessageStrategyProvider(
            List<PushMessageStrategy> pushMessageStrategies) {
        this.pushMessageStrategies = pushMessageStrategies;
    }

    @Override
    public PushMessageStrategy getPushMessageStrategy(PushType pushType) {
        return pushMessageStrategies.stream()
                .filter(pushMessageStrategy -> pushMessageStrategy.supports(pushType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such push type: " + pushType));
    }
}
