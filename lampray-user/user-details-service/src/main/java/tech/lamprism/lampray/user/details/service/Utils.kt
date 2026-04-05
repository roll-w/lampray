/*
 * Copyright (C) 2023-2026 RollW
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

package tech.lamprism.lampray.user.details.service

import tech.lamprism.lampray.user.details.Birthday
import tech.lamprism.lampray.user.details.Gender
import tech.lamprism.lampray.user.details.UserDataField
import tech.lamprism.lampray.user.details.UserDataFieldType
import tech.lamprism.lampray.user.details.persistence.UserPersonalDataEntity

/**
 * @author RollW
 */
internal object Utils {
    @JvmStatic
    fun UserPersonalDataEntity.Builder.setBuilderValue(field: UserDataField<*>) {
        when (field.type()) {
            UserDataFieldType.AVATAR -> setAvatar(field.value() as String)
            UserDataFieldType.INTRO -> setIntroduction(field.value() as String)
            UserDataFieldType.GENDER -> setGender(field.value() as Gender)
            UserDataFieldType.WEBSITE -> setWebsite(field.value() as String)
            UserDataFieldType.LOCATION -> setLocation(field.value() as String)
            UserDataFieldType.BIRTHDAY -> setBirthday(field.value() as Birthday)
            UserDataFieldType.NICKNAME -> setNickname(field.value() as String)
            else -> throw IllegalArgumentException("Unknown field type: ${field.type()}")
        }
    }
}