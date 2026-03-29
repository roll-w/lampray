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

package tech.lamprism.lampray.storage.monitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author RollW
 */
@Component
public class StorageTrafficPublisher {
    private final List<StorageMonitoringListener> listeners;

    public StorageTrafficPublisher() {
        this(List.of());
    }

    @Autowired
    public StorageTrafficPublisher(List<StorageMonitoringListener> listeners) {
        this.listeners = List.copyOf(listeners);
    }

    public void publishProxyUpload(String groupName,
                                   long bytes) {
        publish(StorageTrafficEvent.uploadRequest(StorageTrafficScope.group(groupName)));
        publish(StorageTrafficEvent.uploadBytes(StorageTrafficScope.group(groupName), bytes));
    }

    public void publishProxyDownload(String groupName,
                                     long bytes) {
        publish(StorageTrafficEvent.downloadBytes(StorageTrafficScope.group(groupName), bytes));
    }

    public void publishProxyDownloadRequest(String groupName) {
        publish(StorageTrafficEvent.downloadRequest(StorageTrafficScope.group(groupName)));
    }

    public void publishDirectUploadRequest(String groupName,
                                           String backendName,
                                           long declaredBytes) {
        publish(StorageTrafficEvent.directUploadRequest(StorageTrafficScope.OVERALL, declaredBytes));
        publish(StorageTrafficEvent.directUploadRequest(StorageTrafficScope.group(groupName), declaredBytes));
        publish(StorageTrafficEvent.directUploadRequest(StorageTrafficScope.backend(backendName), declaredBytes));
    }

    public void publishDirectDownloadRequest(String groupName,
                                             String backendName) {
        publish(StorageTrafficEvent.directDownloadRequest(StorageTrafficScope.OVERALL));
        publish(StorageTrafficEvent.directDownloadRequest(StorageTrafficScope.group(groupName)));
        publish(StorageTrafficEvent.directDownloadRequest(StorageTrafficScope.backend(backendName)));
    }

    public void publishBackendUpload(String backendName,
                                     long bytes) {
        publish(StorageTrafficEvent.uploadBytes(StorageTrafficScope.OVERALL, bytes));
        publish(StorageTrafficEvent.uploadBytes(StorageTrafficScope.backend(backendName), bytes));
    }

    public void publishBackendUploadRequest(String backendName) {
        publish(StorageTrafficEvent.uploadRequest(StorageTrafficScope.OVERALL));
        publish(StorageTrafficEvent.uploadRequest(StorageTrafficScope.backend(backendName)));
    }

    public void publishBackendDownloadRequest(String backendName) {
        publish(StorageTrafficEvent.downloadRequest(StorageTrafficScope.OVERALL));
        publish(StorageTrafficEvent.downloadRequest(StorageTrafficScope.backend(backendName)));
    }

    public void publishBackendDownload(String backendName,
                                       long bytes) {
        publish(StorageTrafficEvent.downloadBytes(StorageTrafficScope.OVERALL, bytes));
        publish(StorageTrafficEvent.downloadBytes(StorageTrafficScope.backend(backendName), bytes));
    }

    private void publish(StorageTrafficEvent event) {
        if (event.getAmount() < 0) {
            return;
        }
        for (StorageMonitoringListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
