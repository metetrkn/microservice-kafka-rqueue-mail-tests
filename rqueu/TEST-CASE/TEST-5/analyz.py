import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt

# 1. Load and combine the CSV files
files = ['../TEST-1/log_report.csv', '../TEST-2/log_report.csv', '../TEST-3/log_report.csv',
          '../TEST-4/log_report.csv']
df = pd.concat((pd.read_csv(f) for f in files), ignore_index=True)

# 2. Sort the Concurrency Axis
# Extract the lower bound (e.g., '5' from '5-10') to sort logically
df['sort_key'] = df['concurrency'].apply(lambda x: int(x.split('-')[0]))
df = df.sort_values('sort_key')

# 3. Create the Plots
fig, axes = plt.subplots(1, 2, figsize=(15, 6))

# Graph 1: Average Execution Time
sns.lineplot(
    ax=axes[0], 
    data=df, 
    x='concurrency', 
    y='avg_time(sc)', 
    hue='subject', 
    style='subject', 
    markers=True, 
    dashes=False
)
axes[0].set_title('Average Execution Time by Concurrency')
axes[0].set_ylabel('Avg Time (seconds)')
axes[0].grid(True, linestyle='--', alpha=0.7)

# Graph 2: Max Execution Time
sns.lineplot(
    ax=axes[1], 
    data=df, 
    x='concurrency', 
    y='max_time(sc)', 
    hue='subject', 
    style='subject', 
    markers=True, 
    dashes=False
)
axes[1].set_title('Max Execution Time by Concurrency')
axes[1].set_ylabel('Max Time (seconds)')
axes[1].grid(True, linestyle='--', alpha=0.7)

plt.tight_layout()
plt.savefig('concurrency_analysis.png')
plt.show()