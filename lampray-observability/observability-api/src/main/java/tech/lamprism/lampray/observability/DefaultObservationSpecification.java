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

package tech.lamprism.lampray.observability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
public final class DefaultObservationSpecification extends AbstractSignalSpecification implements ObservationSpecification {
    private final SignalDomain domain;

    private DefaultObservationSpecification(Builder builder) {
        super(builder.name, builder.tagSpecifications);
        this.domain = Objects.requireNonNull(builder.domain, "domain cannot be null");
    }

    public static Builder business(String name) {
        return new Builder(name, SignalDomain.BUSINESS);
    }

    public static Builder system(String name) {
        return new Builder(name, SignalDomain.SYSTEM);
    }

    @Override
    public SignalDomain getDomain() {
        return domain;
    }

    /**
     * @author RollW
     */
    public static final class Builder {
        private final String name;
        private final SignalDomain domain;
        private final List<TagSpecification> tagSpecifications = new ArrayList<>();

        private Builder(String name,
                        SignalDomain domain) {
            this.name = SpecificationSupport.requireName(name);
            this.domain = Objects.requireNonNull(domain, "domain cannot be null");
        }

        public Builder tag(TagSpecification tagSpecification) {
            this.tagSpecifications.add(tagSpecification);
            return this;
        }

        public Builder tags(TagSpecification... tagSpecifications) {
            this.tagSpecifications.addAll(Arrays.asList(tagSpecifications));
            return this;
        }

        public DefaultObservationSpecification build() {
            return new DefaultObservationSpecification(this);
        }
    }
}
