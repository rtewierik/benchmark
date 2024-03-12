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
package io.openmessaging.benchmark.worker.commands;

import java.util.ArrayList;
import java.util.List;

public class ProducerAssignment {
    public List<String> topics;
    public boolean isTpcH = false;

    public ProducerAssignment() {
        this.topics = new ArrayList<>();
    }

    public ProducerAssignment(List<String> topics) {
        this.topics = topics;
    }
}
