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

package tech.lamprism.lampray.web.controller.system;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.lamprism.lampray.security.firewall.FirewallRegistry;
import tech.lamprism.lampray.security.firewall.IdentifierType;
import tech.lamprism.lampray.security.firewall.RequestIdentifier;
import tech.lamprism.lampray.security.firewall.filtertable.FilterEntry;
import tech.lamprism.lampray.security.firewall.filtertable.FilterTable;
import tech.lamprism.lampray.web.controller.AdminApi;
import tech.lamprism.lampray.web.controller.system.model.AddFilterEntryRequest;
import tech.lamprism.lampray.web.controller.system.model.FilterEntryVo;
import tech.lamprism.lampray.web.controller.system.model.FirewallInfo;
import tech.rollw.common.web.HttpResponseEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author RollW
 */
@AdminApi
@RestController
public class FirewallController {

    private final FirewallRegistry firewallRegistry;
    private final FilterTable filterTable;

    public FirewallController(FirewallRegistry firewallRegistry,
                              FilterTable filterTable) {
        this.firewallRegistry = firewallRegistry;
        this.filterTable = filterTable;
    }


    /**
     * List all firewalls.
     *
     * @return a list of firewall information
     */
    @GetMapping("/firewalls")
    public HttpResponseEntity<List<FirewallInfo>> list() {
        return HttpResponseEntity.success();
    }

    /**
     * Get the filter table entries.
     *
     * @return a list of filter table entries.
     */
    @GetMapping("/firewalls/filter-table")
    public HttpResponseEntity<List<FilterEntryVo>> getFilterTable() {
        List<FilterEntryVo> entries = new ArrayList<>();
        // TODO: page support
        for (FilterEntry entry : filterTable) {
            entries.add(new FilterEntryVo(
                    entry.getIdentifier(),
                    entry.getType(),
                    entry.getMode(),
                    entry.getExpiration(),
                    entry.getReason()
            ));
        }
        return HttpResponseEntity.success(entries);
    }

    @PatchMapping("/firewalls/filter-table")
    public HttpResponseEntity<Void> addFilterEntry(@RequestBody AddFilterEntryRequest request) {
        OffsetDateTime expiration = calculateExpiration(request.getExpirationSeconds());

        FilterEntry entry = new FilterEntry(
                request.getIdentifier(),
                request.getType(),
                request.getMode(),
                expiration,
                request.getReason()
        );
        filterTable.plusAssign(entry);
        return HttpResponseEntity.success();
    }

    @PutMapping("/firewalls/filter-table")
    public HttpResponseEntity<Void> updateFilterEntry(
            @RequestBody AddFilterEntryRequest request) {
        OffsetDateTime expiration = calculateExpiration(request.getExpirationSeconds());
        FilterEntry newEntry = new FilterEntry(
                request.getIdentifier(),
                request.getType(),
                request.getMode(),
                expiration,
                request.getReason()
        );
        filterTable.plusAssign(newEntry);
        return HttpResponseEntity.success();
    }

    @DeleteMapping("/firewalls/filter-table")
    public HttpResponseEntity<Void> removeFilterEntry(@RequestParam String identifier, @RequestParam String type) {
        IdentifierType identifierType = IdentifierType.fromString(type);
        RequestIdentifier requestIdentifier = new RequestIdentifier(
                identifier, identifierType);

        FilterEntry entry = filterTable.get(requestIdentifier);
        if (entry != null) {
            filterTable.minusAssign(entry);
        }
        return HttpResponseEntity.success();
    }

    @PostMapping("/firewalls/filter-table/clear")
    public HttpResponseEntity<Void> clearFilterTable() {
        filterTable.clear();
        return HttpResponseEntity.success();
    }

    /**
     * Calculate expiration time from seconds.
     *
     * @param seconds Duration in seconds. Use -1 or 0 for permanent.
     * @return Expiration time
     */
    private OffsetDateTime calculateExpiration(Long seconds) {
        if (seconds == null || seconds <= 0) {
            return FilterEntry.INF;
        }
        return OffsetDateTime.now().plusSeconds(seconds);
    }
}
