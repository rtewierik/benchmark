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


def filter_by_metadata(data, label, name):
    filtered_data = []
    for obj in data:
        if obj['label'] == label and any(tag['Key'] == 'Name' and name in tag['Value'] for tag in obj['tags']):
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


def plot_data(data, file_name):
    for obj in data:
        fig = plt.figure(figsize=(10, 5))
        plt.plot(obj['timestamps'], np.array(obj['values']) / 1000000 / 60, marker='o', linestyle='-')
        plt.xlabel('Timestamp')
        plt.ylabel('MB/s')
        plt.title(f"{obj['label']} - {obj['instanceId']}")
        plt.xticks(rotation=45)
        plt.tight_layout()
        plt.savefig(file_name)
        plt.close(fig)


def main(file_names: List[str], start_epochs: List[int], end_epochs: List[int], label: str, name_tag: str):
    json_data = load_json_files(file_names)

    for i, file_data in enumerate(json_data):
        start_epoch, end_epoch = start_epochs[i], end_epochs[i]
        filtered_by_metadata = filter_by_metadata(file_data, label, name_tag)
        filtered_by_time = filter_by_timestamp_range(filtered_by_metadata, start_epoch, end_epoch)
        file_name = f"{file_names[i]}_{i}.png"
        plot_data(filtered_by_time, file_name)


if __name__ == "__main__":
    main(
        [
            '../experiments/kafka/throughput/ec2_metrics.json',
            '../experiments/kafka/throughput/ec2_metrics.json',
            '../experiments/kafka/throughput/ec2_metrics.json',
            '../experiments/kafka/throughput/ec2_metrics.json',
            '../experiments/kafka/throughput/ec2_metrics.json',
            '../experiments/kafka/throughput/ec2_metrics.json',
            '../experiments/kafka/throughput/ec2_metrics.json',
            '../experiments/kafka/throughput/ec2_metrics.json',
            '../experiments/kafka/throughput/ec2_metrics.json'
        ],
        [
            1716933836,
            1716934036,
            1716934259,
            1716934548,
            1716934743,
            1716934952,
            1716935232,
            1716935428,
            1716935639
        ],
        [
            1716934030,
            1716934232,
            1716934459,
            1716934735,
            1716934932,
            1716935144,
            1716935420,
            1716935620,
            1716935835
        ],
        'AWS/EC2 DiskWriteBytes',
        'kafka')