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
package io.openmessaging.benchmark.worker;


import java.io.File;
import java.io.IOException;

public class BenchmarkWorkers {

    public final Worker worker;
    public final LocalWorker localWorker;

    public BenchmarkWorkers(Worker worker, LocalWorker localWorker) {
        this.worker = worker;
        this.localWorker = localWorker;
    }

    public void stopAll() {
        worker.stopAll();
        if (worker != localWorker) {
            localWorker.stopAll();
        }
    }

    public void close() throws Exception {
        worker.close();
        if (worker != localWorker) {
            localWorker.close();
        }
    }

    public void initializeDriver(String driverConfig) throws IOException {
        File driverConfigFile = new File(driverConfig);
        worker.initializeDriver(driverConfigFile);
        if (worker != localWorker) {
            localWorker.initializeDriver(driverConfigFile);
        }
    }
}
