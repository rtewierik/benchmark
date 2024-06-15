import json
import matplotlib.pyplot as plt
import numpy as np


def plot_latency_quantiles(json_data, label, technology, size, plot):
    publish_latency_quantiles = json_data["aggregatedPublishLatencyQuantiles"]
    end_to_end_latency_quantiles = json_data["aggregatedEndToEndLatencyQuantiles"]

    publish_x = list(map(float, publish_latency_quantiles.keys()))
    publish_y = list(publish_latency_quantiles.values())

    end_to_end_x = list(map(float, end_to_end_latency_quantiles.keys()))
    end_to_end_y = list(end_to_end_latency_quantiles.values())

    plt.plot(publish_x, publish_y, label=f'Publish latency quantiles ({label})')
    plt.plot(end_to_end_x, end_to_end_y, label=f'E2E latency quantiles ({label})')
    plt.xlabel('Quantiles')
    plt.ylabel('Latency (ms)')
    plt.title('Latency quantiles')
    plt.legend()
    plt.grid(True)

    if plot:
        plt.savefig(f'latency_quantiles_{technology}_{size}.png')


def plot_publish_consume_rates(json_data, label, technology, size, plot):
    publish_rates = json_data["publishRate"]
    consume_rates = json_data["consumeRate"]

    x = np.arange(len(publish_rates))

    plt.plot(x, publish_rates, label=f'Publish rate ({label})', marker='o')
    plt.plot(x, consume_rates, label=f'Consume rate ({label})', marker='o')
    plt.xlabel('Time interval')
    plt.ylabel('Rate (messages/second)')
    plt.title('Publish and consume rates')
    plt.legend()
    plt.grid(True)

    if plot:
        plt.savefig(f'publish_consume_rates_{technology}_{size}.png')


def plot_data(json_file_path, label, technology, size, plot, f):
    with open(json_file_path, 'r') as file:
        data = json.load(file)

    f(data, label, technology, size, plot)


def main(files, prefix, labels, technology, size, f):
    file_paths = [get_filepath(prefix, f) for f in files]

    fig = plt.figure(figsize=(10, 5))
    for i, file_path in enumerate(file_paths[:-1]):
        plot_data(file_path, labels[i], technology, size, False, f)
    plot_data(file_paths[-1], labels[-1], technology, size, True, f)
    plt.close(fig)


def get_filepath(prefix, filename):
    return f"{prefix}{filename}.json"


if __name__ == "__main__":
    sizes = ["10", "100", "500"]
    kafka_p = '../experiments/kafka/throughput/'
    kafka_100b_f = [
        'throughput-100b-10-max-Kafka-2024-05-28-22-07-10',
        'throughput-100b-100-max-Kafka-2024-05-28-22-10-51',
        'throughput-100b-500-max-Kafka-2024-05-28-22-15-43'
    ]
    kafka_1kb_f = [
        'throughput-1kb-10-max-Kafka-2024-05-28-22-30-20',
        'throughput-1kb-100-max-Kafka-2024-05-28-22-33-53',
        'throughput-1kb-500-max-Kafka-2024-05-28-22-38-32'
    ]
    kafka_10kb_f = [
        'throughput-10kb-10-max-Kafka-2024-05-28-22-18-56',
        'throughput-10kb-100-max-Kafka-2024-05-28-22-22-25',
        'throughput-10kb-500-max-Kafka-2024-05-28-22-27-07'
    ]

    pravega_p = '../experiments/pravega/throughput/'
    pravega_100b_f = [
        'throughput-100b-10-max-Pravega-2024-05-29-21-39-45',
        'throughput-100b-100-max-Pravega-2024-05-29-21-16-21',
        'throughput-100b-500-max-Pravega-2024-05-29-22-03-47'
    ]
    pravega_1kb_f = [
        'throughput-1kb-10-max-Pravega-2024-05-29-20-32-33',
        'throughput-1kb-100-max-Pravega-2024-05-29-20-56-12',
        'throughput-1kb-500-max-Pravega-2024-05-29-19-34-07'
    ]
    pravega_10kb_f = [
        'throughput-10kb-10-max-Pravega-2024-05-29-20-14-37',
        'throughput-10kb-100-max-Pravega-2024-05-29-14-38-27',
        'throughput-10kb-500-max-Pravega-2024-05-29-19-01-26'
    ]

    pulsar_p = '../experiments/pulsar/throughput/'
    pulsar_100b_f = [
        'throughput-100b-10-max-Pulsar-2024-05-28-18-52-28',
        'throughput-100b-100-max-Pulsar-2024-05-28-18-55-48',
        'throughput-100b-500-max-Pulsar-2024-05-28-18-59-14'
    ]
    pulsar_1kb_f = [
        'throughput-1kb-10-max-Pulsar-2024-05-28-19-05-45',
        'throughput-1kb-100-max-Pulsar-2024-05-28-19-09-01',
        'throughput-1kb-500-max-Pulsar-2024-05-28-19-12-26'
    ]
    pulsar_10kb_f = [
        'throughput-10kb-10-max-Pulsar-2024-05-28-19-02-29',
        'throughput-10kb-100-max-Pulsar-2024-05-28-18-44-42',
        'throughput-10kb-500-max-Pulsar-2024-05-28-18-48-04'
    ]

    f1, f2 = plot_latency_quantiles, plot_publish_consume_rates

    # Kafka
    main(kafka_100b_f, kafka_p, sizes, "kafka", "100b", f1)
    main(kafka_100b_f, kafka_p, sizes, "kafka", "100b", f2)

    main(kafka_1kb_f, kafka_p, sizes, "kafka", "1kb", f1)
    main(kafka_1kb_f, kafka_p, sizes, "kafka", "1kb", f2)

    main(kafka_10kb_f, kafka_p, sizes, "kafka", "10kb", f1)
    main(kafka_10kb_f, kafka_p, sizes, "kafka", "10kb", f2)

    # Pravega
    main(pravega_100b_f, pravega_p, sizes, "pravega", "100b", f1)
    main(pravega_100b_f, pravega_p, sizes, "pravega", "100b", f2)

    main(pravega_1kb_f, pravega_p, sizes, "pravega", "1kb", f1)
    main(pravega_1kb_f, pravega_p, sizes, "pravega", "1kb", f2)

    main(pravega_10kb_f, pravega_p, sizes, "pravega", "10kb", f1)
    main(pravega_10kb_f, pravega_p, sizes, "pravega", "10kb", f2)

    # Pulsar
    main(pulsar_100b_f, pulsar_p, sizes, "pulsar", "100b", f1)
    main(pulsar_100b_f, pulsar_p, sizes, "pulsar", "100b", f2)

    main(pulsar_1kb_f, pulsar_p, sizes, "pulsar", "1kb", f1)
    main(pulsar_1kb_f, pulsar_p, sizes, "pulsar", "1kb", f2)

    main(pulsar_10kb_f, pulsar_p, sizes, "pulsar", "10kb", f1)
    main(pulsar_10kb_f, pulsar_p, sizes, "pulsar", "10kb", f2)
