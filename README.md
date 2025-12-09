# High-Performance Kafka Email Consumer

A high-throughput, latency-aware Kafka Consumer application written in Java. This project demonstrates how to handle distinct priority queues (High vs. Low) with dedicated thread pools and dynamic batch sizing, simulating a production email dispatch system using HTTP APIs. Values currently are samples and will be fixed when all project ends.

## 🚀 Features

* **Priority Queues:** Separated processing for `high-priority-mails` and `low-priority-mails` to ensure critical tasks are not blocked by bulk traffic.
* **Dedicated Thread Pools:**
    * **High Priority:** Low latency, smaller batch sizes (10 threads).
    * **Low Priority:** High throughput, larger batch sizes (40 threads).
* **Manual Flow Control:** strict 1-to-1 mapping between Kafka batch sizes and worker threads to prevent pool saturation.
* **HTTP Integration:** Replaces legacy SMTP with modern HTTP API calls (Java `HttpClient`).
* **Simulation Mode:** Integrated with **WireMock** to simulate API latency and test backpressure handling without sending real emails.
* **Graceful Shutdown:** Ensures all in-flight batches complete before the application stops.

## 🛠️ Tech Stack

* **Language:** Java 11+ (Required for `java.net.http.HttpClient`)
* **Messaging:** Apache Kafka (Clients 3.x)
* **JSON Processing:** `org.json`
* **Testing/Simulation:** WireMock (Docker)
* **Build Tool:** Maven/Gradle (implied)

## 🏗️ Architecture

The system uses a "Consumer-Worker" pattern where the Kafka poller hands off entire batches to a specific executor service depending on the priority.


graph TD
    K[Kafka Cluster] -->|Topic: high-priority-mails| C1[High Priority Consumers]
    K -->|Topic: low-priority-mails| C2[Low Priority Consumers]
    
    C1 -->|Batch Size: 10| WP1[High Worker Pool (10 Threads)]
    C2 -->|Batch Size: 40| WP2[Low Worker Pool (40 Threads)]
    
    WP1 -->|HTTP POST| API[Email API (WireMock)]
    WP2 -->|HTTP POST| API


## ⚙️ Configuration
Performance tuning is centralized in Main.java. The system couples thread pool size with Kafka consumer batch limits to ensure stability.

JAVA
// kafkaConsumer/Main.java
private static final int HIGH_THREADS = 10; // Sets ThreadPool + Poll Batch Size
private static final int LOW_THREADS  = 40; // Sets ThreadPool + Poll Batch Size

## 🏃 How to Run
1. Start Prerequisites
You need a running Kafka instance and Zookeeper.

2. Start WireMock (Email API Simulator)
We use WireMock to mimic an email provider (like Mailgun) with artificial latency to test performance.

Bash
docker run -it --rm \
  -p 8080:8080 \
  --name wiremock \
  wiremock/wiremock:latest \
  --verbose

  Note: The code assumes WireMock is running at http://localhost:8080.

3. Run the Application
Execute the Main class. The application will start:

4 High-Priority Consumers

7 Low-Priority Consumers

4. Produce Test Data
Send JSON messages to the Kafka topics:

Topic: high-priority-mails

JSON
    {
    "to": "vip@example.com",
    "subject": "Urgent Alert",
    "body": "System is down",
    "createdAt": 1700000000000
    }

## 📂 Project Structure
kafkaConsumer -> Queries kafka, recieves mails and send to mail sender client, logs the execution times
kafkaProducer -> Creates mock mails simultaneously based on 2 topics.
log-analyzer -> Python project that analyz logs in kafkaConsumer

