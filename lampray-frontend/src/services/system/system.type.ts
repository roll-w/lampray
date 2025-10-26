
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

export interface SettingVo {
    key: string;
    value?: any;
    description: string;
    type: string;
    source: SettingSource;
    updateTime?: string;
    supportedSources: SettingSource[];
}

export interface SettingDetailsVo {
    key: string;
    value?: any;
    description: string;
    type: string;
    source: SettingSource;
    updateTime?: string;
    supportedSources: SettingSource[];
    defaults: number[];
    valueEntries: any[];
    layers: SettingDetailsValueLayer[];
}

export interface SettingDetailsValueLayer {
    source: SettingSource;
    value?: any;
}

export const SettingSource = {
    NONE: "NONE",
    LOCAL: "LOCAL",
    MEMORY: "MEMORY",
    PROPERTIES: "PROPERTIES",
    ENVIRONMENT: "ENVIRONMENT",
    DATABASE: "DATABASE"
} as const;

export type SettingSource = typeof SettingSource[keyof typeof SettingSource];
