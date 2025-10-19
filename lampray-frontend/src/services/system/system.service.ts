import type {AxiosInstance, RawAxiosRequestConfig} from "axios";
import type {HttpResponseBody, PageableRequest} from "@/services/common.type.ts";
import type {SettingVo} from "@/services/system/system.type.ts";

export const systemSettingService = (axios: AxiosInstance) => {
    return {
        async listSettings(pageableRequest: PageableRequest, options: RawAxiosRequestConfig = {}): Promise<HttpResponseBody<SettingVo[]>> {
            const mergedOptions = {...options};
            if (!mergedOptions.params) {
                mergedOptions.params = {};
            }
            mergedOptions.params.page = pageableRequest.page;
            mergedOptions.params.size = pageableRequest.size;
            const response = await axios.get<HttpResponseBody<SettingVo[]>>(
                '/api/v1/admin/system/settings', mergedOptions
            );
            return response.data;
        },
        async setSetting(key: string, value: string | null, options: RawAxiosRequestConfig = {}): Promise<void> {
            const mergedOptions = {...options};
            const path = `/api/v1/system/settings/{key}`
                .replace(`{key}`, encodeURIComponent(String(key)));
            await axios.put(
                path,
                {
                    value: value
                },
                mergedOptions
            );
        },
        async getSetting(key: string, options: RawAxiosRequestConfig = {}): Promise<HttpResponseBody<SettingVo>> {
            const mergedOptions = {...options};
            const path = `/api/v1/system/settings/{key}`
                .replace(`{key}`, encodeURIComponent(String(key)));
            const response = await axios.get<HttpResponseBody<SettingVo>>(
                path, mergedOptions
            );
            return response.data;
        }
    }
}
