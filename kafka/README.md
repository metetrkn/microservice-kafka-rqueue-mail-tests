# Kafka Email Processing Project

## Overview

Test results and evaluation outputs are stored in the **TEST RESULTS** and **log-eval** folders.


#### Detailed Operation
- **Threading**: Two threads are started, each responsible for asynchronously sending messages to Kafka.
- **Batching**: Messages are grouped and sent in batches for efficiency.
- **Message Content**: Each message includes a unique ID, timestamp, and payload (e.g., email data).
- **Error Handling**: Failed sends are logged and may be retried depending on configuration.
## How the System Works

### Producer (kafkaProducer)
- **Concurrency**: Utilizes 2 asynchronous threads to publish messages to Kafka topics.
- **Topics**:
- **Message Publishing**:
  - **High Topic**: Publishes a total of 11,000 messages.
  - **Low Topic**: Publishes a total of 111,000 messages.

#### Detailed Operation
- **Partition Assignment**: For high-throughput topics, each consumer instance is assigned to a unique partition, ensuring parallel processing.
- **Threading**: Each consumer runs in its own thread, polling Kafka and processing batches.
- **Batch Processing**: Messages are processed in batches (size and poll interval configurable per topic).
- **Email Sending**: Each message triggers the `EmailSender`, which may simulate or actually send an email, and logs the outcome.
- **Offset Management**: Offsets are committed after successful batch processing to ensure at-least-once delivery.
- **Error Handling**: Processing errors are logged; failed messages may be retried or skipped based on logic.

### Consumer (javaConsumer)
  - **Consumers**: 6 consumer instances, each assigned to a partition.
  - **Threads**: 6 threads (one per consumer/partition).
  - **Batch Size**: 50 messages per poll.
  - **Parallelism**: Each consumer thread processes its batch independently, maximizing throughput.
- **Low Topic Consumption**:
  - **Partitions**: 1 partition.
  - **Consumers**: 1 consumer instance.

#### Detailed Operation
- **Parsing**: Reads consumer log files, extracting timestamps, message IDs, and processing results.
- **Metric Calculation**: Computes end-to-end latency, throughput, error rates, and batch statistics.
- **CSV Reporting**: Outputs detailed CSV files for each test scenario, including per-batch and per-message metrics.
- **Visualization**: Generates plots (e.g., latency histograms, throughput over time) for performance analysis.
  - **Threads**: 1 thread.
  - **Batch Size**: 200 messages per poll.

#### Detailed Operation
- **Scenario Documentation**: Each folder contains a description of the test scenario (topic/partition/batch configuration).
- **Log Storage**: Raw logs and generated reports for each test are stored for reproducibility and comparison.
- **Email Sending**: Each consumed message triggers the `EmailSender` logic, which simulates or sends an email and logs the result.


---

## Example Message Lifecycle

1. **Producer Thread** creates a message with a unique ID and timestamp, and sends it to the Kafka broker.
2. **Kafka Broker** stores the message in the appropriate topic partition.
3. **Consumer Thread** polls the partition, retrieves a batch, and processes each message (e.g., sends an email).
4. **Processing Event** is logged with timestamps for received, processed, and sent.
5. **Log Analyzer** parses the logs, calculates the latency for each message, and generates reports.
6. **Visualization** scripts create charts for further analysis.

---

## Performance and Evaluation

- **High Topic**: Designed to test parallelism and low-latency processing with many partitions and consumers.
- **Low Topic**: Simulates sequential, high-batch, low-frequency processing.
- **Metrics**: End-to-end latency, throughput, error rates, and batch statistics are computed and visualized.

---

## Customization & Extensibility

- **Kafka Configuration**: Easily adjust topic, partition, batch size, and poll interval in the source code.
- **Email Logic**: Swap out the email sending logic for integration with real SMTP servers or other services.
- **Log Analysis**: Extend the Python scripts to compute additional metrics or support new log formats.

---

## Troubleshooting

6. **Analysis**: Log files are analyzed by the log-eval scripts to compute metrics and generate reports.

### Latency Calculation
- **End-to-End Latency**: For each message, the time difference between the producer's publish timestamp and the consumer's processing timestamp is calculated.
- **Batch Latency**: Average, min, and max latencies are computed per batch and per scenario.
- **Reporting**: Latency statistics are included in the generated CSV reports and visualizations.

### Reporting and Evaluation
- **Log Generation**: The consumer writes detailed logs (including timestamps, message IDs, and processing results) to the `logs/` directory.
- **Log Analysis**: The `log-analyzer.py` script in `log-eval/` parses these logs, calculates metrics (latency, throughput, error rates), and outputs CSV reports.
- **Visualization**: The `visualize.py` script generates charts and graphs from the CSV data for further analysis.
- **Test Scenarios**: Each test scenario (e.g., different topic/partition/batch configurations) is documented in the `TEST RESULTS/` folder, with corresponding logs and reports.

## Folder Structure

```
javaConsumer/
  └─ javaConsumer/
      ├─ pom.xml
      ├─ logs/
      ├─ src/
      │   └─ main/java/org/consumer/
      │        ├─ EmailSender.java
      │        ├─ KafkaEmailConsumer.java
      │        └─ Main.java
      │   └─ resources/logback.xml
      └─ target/
kafkaProducer/
  ├─ pom.xml
  └─ src/main/java/org/producer/Main.java
log-eval/
  ├─ log_report.csv
  ├─ log-analyzer.py
  ├─ state.json
  ├─ time_report.csv
  └─ visualize.py
TEST RESULTS/
  └─ TEST1/ ... TEST7/
      ├─ log_report.csv
      ├─ testX-doc.txt
      └─ scnerio1.txt
```

---

## Module Details

### 1. kafkaProducer
- **Purpose**: Publishes messages (e.g., email events) to a Kafka topic.
- **Key File**: `src/main/java/org/producer/Main.java`
- **Build**: Uses Maven (`pom.xml`).
- **How to Run**:
  1. Navigate to the `kafkaProducer` directory.
  2. Build: `mvn clean package`
  3. Run: `java -cp target/classes org.producer.Main`
- **Configuration**: Update Kafka broker and topic details in the source code or configuration files as needed.

### 2. javaConsumer
- **Purpose**: Consumes messages from Kafka and processes them (e.g., sends emails).
- **Key Files**:
  - `EmailSender.java`: Handles email sending logic.
  - `KafkaEmailConsumer.java`: Kafka consumer implementation.
  - `Main.java`: Entry point.
- **Logging**: Uses `logback.xml` for logging configuration. Logs are stored in the `logs/` directory.
- **How to Run**:
  1. Navigate to `javaConsumer/javaConsumer`.
  2. Build: `mvn clean package`
  3. Run: `java -cp target/classes org.consumer.Main`
- **Configuration**: Update Kafka and email server settings in the source code or `resources`.

### 3. log-eval
- **Purpose**: Analyzes logs generated by the consumer and produces reports.
- **Key Files**:
  - `log-analyzer.py`: Main log analysis script.
  - `visualize.py`: Visualization of log data.
  - `log_report.csv`, `time_report.csv`: Output reports.
- **How to Use**:
  1. Ensure Python 3 is installed.
  2. Install dependencies (if any): `pip install -r requirements.txt` (create if needed).
  3. Run analysis: `python log-analyzer.py`
  4. Visualize: `python visualize.py`

### 4. TEST RESULTS
- **Purpose**: Stores results from various test scenarios.
- **Structure**: Each test folder (e.g., TEST1, TEST2, ...) contains log reports and documentation for that scenario.

---

## Prerequisites
- **Java 8+** and **Maven** for building and running producer/consumer modules.
- **Python 3** for log analysis.
- **Kafka** cluster running and accessible.
- **Email server** (SMTP) for email sending functionality.

---

## Setup & Usage

### 1. Start Kafka
Ensure your Kafka broker is running and accessible. Update connection details in both producer and consumer modules.

### 2. Build and Run Producer
```
cd kafkaProducer
mvn clean package
java -cp target/classes org.producer.Main
```

### 3. Build and Run Consumer
```
cd javaConsumer/javaConsumer
mvn clean package
java -cp target/classes org.consumer.Main
```

### 4. Analyze Logs
```
cd log-eval
python log-analyzer.py
python visualize.py
```

### 5. Review Test Results
Check the `TEST RESULTS` folder for scenario-specific logs and documentation.

---

## Logging & Reports
- **Logs**: Generated by the consumer, stored in `javaConsumer/javaConsumer/logs/`.
- **Reports**: Generated by log-eval scripts, stored in `log-eval/` and `TEST RESULTS/`.

---

## Customization
- **Kafka Topics/Brokers**: Update in source code or config files.
- **Email Settings**: Update SMTP server details in `EmailSender.java` or config.
- **Logback**: Adjust logging in `logback.xml`.

---

## Troubleshooting
- Ensure Kafka and SMTP servers are running and accessible.
- Check logs for errors in `logs/` and `log-eval/`.
- Verify Java and Python versions.

---

## License
Specify your license here.

---

## Authors
- Add your name and contact info here.

---

## Acknowledgements
- Kafka, Java, Python, and any libraries used.
