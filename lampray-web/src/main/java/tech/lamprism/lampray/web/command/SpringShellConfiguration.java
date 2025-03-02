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

import org.jline.terminal.Terminal;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.shell.ResultHandler;
import org.springframework.shell.ResultHandlerService;
import org.springframework.shell.boot.SpringShellAutoConfiguration;
import org.springframework.shell.command.CommandCatalog;
import org.springframework.shell.config.ShellConversionServiceSupplier;
import org.springframework.shell.context.ShellContext;
import org.springframework.shell.exit.ExitCodeMappings;
import org.springframework.shell.result.GenericResultHandlerService;
import org.springframework.shell.result.ResultHandlerConfig;
import tech.lamprism.lampray.shell.OperatorSupportedShell;

import java.util.Set;

/**
 * Creates supporting beans for running the Shell
 */
@Configuration
@EnableAutoConfiguration(exclude = {SpringShellAutoConfiguration.class})
@Import(ResultHandlerConfig.class)
public class SpringShellConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ShellConversionServiceSupplier shellConversionServiceSupplier(
            ApplicationContext applicationContext) {
        ApplicationConversionService service = new ApplicationConversionService();
        DefaultConversionService.addDefaultConverters(service);
        DefaultConversionService.addCollectionConverters(service);
        ApplicationConversionService.addBeans(service, applicationContext);
        return () -> service;
    }

    @Bean
    public ResultHandlerService resultHandlerService(Set<ResultHandler<?>> resultHandlers) {
        GenericResultHandlerService service = new GenericResultHandlerService();
        for (ResultHandler<?> resultHandler : resultHandlers) {
            service.addResultHandler(resultHandler);
        }
        return service;
    }

    @Bean
    public OperatorSupportedShell shell(
            ResultHandlerService resultHandlerService,
            CommandCatalog commandRegistry,
            Terminal terminal,
            ShellConversionServiceSupplier shellConversionServiceSupplier,
            ShellContext shellContext,
            ExitCodeMappings exitCodeMappings) {
        OperatorSupportedShell shell = new OperatorSupportedShell(resultHandlerService, commandRegistry, terminal, shellContext, exitCodeMappings);
        shell.setConversionService(shellConversionServiceSupplier.get());
        return shell;
    }
}
