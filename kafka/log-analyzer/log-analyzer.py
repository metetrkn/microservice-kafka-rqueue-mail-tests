import re
import csv
from collections import defaultdict
import json
import os

STATE_FILE = 'state.json'
TIME_REPORT_FILE = 'time_report.csv'

def get_report_id():
    if not os.path.exists(STATE_FILE):
        with open(STATE_FILE, 'w') as f:
            json.dump({'report_count': 0}, f)

    with open(STATE_FILE, 'r') as f:
        try:
            state = json.load(f)
        except json.JSONDecodeError:
            state = {'report_count': 0}

    current_count = state.get('report_count', 0)
    new_count = current_count + 1
    state['report_count'] = new_count

    with open(STATE_FILE, 'w') as f:
        json.dump(state, f, indent=4)
        
    return current_count


def analyze_logs_to_csv(input_file, output_file):
    # Regex handles the "Sent to: email |" section
    log_pattern = re.compile(
        r"Topic:\s*(?P<topic>[^|]+)\s*\|\s*"
        r"Consumer:\s*(?P<consumer>[^|]+)\s*\|\s*"
        r"Sent to:\s*[^|]+\s*\|\s*"
        r"Created:\s*(?P<created>\d+)\s*\|\s*"
        r"Sent:\s*(?P<sent>\d+)"
    )

    stats = defaultdict(list)
    report_id = get_report_id()
    
    # Initialize error counter
    total_errors = 0

    try:
        # 1. Read and Parse
        with open(input_file, 'r', encoding='utf-8') as f:
            for line in f:
                # Check for ERROR simply by string matching
                if "ERROR" in line:
                    total_errors += 1

                # Check for successful send log
                match = log_pattern.search(line)
                if match:
                    topic = match.group('topic').strip()
                    consumer = match.group('consumer').strip()
                    created_ts = int(match.group('created'))
                    sent_ts = int(match.group('sent'))

                    exec_time_ms = sent_ts - created_ts
                    stats[(topic, consumer)].append(exec_time_ms)

        # 2. CALCULATE GRAND TOTAL (Sum of all emails in this run)
        grand_total_mails = sum(len(times) for times in stats.values())

        # 3. Write to Main Report CSV (log_report.csv)
        file_exists = os.path.isfile(output_file) and os.path.getsize(output_file) > 0

        with open(output_file, 'a', newline='', encoding='utf-8') as csvfile:
            writer = csv.writer(csvfile)

            if not file_exists:
                writer.writerow(['report_id', 'topic', 'consumer', 'total_mails', 'average_execution_time_(s)', 'max_execution_time_(s)', 'grand_total_mails', 'total_errors'])

            for (topic, consumer), times_ms in stats.items():
                total_count = len(times_ms)
                
                avg_time_ms = sum(times_ms) / total_count
                max_time_ms = max(times_ms)

                avg_time_s = avg_time_ms / 1000.0
                max_time_s = max_time_ms / 1000.0
                
                writer.writerow([
                    report_id, 
                    topic, 
                    consumer, 
                    total_count, 
                    f"{avg_time_s:.2f}", 
                    f"{max_time_s:.2f}",
                    grand_total_mails,
                    total_errors
                ])

        # 4. [NEW] Aggregation for Time Report (time_report.csv)
        # Group raw times by priority (starts with 'high' or 'low')
        priority_stats = defaultdict(list)
        
        for (topic, consumer), times_ms in stats.items():
            topic_lower = topic.lower()
            if topic_lower.startswith("high"):
                priority_stats["high"].extend(times_ms)
            elif topic_lower.startswith("low"):
                priority_stats["low"].extend(times_ms)

        # Write to Time Report CSV
        tr_exists = os.path.isfile(TIME_REPORT_FILE) and os.path.getsize(TIME_REPORT_FILE) > 0
        
        with open(TIME_REPORT_FILE, 'a', newline='', encoding='utf-8') as tr_file:
            tr_writer = csv.writer(tr_file)
            
            # Header
            if not tr_exists:
                tr_writer.writerow(['report_id', 'priority', 'avr_execution_time', 'max_execution_time'])
            
            # Write rows for High and Low
            # We explicitly check for 'high' and 'low' to ensure order, or iterate the dict
            for priority in ['high', 'low']:
                times = priority_stats.get(priority)
                if times:
                    # Calculate overall average and max for the priority group
                    group_avg_s = (sum(times) / len(times)) / 1000.0
                    group_max_s = max(times) / 1000.0
                    
                    tr_writer.writerow([
                        report_id,
                        priority,
                        f"{group_avg_s:.2f}",
                        f"{group_max_s:.2f}"
                    ])

        print(f"Success! Reports updated: {output_file} & {TIME_REPORT_FILE} (Total Errors: {total_errors})")

    except FileNotFoundError:
        print(f"Error: The file '{input_file}' was not found.")

if __name__ == "__main__":
    analyze_logs_to_csv('C:/Users/mete/Desktop/staj/test-case/test/javaConsumer/javaConsumer/logs/email-consumer.log',
                        'log_report.csv')