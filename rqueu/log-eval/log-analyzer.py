import re
import csv
from collections import defaultdict
import json
import os

# File Paths
STATE_FILE = 'state.json'
INPUT_LOG_FILE = r'C:/Users/mete/Desktop/staj/rqueue/rqueu-consumer/logs/email-consumer.log'
OUTPUT_CSV_FILE = 'log_report.csv'

def get_report_id():
    """Manage the report_id counter in a JSON state file."""
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
    # 1. Regex for Email Logs
    # Matches: ... | Subject: STANDARD | ... | Lag: 314ms
    email_pattern = re.compile(
        r"Subject:\s*(?P<subject>[^|]+)\s*\|.*?"
        r"Lag:\s*(?P<lag>\d+)ms",
        re.IGNORECASE
    )

    # 2. Regex for Concurrency Settings
    # Matches: 🚀 Starting HighConsumers. Concurrency range: 5-10
    concurrency_pattern = re.compile(
        r"Starting\s+(?P<type>High|Low)Consumers.*?Concurrency range:\s*(?P<range>[\d\-]+)",
        re.IGNORECASE
    )

    # Data Structures
    subject_stats = defaultdict(list)
    concurrency_map = {'High': 'Unknown', 'Low': 'Unknown'} # Default values
    total_errors = 0
    
    report_id = get_report_id()

    print(f"Processing file: {input_file}...")

    try:
        # READ LOGS
        with open(input_file, 'r', encoding='utf-8') as f:
            for line in f:
                # A. Count global errors
                if "error" in line.lower():
                    total_errors += 1

                # B. Check for Concurrency Settings
                conc_match = concurrency_pattern.search(line)
                if conc_match:
                    c_type = conc_match.group('type') # "High" or "Low"
                    c_range = conc_match.group('range') # "5-10"
                    concurrency_map[c_type] = c_range

                # C. Check for Email Logs
                email_match = email_pattern.search(line)
                if email_match:
                    subject = email_match.group('subject').strip()
                    lag_ms = int(email_match.group('lag'))
                    subject_stats[subject].append(lag_ms)

        # WRITE CSV
        file_exists = os.path.isfile(output_file) and os.path.getsize(output_file) > 0

        with open(output_file, 'a', newline='', encoding='utf-8') as csvfile:
            writer = csv.writer(csvfile)

            # Write Header
            if not file_exists:
                writer.writerow([
                    'report_id', 
                    'subject', 
                    'concurrency',  # New Column
                    'avg_time(sc)', 
                    'max_time(sc)', 
                    'total_mails', 
                    'total_errors'
                ])

            if subject_stats:
                for subject, times_ms in subject_stats.items():
                    count = len(times_ms)
                    
                    # Calculations
                    avg_s = (sum(times_ms) / count) / 1000.0
                    max_s = max(times_ms) / 1000.0
                    
                    # Determine Concurrency based on Subject
                    # Mapping: VIP -> High, STANDARD -> Low
                    current_concurrency = "Unknown"
                    if "VIP" in subject.upper():
                        current_concurrency = concurrency_map.get('High', 'Unknown')
                    elif "STANDARD" in subject.upper():
                        current_concurrency = concurrency_map.get('Low', 'Unknown')
                    
                    writer.writerow([
                        report_id,
                        subject,
                        current_concurrency,
                        f"{avg_s:.2f}",
                        f"{max_s:.2f}",
                        count,
                        total_errors
                    ])
            else:
                print("No valid email logs found matching pattern.")

        print(f"Success! Report appended to {output_file} (Report ID: {report_id})")

    except FileNotFoundError:
        print(f"Error: The input file '{input_file}' was not found.")
    except Exception as e:
        print(f"An unexpected error occurred: {e}")

if __name__ == "__main__":
    analyze_logs_to_csv(INPUT_LOG_FILE, OUTPUT_CSV_FILE)