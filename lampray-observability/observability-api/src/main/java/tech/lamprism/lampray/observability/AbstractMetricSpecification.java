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

import io.micrometer.core.instrument.Meter;

import java.util.List;

/**
 * @author RollW
 */
public abstract class AbstractMetricSpecification<T extends Meter> extends AbstractSignalSpecification
        implements MetricSpecification<T> {
    private final String description;
    private final String baseUnit;
    private final Class<T> meterType;

    protected AbstractMetricSpecification(String name,
                                          List<TagSpecification> tagSpecifications,
                                          String description,
                                          String baseUnit,
                                          Class<T> meterType) {
        super(name, tagSpecifications);
        this.description = SpecificationSupport.normalizeDescription(description);
        this.baseUnit = SpecificationSupport.normalizeOptionalText(baseUnit, "baseUnit");
        this.meterType = meterType;
    }

    @Override
    public final String getDescription() {
        return description;
    }

    @Override
    public final String getBaseUnit() {
        return baseUnit;
    }

    @Override
    public final Class<T> getMeterType() {
        return meterType;
    }
}
