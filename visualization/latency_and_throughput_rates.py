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


def main(files, label, technology, size, f):
    fig = plt.figure(figsize=(10, 5))

    for file in files[:-1]:
        plot_data(file, label, technology, size, False, f)
    plot_data(files[-1], label, technology, size, True, f)

    plt.close(fig)


def get_filepath(prefix, filename):
    return f"{prefix}{filename}"


if __name__ == "__main__":
    kafka_p = '../experiments/kafka/throughput/'
    kafka_1kb_f = [
        'throughput-1kb-10-max-Kafka-2024-05-28-22-30-20.json',
        'throughput-1kb-100-max-Kafka-2024-05-28-22-33-53.json',
        'throughput-1kb-500-max-Kafka-2024-05-28-22-38-32.json'
    ]
    kafka_f = [get_filepath(kafka_p, f) for f in kafka_1kb_f]

    f1, f2 = plot_latency_quantiles, plot_publish_consume_rates

    main(kafka_f, "10", "kafka", "1kb", f1)
    main(kafka_f, "10", "kafka", "1kb", f2)
