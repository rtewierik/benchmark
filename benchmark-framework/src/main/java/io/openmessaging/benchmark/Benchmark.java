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
package io.openmessaging.benchmark;

import static java.util.stream.Collectors.toList;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openmessaging.benchmark.driver.TpcHQuery;
import io.openmessaging.benchmark.tpch.*;
import io.openmessaging.benchmark.worker.DistributedWorkersEnsemble;
import io.openmessaging.benchmark.worker.HttpWorkerClient;
import io.openmessaging.benchmark.worker.LocalWorker;
import io.openmessaging.benchmark.worker.Worker;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Benchmark {

    static class Arguments {

        @Parameter(
                names = {"-c", "--csv"},
                description = "Print results from this directory to a csv file")
        String resultsDir;

        @Parameter(
                names = {"-h", "--help"},
                description = "Help message",
                help = true)
        boolean help;

        @Parameter(
                names = {"-d", "--drivers"},
                description =
                        "Drivers list. eg.: pulsar/pulsar.yaml,kafka/kafka.yaml") // , required = true)
        public List<String> drivers;

        @Parameter(
                names = {"-w", "--workers"},
                description = "List of worker nodes. eg: http://1.2.3.4:8080,http://4.5.6.7:8080")
        public List<String> workers;

        @Parameter(
                names = {"-wf", "--workers-file"},
                description = "Path to a YAML file containing the list of workers addresses")
        public File workersFile;

        @Parameter(
                names = {"-tpch", "--tpc-h-file"},
                description = "Path to a YAML file containing the TPC H command")
        public File tpcHFile;

        @Parameter(
                names = {"-x", "--extra"},
                description = "Allocate extra consumer workers when your backlog builds.")
        boolean extraConsumers;

        @Parameter(description = "Workloads") // , required = true)
        public List<String> workloads;

        @Parameter(
                names = {"-o", "--output"},
                description = "Output",
                required = false)
        public String output;
    }

    public static void main(String[] args) throws Exception {
        benchmark(new String[] { "--drivers", "workloads/redis-default.yaml", "--tpc-h-file", "workloads/tpc-h-default.yaml", "workloads/simple-workload.yaml" });
        // testTpcHAlgorithmLocally();
    }

    private static void testTpcHAlgorithmLocally() {
        TpcHQuery query = TpcHQuery.PricingSummaryReport;
        List<String> chunkFiles = Arrays.asList(
                 "../tpc-h-chunks/ref/chunk_1.csv",
                 "../tpc-h-chunks/ref/chunk_2.csv",
                 "../tpc-h-chunks/ref/chunk_3.csv",
                 "../tpc-h-chunks/ref/chunk_4.csv",
                 "../tpc-h-chunks/ref/chunk_5.csv",
                 "../tpc-h-chunks/ref/chunk_6.csv",
                 "../tpc-h-chunks/ref/chunk_7.csv",
                 "../tpc-h-chunks/ref/chunk_8.csv",
                 "../tpc-h-chunks/ref/chunk_9.csv",
                 "../tpc-h-chunks/ref/chunk_10.csv"
                // "../tpc-h-chunks/lineitem.tbl"
        );
        List<TpcHIntermediateResult> chunk = new ArrayList<>();
        for (String chunkFile : chunkFiles) {
            System.out.printf("[INFO] Applying map to chunk \"%s\"...%n", chunkFile);
            try (InputStream stream = Files.newInputStream(Paths.get(chunkFile))) {
                List<TpcHRow> chunkData = TpcHDataParser.readTpcHRowsFromStream(stream);
                TpcHIntermediateResult result = TpcHAlgorithm.applyQueryToChunk(chunkData, query, new TpcHConsumerAssignment());
                chunk.add(result);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        System.out.println("[INFO] Applying reducer to chunk...");
        TpcHIntermediateResult intermediateResult = TpcHQueryIntermediateResultsReducer.applyReduceToChunk(chunk, query);
        System.out.println("[INFO] Generating result from reduced intermediate result...");
        TpcHQueryResult result = TpcHQueryResultGenerator.generateResult(intermediateResult, query);
        System.out.println(result);
    }

    private static void benchmark(String[] args) throws Exception {
        final Arguments arguments = new Arguments();
        JCommander jc = new JCommander(arguments);
        jc.setProgramName("messaging-benchmark");

        try {
            jc.parse(args);
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            jc.usage();
            System.exit(-1);
        }

        if (arguments.help) {
            jc.usage();
            System.exit(-1);
        }

        if (arguments.resultsDir != null) {
            ResultsToCsv r = new ResultsToCsv();
            r.writeAllResultFiles(arguments.resultsDir);
            System.exit(0);
        }

        if (arguments.workers != null && arguments.workersFile != null) {
            System.err.println("Only one between --workers and --workers-file can be specified");
            System.exit(-1);
        }

        if (arguments.workers == null && arguments.workersFile == null) {
            File defaultFile = new File("workers.yaml");
            if (defaultFile.exists()) {
                log.info("Using default worker file workers.yaml");
                arguments.workersFile = defaultFile;
            }
        }

        if (arguments.workersFile != null) {
            log.info("Reading workers list from {}", arguments.workersFile);
            arguments.workers = mapper.readValue(arguments.workersFile, Workers.class).workers;
        }

        // Dump configuration variables
        log.info("Starting benchmark with config: {}", writer.writeValueAsString(arguments));

        Map<String, Workload> workloads = new TreeMap<>();
        for (String path : arguments.workloads) {
            File file = new File(path);
            String name = file.getName().substring(0, file.getName().lastIndexOf('.'));

            workloads.put(name, mapper.readValue(file, Workload.class));
        }

        log.info("Workloads: {}", writer.writeValueAsString(workloads));

        TpcHCommand tpcHCommand;
        if (arguments.tpcHFile != null) {
            tpcHCommand = mapper.readValue(arguments.tpcHFile, TpcHCommand.class);
        } else {
            tpcHCommand = null;
        }
        log.info("TPC-H command: {}", writer.writeValueAsString(tpcHCommand));

        Worker worker;
        LocalWorker localWorker = new LocalWorker();

        if (arguments.workers != null && !arguments.workers.isEmpty()) {
            List<Worker> workers =
                    arguments.workers.stream().map(HttpWorkerClient::new).collect(toList());
            worker = new DistributedWorkersEnsemble(workers, arguments.extraConsumers);
        } else {
            // Use local worker implementation
            worker = localWorker;
        }

        workloads.forEach(
                (workloadName, workload) -> {
                    arguments.drivers.forEach(
                            driverConfig -> {
                                try {
                                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                                    File driverConfigFile = new File(driverConfig);
                                    DriverConfiguration driverConfiguration =
                                            mapper.readValue(driverConfigFile, DriverConfiguration.class);
                                    log.info(
                                            "--------------- WORKLOAD : {} --- DRIVER : {}---------------",
                                            workload.name,
                                            driverConfiguration.name);

                                    // Stop any left over workload
                                    worker.stopAll();
                                    if (worker != localWorker) {
                                        localWorker.stopAll();
                                    }

                                    worker.initializeDriver(new File(driverConfig));
                                    if (worker != localWorker) {
                                        localWorker.initializeDriver(new File(driverConfig));
                                    }

                                    WorkloadGenerator generator =
                                            new WorkloadGenerator(driverConfiguration.name, workload, tpcHCommand, worker, localWorker);

                                    TestResult result = generator.run();

                                    log.info("Preparing to write test results...");

                                    boolean useOutput = (arguments.output != null) && (arguments.output.length() > 0);

                                    String fileName =
                                            useOutput
                                                    ? arguments.output
                                                    : String.format(
                                                    "%s-%s-%s.json",
                                                    workloadName,
                                                    driverConfiguration.name,
                                                    dateFormat.format(new Date()));

                                    log.info("Writing test result into {}", fileName);
                                    writer.writeValue(new File(fileName), result);

                                    generator.close();

                                    log.info("Finished test and closed generator.");
                                } catch (Exception e) {
                                    log.error(
                                            "Failed to run the workload '{}' for driver '{}'",
                                            workload.name,
                                            driverConfig,
                                            e);
                                } finally {
                                    worker.stopAll();
                                    if (worker != localWorker) {
                                        localWorker.stopAll();
                                    }
                                }
                            });
                });

        log.info("Doing final clean-up...");
        worker.close();
        if (worker != localWorker) {
            localWorker.close();
        }
        log.info("Final clean-up finished.");
        System.exit(0);
    }

    private static final ObjectMapper mapper =
            new ObjectMapper(new YAMLFactory())
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    static {
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    private static final ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();

    private static final Logger log = LoggerFactory.getLogger(Benchmark.class);
}
