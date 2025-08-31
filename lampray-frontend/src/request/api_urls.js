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

function getServerConfig() {
    return (window.config || {}).server
}

const base = () => getServerConfig().host
const httpProtocol = () => getServerConfig().httpProtocol
const wsProtocol = () => getServerConfig().wsProtocol

const frontBase = () => 'localhost:5000';

export const frontBaseUrl = () => `${httpProtocol()}://${frontBase()}`;

export const baseUrl = () => `${httpProtocol()}://${base()}`;
export const wsBaseUrl = () => `${wsProtocol()}://${base()}`;

const prefix = () => `${baseUrl()}/api/v1`;
const adminPrefix = () => `${baseUrl()}/api/v1/admin`;
const wsPrefix =  () => `${wsBaseUrl()}/api/v1`;

export const passwordLogin = `${prefix()}/user/login/password`;
export const emailLogin = `${prefix()}/user/login/email`;
export const logout = `${prefix()}/user/logout`;
export const register = `${prefix()}/user/register`;
export const refreshToken = `${prefix()}/auth/token:refresh`;
export const registerActivate = (token) =>
    `${prefix()}/user/register/token/${token}`;

export const user = (admin = false) =>
    `${admin ? adminPrefix() : prefix()}/user`;
export const users = (admin = false, id = null) =>
    `${admin ? adminPrefix() : prefix()}/users${id ? `/${id}` : ''}`;
export const searchUsers = (admin = false) =>
    `${admin ? adminPrefix() : prefix()}/users/search`;
export const currentUser = `${prefix()}/user`;


export const userChat = wsPrefix + '/message/talk';
export const userMessages = (userId, admin = false) =>
    `${admin ? adminPrefix() : prefix()}/users/${userId}/messages`;
export const messages = (admin = false) =>
    `${admin ? adminPrefix() : prefix()}/messages`;

export const staffs = `${adminPrefix()}/staffs`;
export const staffInfo = (staffId, admin = false) =>
    `${prefix()}/staffs/${staffId}`;

export const staffInfoByUser = (userId, admin = false) =>
    `${prefix()}/users/${userId}/staff`;

export const allReviews = `${adminPrefix()}/reviews`;
export const currentReviews = `${prefix()}/reviews`;

export const statusReviews = (userId, admin = false) =>
    `${admin ? adminPrefix() : prefix()}/${userId}/reviews`;
export const reviewContent = (jobId, admin = false) =>
    `${admin ? adminPrefix() : prefix()}/reviews/${jobId}/content`;
export const reviewResource = (jobId, admin = false) =>
    `${admin ? adminPrefix() : prefix()}/reviews/${jobId}`;


export const tokenRefresh = `${prefix()}/auth/token/r`;

export const articleDetails = (userId, articleId, admin) => {
    return `${admin ? adminPrefix() : prefix()}/users/${userId}/articles/${articleId}`;
}
export const userArticles = (userId, admin = false) => {
    return `${admin ? adminPrefix() : prefix()}/users/${userId}/articles`;
}
export const articles = (admin) => `${admin ? adminPrefix() : prefix()}/articles`;
export const comments = (admin) => `${admin ? adminPrefix() : prefix()}/comments`;
export const contentComments = (contentType, contentId, admin) =>
    `${admin ? adminPrefix() : prefix()}/${contentType}/${contentId}/comments`;

export const userList = `${adminPrefix()}/users`;
export const systemErrorLog = `${adminPrefix()}/system/errors`;
export const systemSettings = `${adminPrefix()}/system/settings`;
export const systemMessageResources = `${adminPrefix()}/system/resources/messages`;
