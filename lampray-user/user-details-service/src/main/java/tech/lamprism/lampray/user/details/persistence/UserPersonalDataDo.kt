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

package tech.lamprism.lampray.user.details.persistence

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.lamprism.lampray.DataEntity
import tech.lamprism.lampray.TimeAttributed
import tech.lamprism.lampray.user.UserResourceKind
import tech.lamprism.lampray.user.details.Birthday
import tech.lamprism.lampray.user.details.Gender
import tech.lamprism.lampray.user.details.UserPersonalData
import tech.rollw.common.web.system.SystemResourceKind
import java.time.OffsetDateTime

/**
 * @author RollW
 */
@Entity
@Table(name = "user_personal_data")
class UserPersonalDataDo(
    @Id
    @Column(name = "id", nullable = false)
    private var id: Long = 0,

    @Column(name = "nickname", nullable = false, length = 120)
    var nickname: String = "",

    @Column(name = "avatar", nullable = false, length = 255)
    var avatar: String = "",

    @Column(name = "cover", nullable = false, length = 255)
    var cover: String = "",

    @Column(name = "birthday", nullable = false, length = 12)
    @Convert(converter = BirthdayConverter::class)
    var birthday: Birthday = Birthday.UNKNOWN,

    @Column(name = "introduction", nullable = true, length = 255)
    var introduction: String? = null,

    @Column(name = "gender", nullable = false, length = 24)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var gender: Gender = Gender.PRIVATE,

    @Column(name = "location", nullable = true, length = 120)
    var location: String? = null,

    @Column(name = "website", nullable = true, length = 255)
    var website: String? = null,

    @Column(name = "update_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private var updateTime: OffsetDateTime = OffsetDateTime.now()
) : DataEntity<Long> {
    override fun getEntityId(): Long = id

    override fun getSystemResourceKind(): SystemResourceKind =
        UserResourceKind

    override fun getCreateTime(): OffsetDateTime = TimeAttributed.NONE_TIME

    override fun getUpdateTime(): OffsetDateTime = updateTime

    class BirthdayConverter : AttributeConverter<Birthday, String> {
        override fun convertToDatabaseColumn(attribute: Birthday?): String? {
            return attribute?.birthday
        }

        override fun convertToEntityAttribute(dbData: String?): Birthday? {
            return Birthday.fromString(dbData)
        }
    }

    fun lock() = UserPersonalData(
        id, nickname, avatar, cover, birthday, introduction,
        gender, location, website, updateTime
    )

    fun toBuilder() = Builder(this)

    class Builder {
        private var id: Long = 0
        private var nickname: String? = null
        private var avatar: String? = null
        private var cover: String? = null
        private var birthday: Birthday? = null
        private var introduction: String? = null
        private var gender: Gender? = null
        private var location: String? = null
        private var website: String? = null
        private var updateTime: OffsetDateTime? = null

        constructor()

        constructor(other: UserPersonalDataDo) {
            this.id = other.id
            this.nickname = other.nickname
            this.avatar = other.avatar
            this.cover = other.cover
            this.birthday = other.birthday
            this.introduction = other.introduction
            this.gender = other.gender
            this.location = other.location
            this.website = other.website
            this.updateTime = other.updateTime
        }

        fun setId(id: Long) = apply {
            this.id = id
        }

        fun setNickname(nickname: String) = apply {
            this.nickname = nickname
        }

        fun setAvatar(avatar: String) = apply {
            this.avatar = avatar
        }

        fun setCover(cover: String) = apply {
            this.cover = cover
        }

        fun setBirthday(birthday: Birthday) = apply {
            this.birthday = birthday
        }

        fun setIntroduction(introduction: String?) = apply {
            this.introduction = introduction
        }

        fun setGender(gender: Gender) = apply {
            this.gender = gender
        }

        fun setLocation(location: String?) = apply {
            this.location = location
        }

        fun setWebsite(website: String?) = apply {
            this.website = website
        }

        fun setUpdateTime(updateTime: OffsetDateTime) = apply {
            this.updateTime = updateTime
        }

        fun build(): UserPersonalDataDo {
            return UserPersonalDataDo(
                id,
                nickname!!,
                avatar!!,
                cover!!,
                birthday!!,
                introduction,
                gender!!,
                location,
                website,
                updateTime!!
            )
        }
    }

    companion object {
        @JvmStatic
        fun UserPersonalData.toDo() = UserPersonalDataDo(
            entityId, nickname, avatar, cover, birthday, introduction,
            gender, location, website, updateTime
        )

        @JvmStatic
        fun builder() = Builder()
    }
}