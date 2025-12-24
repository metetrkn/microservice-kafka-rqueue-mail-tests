import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt

# 1. Load Data
df = pd.read_csv('log_report.csv')

# --- PRE-PROCESSING FOR MAILS GRAPH ---
# Create a unique label for consumers so "Consumer 0 (High)" is distinct from "Consumer 0 (Low)"
df['consumer_label'] = df['topic'].apply(lambda x: x.split('-')[0].capitalize()) + ": " + df['consumer'].astype(str)

# --- AGGREGATION FOR TIME GRAPHS ---
# Group by Report & Topic for the Max/Avg charts (merging consumers)
df_agg = df.groupby(['report_id', 'topic']).agg({
    'max_execution_time_(s)': 'max',
    'average_execution_time_(s)': 'mean'
}).reset_index()

# 2. Setup Visualization (2 Rows, 2 Columns)
sns.set_theme(style="whitegrid")
# Create a grid: Top row has 2 cols, Bottom row spans both
fig = plt.figure(figsize=(14, 10))
gs = fig.add_gridspec(2, 2)

ax1 = fig.add_subplot(gs[0, 0]) # Top Left
ax2 = fig.add_subplot(gs[0, 1]) # Top Right
ax3 = fig.add_subplot(gs[1, :]) # Bottom (Spans entire width)

# --- Graph 1: Max Time (Top Left) ---
sns.barplot(
    data=df_agg,
    x='report_id',
    y='max_execution_time_(s)',
    hue='topic',
    palette='viridis',
    ax=ax1
)
ax1.set_title('Max Execution Time (Aggregated by Topic)')

# --- Graph 2: Avg Time (Top Right) ---
sns.barplot(
    data=df_agg,
    x='report_id',
    y='average_execution_time_(s)',
    hue='topic',
    palette='viridis',
    ax=ax2
)
ax2.set_title('Avg Execution Time (Aggregated by Topic)')

# Get all unique consumer labels (e.g., "High: 0", "Low: 2")
unique_consumers = df['consumer_label'].unique()

# Create a dictionary mapping: If "High" is in the name -> Red, else -> Blue
custom_colors = {
    label: 'red' if 'High' in label else 'blue' 
    for label in unique_consumers
}

# --- Graph 3: Total Mails (Bottom) ---
sns.pointplot(
    data=df,
    x='report_id',
    y='total_mails',
    hue='consumer_label',
    palette=custom_colors,  # <--- CHANGED: Use the custom dictionary here
    dodge=True,
    linestyle='none',
    markersize=5,
    linewidth=1,
    ax=ax3
)
ax3.grid(True, axis='y', alpha=0.3)
ax3.set_title('Total Mails Handled (Point Comparison)')
ax3.legend(bbox_to_anchor=(1.0, 1.0), loc='upper left')

# --- 4. SHOW THE PLOT ---
plt.tight_layout()  # Fixes overlap between subplots
plt.show()          # Actually opens the window