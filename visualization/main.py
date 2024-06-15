import json
import matplotlib.pyplot as plt
import datetime
import time
import numpy as np
from typing import List


def load_json_files(file_names: List[str]):
    data = []
    for file_name in file_names:
        with open(file_name, 'r') as file:
            data.append(json.load(file))
    return data


def match_tags(t, name, allow_client):
    return t['Key'] == 'Name' and name in t['Value'] and (allow_client or 'client' not in t['Value'])


def filter_by_metadata(data, label, name, allow_client):
    filtered_data = []
    for obj in data:
        instance_match = any(match_tags(t, name, allow_client) for t in obj['tags'])
        if obj['label'] == label and instance_match:
            filtered_data.append(obj)
    return filtered_data


def filter_by_timestamp_range(data, start_epoch, end_epoch):
    start_datetime = datetime.datetime.fromtimestamp(start_epoch, tz=datetime.timezone.utc)
    end_datetime = datetime.datetime.fromtimestamp(end_epoch, tz=datetime.timezone.utc)

    filtered_data = []
    for obj in data:
        filtered_timestamps = []
        filtered_values = []
        for ts, value in zip(obj['timestamps'], obj['values']):
            ts_datetime = datetime.datetime.strptime(ts, "%Y-%m-%dT%H:%M:%S%z")
            if start_datetime <= ts_datetime <= end_datetime:
                filtered_timestamps.append(ts_datetime)
                filtered_values.append(value)
        if filtered_timestamps:
            filtered_data.append({
                'instanceId': obj['instanceId'],
                'timestamps': filtered_timestamps,
                'values': filtered_values,
                'label': obj['label']
            })
    return filtered_data


def plot_data(data, file_name, label, y_label):
    fig = plt.figure(figsize=(10, 5))
    for j, obj in enumerate(data):
        timestamps = obj['timestamps']
        values = np.array(obj['values'])
        timestamps_actual = timestamps[:2] if "Bytes" in label else timestamps
        values_actual = values[:2] / 1000000 / 60 if "Bytes" in label else values
        plt.plot(timestamps_actual, values_actual, marker='o', linestyle='-')
        plt.xlabel('Timestamp')
        plt.ylabel(y_label)
        plt.title(f"{obj['label']} - {obj['instanceId']}")
        plt.xticks(rotation=45)
        plt.tight_layout()
    plt.savefig(file_name)
    plt.close(fig)


def main(
    file_names: List[str],
    start_epochs: List[int],
    end_epochs: List[int],
    label: str,
    name_tag: str,
    y_label: str,
    allow_client: bool
):
    json_data = load_json_files(file_names)

    for i, file_data in enumerate(json_data):
        start_epoch, end_epoch = start_epochs[i], end_epochs[i]
        filtered_by_metadata = filter_by_metadata(file_data, label, name_tag, allow_client)
        filtered_by_time = filter_by_timestamp_range(filtered_by_metadata, start_epoch, end_epoch)
        file_name = f"{label}_{i}.png"\
            .replace("AWS/EC2 ", "")\
            .replace("CWAgent ", "")\
            .replace(" ", "_")\
            .replace(".json", "")
        plot_data(filtered_by_time, file_name, label, y_label)


if __name__ == "__main__":
    kafka_ec2_metrics = ['../experiments/kafka/throughput/ec2_metrics.json'] * 9
    kafka_starts = [
        1716933836,
        1716934036,
        1716934259,
        1716934548,
        1716934743,
        1716934952,
        1716935232,
        1716935428,
        1716935639
    ]
    kafka_ends = [
        1716934030,
        1716934232,
        1716934459,
        1716934735,
        1716934932,
        1716935144,
        1716935420,
        1716935620,
        1716935835
    ]

    main(
        kafka_ec2_metrics,
        kafka_starts,
        kafka_ends,
        'AWS/EC2 DiskWriteBytes',
        'kafka',
        'MB/s',
        False)
    main(
        kafka_ec2_metrics,
        kafka_starts,
        kafka_ends,
        'AWS/EC2 DiskReadBytes',
        'kafka',
        'MB/s',
        False)
    main(
        kafka_ec2_metrics,
        kafka_starts,
        kafka_ends,
        'CWAgent CPUUtilization',
        'kafka',
        'CPU utilization %',
        False)
    main(
        kafka_ec2_metrics,
        kafka_starts,
        kafka_ends,
        'CWAgent mem_used_percent',
        'kafka',
        'Memory utilization %',
        False)