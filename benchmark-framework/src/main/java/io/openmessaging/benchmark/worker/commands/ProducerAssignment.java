package io.openmessaging.benchmark.worker.commands;

import java.util.ArrayList;
import java.util.List;

public class ProducerAssignment {
    public List<String> topics;
    public boolean isTpcH = false;

    public ProducerAssignment() {
        this.topics = new ArrayList<>();
    }

    public ProducerAssignment(List<String> topics) {
        this.topics = topics;
    }
}
