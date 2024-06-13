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
import io.netty.util.concurrent.DefaultThreadFactory;
import io.openmessaging.benchmark.common.EnvironmentConfiguration;
import io.openmessaging.benchmark.worker.BenchmarkWorkers;
import io.openmessaging.benchmark.worker.DistributedWorkersEnsemble;
import io.openmessaging.benchmark.worker.HttpWorkerClient;
import io.openmessaging.benchmark.worker.LocalWorker;
import io.openmessaging.benchmark.worker.Worker;
import io.openmessaging.tpch.client.S3Client;
import io.openmessaging.tpch.model.TpcHArguments;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.openmessaging.tpch.model.TpcHConsumerAssignment;
import io.openmessaging.tpch.model.TpcHIntermediateResult;
import io.openmessaging.tpch.model.TpcHQuery;
import io.openmessaging.tpch.processing.TpcHMessageProcessor;
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
                names = {"-tpch", "--tpc-h-files"},
                description = "Paths to a YAML file containing the TPC H command")
        public List<String> tpcHFiles;

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
//        benchmark(args);
        log.info("Starting at {}", System.currentTimeMillis());
        ExecutorService executorService = new ThreadPoolExecutor(
                8,
                8,
                50L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(5000),
                new DefaultThreadFactory("tpc-h-test"),
                new ThreadPoolExecutor.AbortPolicy());
        TpcHMessageProcessor processor = new TpcHMessageProcessor(
                () -> "tpc-h-test-id",
                new ArrayList<>(),
                null,
                () -> {},
                LoggerFactory.getLogger(Benchmark.class)
        );
        S3Client client = new S3Client(executorService, processor);
        ObjectWriter writer = new ObjectMapper().writer();
        Future<TpcHIntermediateResult>[] f = new Future[100];
        CompletableFuture<TpcHIntermediateResult>[] futures = new CompletableFuture[f.length];
        for (int i = 0; i < futures.length; i++) {
            int i2 = i + 1;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    return client.fetchAndProcessCsvInChunks(createTpcHConsumerAssignment(i2), 5242848).get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executorService);
        }
        log.info("Awaiting all futures...");
        CompletableFuture.allOf(futures).join();
        log.info("All futures completed.");
        TpcHIntermediateResult result = futures[0].get();
        for (int i = 1; i < 100; i++) {
            result.aggregateReducedResult(futures[i].get());
        }
        log.info("TPC-H query result: {}", writer.writeValueAsString(result));
    }

    private static TpcHConsumerAssignment createTpcHConsumerAssignment(int index) {
        return new TpcHConsumerAssignment(
                TpcHQuery.ForecastingRevenueChange,
                "test-query-id",
                "test-query-batch-id",
                0,
                0,
                10,
                100,
                "s3://tpc-h-chunks/chunks-by-file-size/70mb/chunk_" + index + ".csv"
        );
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
                log.info("Using default worker file workers.yaml!");
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
        List<TpcHArguments> tpcHArgumentsList = getTpcHArguments(arguments);

        workloads.forEach(
                (workloadName, workload) ->
                        arguments.drivers.forEach(
                                driverConfiguration ->
                                        tpcHArgumentsList.forEach(
                                                tpcHArguments ->
                                                        executeBenchmark(
                                                                arguments,
                                                                workloadName,
                                                                workload,
                                                                driverConfiguration,
                                                                tpcHArguments))));

        if (EnvironmentConfiguration.isDebug()) {
            log.info("Doing final clean-up...");
        }
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Final clean-up finished.");
        }
        System.exit(0);
    }

    private static void executeBenchmark(
            Arguments arguments,
            String workloadName,
            Workload workload,
            String driverConfig,
            TpcHArguments tpcHArguments) {
        BenchmarkWorkers workers = getWorkers(arguments);
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
            workers.stopAll();

            workers.initializeDriver(driverConfig);

            String driverName = driverConfiguration.name;
            WorkloadGenerator generator =
                    new WorkloadGenerator(driverName, workload, tpcHArguments, workers);

            TestResult result = generator.run();

            if (EnvironmentConfiguration.isDebug()) {
                log.info("Preparing to write test results...");
            }

            boolean useOutput = (arguments.output != null) && (arguments.output.length() > 0);

            String fileName =
                    useOutput
                            ? arguments.output
                            : String.format(
                                    "%s-%s-%s.json",
                                    workloadName, driverConfiguration.name, dateFormat.format(new Date()));

            if (EnvironmentConfiguration.isDebug()) {
                log.info("Writing test result into {}", fileName);
            }
            writer.writeValue(new File(fileName), result);

            generator.close();

            if (EnvironmentConfiguration.isDebug()) {
                log.info("Finished test and closed generator.");
            }
        } catch (Exception e) {
            log.error("Failed to run the workload '{}' for driver '{}'", workload.name, driverConfig, e);
        } finally {
            workers.stopAll();
        }
    }

    private static List<TpcHArguments> getTpcHArguments(Arguments arguments) throws IOException {
        List<TpcHArguments> tpcHArgumentsList = new ArrayList<>();
        if (arguments.tpcHFiles != null) {
            arguments.tpcHFiles.forEach(
                    file -> {
                        File tpcHArgumentsFile = new File(file);
                        try {
                            TpcHArguments tpcHArguments =
                                    mapper.readValue(tpcHArgumentsFile, TpcHArguments.class);
                            tpcHArgumentsList.add(tpcHArguments);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } else {
            tpcHArgumentsList.add(null);
        }
        log.info("TPC-H arguments: {}", writer.writeValueAsString(tpcHArgumentsList));
        return tpcHArgumentsList;
    }

    private static BenchmarkWorkers getWorkers(Arguments arguments) {
        Worker worker;
        LocalWorker localWorker = new LocalWorker();
        if (arguments.workers != null && !arguments.workers.isEmpty()) {
            List<Worker> workers =
                    arguments.workers.stream().map(HttpWorkerClient::new).collect(toList());
            boolean isTpcH = arguments.tpcHFiles != null && arguments.tpcHFiles.size() > 0;
            worker = new DistributedWorkersEnsemble(workers, arguments.extraConsumers, isTpcH);
        } else {
            // Use local worker implementation
            worker = localWorker;
        }
        return new BenchmarkWorkers(worker, localWorker);
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
