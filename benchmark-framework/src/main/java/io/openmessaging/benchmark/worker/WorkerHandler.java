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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.io.Files;
import io.javalin.Context;
import io.javalin.Javalin;
import io.openmessaging.benchmark.common.EnvironmentConfiguration;
import io.openmessaging.benchmark.common.ObjectMappers;
import io.openmessaging.benchmark.common.monitoring.CountersStats;
import io.openmessaging.benchmark.common.monitoring.CumulativeLatencies;
import io.openmessaging.benchmark.common.monitoring.PeriodStats;
import io.openmessaging.benchmark.worker.commands.ConsumerAssignment;
import io.openmessaging.benchmark.worker.commands.ProducerAssignment;
import io.openmessaging.benchmark.worker.commands.ProducerWorkAssignment;
import io.openmessaging.benchmark.worker.commands.TopicsInfo;
import java.io.File;
import java.util.List;
import org.apache.bookkeeper.stats.StatsLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class WorkerHandler {

    public static final String INITIALIZE_DRIVER = "/initialize-driver";
    public static final String CREATE_TOPICS = "/create-topics";
    public static final String CREATE_PRODUCERS = "/create-producers";
    public static final String PROBE_PRODUCERS = "/probe-producers";
    public static final String CREATE_CONSUMERS = "/create-consumers";
    public static final String PAUSE_CONSUMERS = "/pause-consumers";
    public static final String RESUME_CONSUMERS = "/resume-consumers";
    public static final String START_LOAD = "/start-load";
    public static final String ADJUST_PUBLISH_RATE = "/adjust-publish-rate";
    public static final String STOP_ALL = "/stop-all";
    public static final String PERIOD_STATS = "/period-stats";
    public static final String CUMULATIVE_LATENCIES = "/cumulative-latencies";
    public static final String COUNTERS_STATS = "/counters-stats";
    public static final String RESET_STATS = "/reset-stats";
    private final Worker localWorker;

    public WorkerHandler(Javalin app, StatsLogger statsLogger) {
        this.localWorker = new LocalWorker(statsLogger, "worker-handler", 0);

        app.post(INITIALIZE_DRIVER, this::handleInitializeDriver);
        app.post(CREATE_TOPICS, this::handleCreateTopics);
        app.post(CREATE_PRODUCERS, this::handleCreateProducers);
        app.post(PROBE_PRODUCERS, this::handleProbeProducers);
        app.post(CREATE_CONSUMERS, this::handleCreateConsumers);
        app.post(PAUSE_CONSUMERS, this::handlePauseConsumers);
        app.post(RESUME_CONSUMERS, this::handleResumeConsumers);
        app.post(START_LOAD, this::handleStartLoad);
        app.post(ADJUST_PUBLISH_RATE, this::handleAdjustPublishRate);
        app.post(STOP_ALL, this::handleStopAll);
        app.get(PERIOD_STATS, this::handlePeriodStats);
        app.get(CUMULATIVE_LATENCIES, this::handleCumulativeLatencies);
        app.get(COUNTERS_STATS, this::handleCountersStats);
        app.post(RESET_STATS, this::handleResetStats);
    }

    private void handleInitializeDriver(Context ctx) throws Exception {
        // Save config to temp file
        File tempFile = File.createTempFile("driver-configuration", "conf");
        Files.write(ctx.bodyAsBytes(), tempFile);

        localWorker.initializeDriver(tempFile);
        tempFile.delete();
    }

    private void handleCreateTopics(Context ctx) throws Exception {
        TopicsInfo topicsInfo = mapper.readValue(ctx.body(), TopicsInfo.class);
        log.info("Received create topics request for topics: {}", ctx.body());
        List<String> topics = localWorker.createTopics(topicsInfo);
        ctx.result(writer.writeValueAsString(topics));
    }

    private void handleCreateProducers(Context ctx) throws Exception {
        String body = ctx.body();
        ProducerAssignment topics = mapper.readValue(body, ProducerAssignment.class);
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Received create producers request for topics: {}", body);
        }
        localWorker.createProducers(topics);
    }

    private void handleProbeProducers(Context ctx) throws Exception {
        localWorker.probeProducers();
    }

    private void handleCreateConsumers(Context ctx) throws Exception {
        ConsumerAssignment consumerAssignment = mapper.readValue(ctx.body(), ConsumerAssignment.class);

        if (EnvironmentConfiguration.isDebug()) {
            log.info("ConsumerAssignment payload: {}", ctx.body());
            log.info(
                    "Received create consumers request for topics: {}",
                    writer.writeValueAsString(consumerAssignment.topicsSubscriptions));
        }
        localWorker.createConsumers(consumerAssignment);
    }

    private void handlePauseConsumers(Context ctx) throws Exception {
        localWorker.pauseConsumers();
    }

    private void handleResumeConsumers(Context ctx) throws Exception {
        localWorker.resumeConsumers();
    }

    private void handleStartLoad(Context ctx) throws Exception {
        ProducerWorkAssignment producerWorkAssignment =
                mapper.readValue(ctx.body(), ProducerWorkAssignment.class);

        int length =
                producerWorkAssignment.payloadData.isEmpty()
                        ? 0
                        : producerWorkAssignment.payloadData.get(0).length;
        log.info(
                "Start load publish-rate: {} msg/s -- payload-size: {} -- producer index: {}",
                producerWorkAssignment.publishRate,
                length,
                producerWorkAssignment.producerIndex);

        localWorker.startLoad(producerWorkAssignment);
    }

    private void handleAdjustPublishRate(Context ctx) throws Exception {
        Double publishRate = mapper.readValue(ctx.body(), Double.class);
        log.info("Adjust publish-rate: {} msg/s", publishRate);
        localWorker.adjustPublishRate(publishRate);
    }

    private void handleStopAll(Context ctx) throws Exception {
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Stop all command received.");
        }
        localWorker.stopAll();
    }

    private void handlePeriodStats(Context ctx) throws Exception {
        PeriodStats stats = localWorker.getPeriodStats();
        String serializedStats = writer.writeValueAsString(stats);
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Sending period stats: {}", serializedStats);
        }
        ctx.result(serializedStats);
    }

    private void handleCumulativeLatencies(Context ctx) throws Exception {
        CumulativeLatencies stats = localWorker.getCumulativeLatencies();
        String serializedStats = writer.writeValueAsString(stats);
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Sending cumulative latencies: {}", serializedStats);
        }
        ctx.result(serializedStats);
    }

    private void handleCountersStats(Context ctx) throws Exception {
        CountersStats stats = localWorker.getCountersStats();
        String serializedStats = writer.writeValueAsString(stats);
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Sending counters stats: {}", serializedStats);
        }
        ctx.result(serializedStats);
    }

    private void handleResetStats(Context ctx) throws Exception {
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Reset stats command received.");
        }
        localWorker.resetStats();
    }

    private static final Logger log = LoggerFactory.getLogger(WorkerHandler.class);

    private static final ObjectMapper mapper = ObjectMappers.mapper;
    private static final ObjectWriter writer = ObjectMappers.writer;
}
