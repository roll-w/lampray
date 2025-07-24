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

package tech.lamprism.lampray.security.firewall;

import space.lingu.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author RollW
 */
public class SimpleFirewallRegistry implements FirewallRegistry {
    private final List<Firewall> firewalls;

    public SimpleFirewallRegistry(List<Firewall> firewalls) {
        this.firewalls = new ArrayList<>(firewalls);
    }

    @NonNull
    @Override
    public List<Firewall> getFirewalls() {
        return firewalls.stream()
                .sorted(Comparator.comparingInt(Firewall::getPriority))
                .toList();
    }

    @Override
    public void addFirewall(@NonNull Firewall firewall) {
        firewalls.add(firewall);
        firewalls.sort(Comparator.comparingInt(Firewall::getPriority));
    }

    @Override
    public void removeFirewall(@NonNull Firewall firewall) {
        firewalls.remove(firewall);
    }
}
