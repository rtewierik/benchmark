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

import io.openmessaging.benchmark.common.key.distribution.KeyDistributorType;
import io.openmessaging.tpch.model.TpcHArguments;

import java.util.List;

public class ProducerWorkAssignment {

    public List<byte[]> payloadData;
    public double publishRate;
    public KeyDistributorType keyDistributorType;
    public TpcHArguments tpcHArguments = null;
    public Integer producerIndex = null;

    public ProducerWorkAssignment withPublishRate(double publishRate) {
        ProducerWorkAssignment copy = new ProducerWorkAssignment();
        copy.keyDistributorType = this.keyDistributorType;
        copy.payloadData = this.payloadData;
        copy.publishRate = publishRate;
        copy.tpcHArguments = this.tpcHArguments;
        copy.producerIndex = this.producerIndex;
        return copy;
    }

    public ProducerWorkAssignment withProducerIndex(Integer producerIndex) {
        ProducerWorkAssignment copy = new ProducerWorkAssignment();
        copy.keyDistributorType = this.keyDistributorType;
        copy.payloadData = this.payloadData;
        copy.publishRate = this.publishRate;
        copy.tpcHArguments = this.tpcHArguments;
        copy.producerIndex = producerIndex;
        return copy;
    }
}
