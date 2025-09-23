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

package tech.lamprism.lampray.web.controller.system;

import com.google.common.base.Strings;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import tech.lamprism.lampray.setting.AttributedSettingSpecification;
import tech.lamprism.lampray.setting.ConfigProvider;
import tech.lamprism.lampray.setting.ConfigValue;
import tech.lamprism.lampray.setting.ConfigValueInfo;
import tech.lamprism.lampray.setting.SecretLevel;
import tech.lamprism.lampray.setting.SettingDescriptionProvider;
import tech.lamprism.lampray.setting.SettingKey;
import tech.lamprism.lampray.setting.SettingSpecificationHelper;
import tech.lamprism.lampray.setting.SettingSpecificationProvider;
import tech.lamprism.lampray.setting.SystemSetting;
import tech.lamprism.lampray.web.controller.AdminApi;
import tech.lamprism.lampray.web.controller.system.model.SettingVo;
import tech.rollw.common.web.HttpResponseEntity;

import java.util.List;

/**
 * @author RollW
 */
@AdminApi
public class SystemSettingController {
    private final ConfigProvider configProvider;
    private final SettingSpecificationProvider settingSpecificationProvider;
    private final SettingDescriptionProvider settingDescriptionProvider;

    public SystemSettingController(
            ConfigProvider configProvider,
            SettingSpecificationProvider settingSpecificationProvider,
            SettingDescriptionProvider settingDescriptionProvider) {
        this.configProvider = configProvider;
        this.settingSpecificationProvider = settingSpecificationProvider;
        this.settingDescriptionProvider = settingDescriptionProvider;
    }

    private String maskSecret(String value,
                              SecretLevel secretLevel) {
        if (Strings.isNullOrEmpty(value) || secretLevel == SecretLevel.NONE) {
            return value;
        }
        return secretLevel.maskValue(value);
    }

    @GetMapping("/system/settings")
    public HttpResponseEntity<List<SettingVo>> getSettings() {
        List<AttributedSettingSpecification<?, ?>> settingSpecifications = settingSpecificationProvider.getSettingSpecifications();
        List<SettingVo> res = configProvider.list(settingSpecifications)
                .stream()
                .map(value -> {
                    AttributedSettingSpecification<?, ?> specification =
                            (AttributedSettingSpecification<?, ?>) value.getSpecification();
                    boolean secret = specification.getSecret();
                    SecretLevel secretLevel = secret ? SecretLevel.MEDIUM : SecretLevel.NONE;
                    return toSettingVo(value, secretLevel, specification);
                }).toList();
        return HttpResponseEntity.success(res);
    }

    private SettingVo toSettingVo(ConfigValue<?, ?> value, SecretLevel secretLevel,
                                  AttributedSettingSpecification<?, ?> specification) {
        SettingKey<?, ?> key = value.getSpecification().getKey();
        String masked = maskSecret(String.valueOf(value.getValue()), secretLevel);
        String description = settingDescriptionProvider.getSettingDescription(specification.getDescription());
        if (value instanceof ConfigValueInfo<?, ?> info) {
            return new SettingVo(
                    key.getName(),
                    masked,
                    description,
                    key.getType().toString(),
                    value.getSource(),
                    info.getLastModified()
            );
        }
        return new SettingVo(
                key.getName(),
                masked,
                description,
                key.getType().toString(),
                value.getSource(),
                null
        );
    }

    @PutMapping("/system/settings/{key}")
    public HttpResponseEntity<Void> setSetting(@PathVariable String key,
                                               @RequestBody Value value) {
        // TODO: check setting key is valid and value is valid
        @SuppressWarnings("unchecked")
        AttributedSettingSpecification<Object, Object> specification = (AttributedSettingSpecification<Object, Object>)
                settingSpecificationProvider.getSettingSpecification(key);
        configProvider.set(specification, SettingSpecificationHelper.INSTANCE.deserialize(value.value(), specification));
        return HttpResponseEntity.success();
    }

    @DeleteMapping("/system/settings/{key}")
    public HttpResponseEntity<Void> deleteSetting(@PathVariable String key) {
        // TODO: may need a delete method in ConfigProvider
        AttributedSettingSpecification<?, ?> specification = settingSpecificationProvider.getSettingSpecification(key);
        configProvider.set(specification, null);
        return HttpResponseEntity.success();
    }

    @GetMapping("/system/settings/{key}")
    public HttpResponseEntity<SystemSetting> getSetting(@PathVariable String key) {
        return HttpResponseEntity.success();
    }

    public record Value(String value) {
    }
}
