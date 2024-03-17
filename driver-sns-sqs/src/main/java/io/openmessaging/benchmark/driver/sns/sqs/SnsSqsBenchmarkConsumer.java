import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import io.openmessaging.benchmark.driver.BenchmarkConsumer;

public class SnsSqsBenchmarkConsumer implements RequestHandler<SQSEvent, Void>, BenchmarkConsumer {

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSMessage message : event.getRecords()) {
            String messageBody = message.getBody();
            processMessage(messageBody);
        }

        return null;
    }

    private void processMessage(String message) {
        System.out.println("Received message: " + message);
    }

    @Override
    public void close() throws Exception {}
}