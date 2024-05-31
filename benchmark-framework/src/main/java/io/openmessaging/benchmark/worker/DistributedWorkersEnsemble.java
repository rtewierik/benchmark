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

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;

import com.beust.jcommander.internal.Maps;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.openmessaging.benchmark.common.EnvironmentConfiguration;
import io.openmessaging.benchmark.common.monitoring.CountersStats;
import io.openmessaging.benchmark.common.monitoring.CumulativeLatencies;
import io.openmessaging.benchmark.common.monitoring.PeriodStats;
import io.openmessaging.benchmark.common.utils.RandomGenerator;
import io.openmessaging.benchmark.utils.ListPartition;
import io.openmessaging.benchmark.worker.commands.ConsumerAssignment;
import io.openmessaging.benchmark.worker.commands.ProducerAssignment;
import io.openmessaging.benchmark.worker.commands.ProducerWorkAssignment;
import io.openmessaging.benchmark.worker.commands.TopicSubscription;
import io.openmessaging.benchmark.worker.commands.TopicsInfo;
import io.openmessaging.tpch.TpcHConstants;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistributedWorkersEnsemble implements Worker {
    private static final int LEADER_WORKER_INDEX = 0;
    private final Thread shutdownHook = new Thread(this::stopAll);
    private final List<Worker> workers;
    private final List<Worker> producerWorkers;
    private final List<Worker> consumerWorkers;
    private final Worker leader;

    private int numberOfUsedProducerWorkers;

    public DistributedWorkersEnsemble(
            List<Worker> workers, boolean extraConsumerWorkers, boolean isTpcH) {
        Preconditions.checkArgument(workers.size() > 1);
        this.workers = unmodifiableList(workers);
        leader = workers.get(LEADER_WORKER_INDEX);
        int numberOfProducerWorkers = getNumberOfProducerWorkers(workers, extraConsumerWorkers);
        if (numberOfProducerWorkers == workers.size() || isTpcH) {
            this.producerWorkers = new ArrayList<>(this.workers);
            this.consumerWorkers = new ArrayList<>(this.workers);
        } else {
            List<List<Worker>> partitions =
                    Lists.partition(Lists.reverse(workers), workers.size() - numberOfProducerWorkers);
            this.producerWorkers = partitions.get(1);
            this.consumerWorkers = partitions.get(0);
        }

        log.info(
                "Workers list - producers: [{}]",
                producerWorkers.stream().map(Worker::id).collect(joining(",")));
        log.info(
                "Workers list - consumers: {}",
                consumerWorkers.stream().map(Worker::id).collect(joining(",")));

        if (!isTpcH) {
            Thread shutdownHook = new Thread(this::stopAll);
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
    }

    /*
     * For driver-jms extra consumers are required. If there is an odd number of workers then allocate the extra
     * to consumption.
     */
    @VisibleForTesting
    static int getNumberOfProducerWorkers(List<Worker> workers, boolean extraConsumerWorkers) {
        if (EnvironmentConfiguration.isProduceWithAllWorkers()) {
            return workers.size();
        }
        return extraConsumerWorkers ? (workers.size() + 2) / 3 : workers.size() / 2;
    }

    @Override
    public void initializeDriver(File configurationFile) throws IOException {
        workers.parallelStream()
                .forEach(
                        w -> {
                            try {
                                w.initializeDriver(configurationFile);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> createTopics(TopicsInfo topicsInfo) throws IOException {
        return leader.createTopics(topicsInfo);
    }

    @Override
    public void createProducers(ProducerAssignment producerAssignment) {
        if (producerAssignment.isTpcH) {
            createTpcHProducers(producerAssignment);
        } else {
            createThroughputProducers(producerAssignment);
        }
    }

    @Override
    public void startLoad(ProducerWorkAssignment producerWorkAssignment) throws IOException {
        double newRate = producerWorkAssignment.publishRate / numberOfUsedProducerWorkers;
        log.debug("Setting worker assigned publish rate to {} msgs/sec", newRate);
        int producersNeeded = this.producerWorkers.size();
        List<AbstractMap.SimpleEntry<Worker, Integer>> workersToStartWithIndices =
                IntStream.range(0, producersNeeded)
                        .mapToObj(i -> new AbstractMap.SimpleEntry<>(this.producerWorkers.get(i), i))
                        .collect(Collectors.toList());
        workersToStartWithIndices.parallelStream()
                .forEach(
                        w -> {
                            try {
                                w.getKey()
                                        .startLoad(
                                                producerWorkAssignment
                                                        .withPublishRate(newRate)
                                                        .withProducerIndex(w.getValue()));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
    }

    @Override
    public void probeProducers() throws IOException {
        producerWorkers.parallelStream()
                .forEach(
                        w -> {
                            try {
                                w.probeProducers();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
    }

    @Override
    public void adjustPublishRate(double publishRate) throws IOException {
        double newRate = publishRate / numberOfUsedProducerWorkers;
        log.debug("Adjusting producer publish rate to {} msgs/sec", newRate);
        producerWorkers.parallelStream()
                .forEach(
                        w -> {
                            try {
                                w.adjustPublishRate(newRate);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
    }

    @Override
    public void stopAll() {
        workers.parallelStream().forEach(Worker::stopAll);
    }

    @Override
    public String id() {
        return "Ensemble[" + workers.stream().map(Worker::id).collect(joining(",")) + "]";
    }

    @Override
    public void pauseConsumers() throws IOException {
        consumerWorkers.parallelStream()
                .forEach(
                        w -> {
                            try {
                                w.pauseConsumers();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
    }

    @Override
    public void resumeConsumers() throws IOException {
        consumerWorkers.parallelStream()
                .forEach(
                        w -> {
                            try {
                                w.resumeConsumers();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
    }

    @Override
    public void createConsumers(ConsumerAssignment assignment) throws IOException {
        if (EnvironmentConfiguration.isDebug()) {
            for (TopicSubscription ts : assignment.topicsSubscriptions) {
                log.info("Topic subscription detected: {}", ts.toString());
            }
        }
        if (assignment.isTpcH) {
            createTpcHConsumers(assignment);
        } else {
            createThroughputConsumers(assignment);
        }
    }

    private void createTpcHConsumers(ConsumerAssignment assignment) throws IOException {
        List<TopicSubscription> subscriptions = assignment.topicsSubscriptions;
        TopicSubscription resultSubscription = subscriptions.get(TpcHConstants.REDUCE_DST_INDEX);
        Map<Integer, ConsumerAssignment> topicsPerConsumerMap = Maps.newHashMap();
        for (int i = 0; i < 3; i++) {
            ConsumerAssignment individualAssignment = new ConsumerAssignment(assignment);
            TopicSubscription mapSubscription = subscriptions.get(TpcHConstants.MAP_CMD_START_INDEX + i);
            TopicSubscription reduceSubscription = subscriptions.get(TpcHConstants.REDUCE_SRC_START_INDEX + i);
            individualAssignment.topicsSubscriptions.add(resultSubscription);
            individualAssignment.topicsSubscriptions.add(mapSubscription);
            individualAssignment.topicsSubscriptions.add(reduceSubscription);
            topicsPerConsumerMap.put(i, individualAssignment);
        }
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Topics per consumer map: {}", writer.writeValueAsString(topicsPerConsumerMap));
        }
        topicsPerConsumerMap.entrySet().parallelStream()
                .forEach(
                        e -> {
                            try {
                                Integer consumerIndex = e.getKey();
                                ConsumerAssignment consumerAssignment =
                                        e.getValue().withConsumerIndex(consumerIndex);
                                workers.get(consumerIndex).createConsumers(consumerAssignment);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        });
    }

    private void createThroughputConsumers(ConsumerAssignment overallConsumerAssignment) {
        List<List<TopicSubscription>> subscriptionsPerConsumer =
                ListPartition.partitionList(
                        overallConsumerAssignment.topicsSubscriptions, consumerWorkers.size());
        Map<Worker, ConsumerAssignment> topicsPerWorkerMap = Maps.newHashMap();
        int i = 0;
        for (List<TopicSubscription> tsl : subscriptionsPerConsumer) {
            ConsumerAssignment individualAssignment = new ConsumerAssignment(overallConsumerAssignment);
            individualAssignment.topicsSubscriptions = tsl;
            topicsPerWorkerMap.put(consumerWorkers.get(i++), individualAssignment);
        }
        topicsPerWorkerMap.entrySet().parallelStream()
                .forEach(
                        e -> {
                            try {
                                e.getKey().createConsumers(e.getValue());
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        });
    }

    private void createThroughputProducers(ProducerAssignment producerAssignment) {
        List<List<String>> topicsPerProducer =
                ListPartition.partitionList(producerAssignment.topics, producerWorkers.size());
        Map<Integer, List<String>> topicsPerProducerMap = Maps.newHashMap();
        int i = 0;
        for (List<String> assignedTopics : topicsPerProducer) {
            topicsPerProducerMap.put(i++, assignedTopics);
        }

        // Number of actually used workers might be less than available workers
        numberOfUsedProducerWorkers =
                (int) topicsPerProducerMap.values().stream().filter(t -> !t.isEmpty()).count();
        log.debug(
                "Producing worker count: {} of {}", numberOfUsedProducerWorkers, producerWorkers.size());
        topicsPerProducerMap.entrySet().parallelStream()
                .forEach(
                        e -> {
                            try {
                                Integer producerIndex = e.getKey();
                                Worker worker = producerWorkers.get(producerIndex);
                                ProducerAssignment assignment =
                                        new ProducerAssignment(e.getValue()).withProducerIndex(producerIndex);
                                worker.createProducers(assignment);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        });
    }

    private void createTpcHProducers(ProducerAssignment producerAssignment) {
        numberOfUsedProducerWorkers = workers.size();
        IntStream.range(0, numberOfUsedProducerWorkers)
                .parallel()
                .forEach(
                        index -> {
                            Worker worker = workers.get(index);
                            try {
                                worker.createProducers(producerAssignment.withProducerIndex(index));
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        });
    }

    @Override
    public PeriodStats getPeriodStats() {
        return workers.parallelStream()
                .map(
                        w -> {
                            try {
                                return w.getPeriodStats();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                .reduce(new PeriodStats(), PeriodStats::plus);
    }

    @Override
    public CumulativeLatencies getCumulativeLatencies() {
        return workers.parallelStream()
                .map(
                        w -> {
                            try {
                                return w.getCumulativeLatencies();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                .reduce(new CumulativeLatencies(), CumulativeLatencies::plus);
    }

    @Override
    public CountersStats getCountersStats() throws IOException {
        return workers.parallelStream()
                .map(
                        w -> {
                            try {
                                return w.getCountersStats();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                .reduce(new CountersStats(), CountersStats::plus);
    }

    @Override
    public void resetStats() throws IOException {
        workers.parallelStream()
                .forEach(
                        w -> {
                            try {
                                w.resetStats();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
    }

    @Override
    public void close() throws Exception {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
        for (Worker w : workers) {
            try {
                w.close();
            } catch (Exception ignored) {
                log.trace("Ignored error while closing worker {}", w, ignored);
            }
        }
    }

    private static final Logger log = LoggerFactory.getLogger(DistributedWorkersEnsemble.class);
    private static final ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();

    private static String generateSubscriptionName(int index) {
        return String.format("sub-%03d-%s", index, RandomGenerator.getRandomString());
    }
}
