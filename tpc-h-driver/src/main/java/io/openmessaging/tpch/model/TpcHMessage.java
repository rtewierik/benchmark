/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openmessaging.tpch.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class TpcHMessage {
    public final String messageId;
    public final TpcHMessageType type;
    public final String message;

    public TpcHMessage(TpcHMessageType type, String message) {
        this.messageId = UUID.randomUUID().toString();
        this.type = type;
        this.message = message;
    }

    public TpcHMessage(
            @JsonProperty("messageId") String messageId,
            @JsonProperty("type") TpcHMessageType type,
            @JsonProperty("message") String message
    ) {
        this.messageId = messageId;
        this.type = type;
        this.message = message;
    }
}
