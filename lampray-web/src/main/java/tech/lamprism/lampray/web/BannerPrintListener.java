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

package tech.lamprism.lampray.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import space.lingu.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Print banner at startup.
 *
 * @author RollW
 */
public class BannerPrintListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    @Override
    public void onApplicationEvent(@NonNull ApplicationEnvironmentPreparedEvent event) {
        Logger logger = LoggerFactory.getLogger("Lampray");
        LamprayBanner banner = new LamprayBanner();
        ByteArrayOutputStream out = new ByteArrayOutputStream(120);
        banner.printBanner(
                event.getEnvironment(),
                event.getSpringApplication().getMainApplicationClass(),
                new PrintStream(out)
        );
        String bannerText = out.toString();
        logger.info(bannerText);
    }
}
