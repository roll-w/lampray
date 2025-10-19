
export interface SettingVo {
    key: string;
    value?: any;
    description: string;
    type: string;
    source: SettingSource;
    updateTime?: string;
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
