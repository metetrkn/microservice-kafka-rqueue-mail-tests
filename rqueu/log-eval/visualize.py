import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt

# 1. Load Data
df = pd.read_csv('log_report.csv')

# --- PRE-PROCESSING ---
# Rename STANDARD to STD
df['subject'] = df['subject'].replace({'STANDARD': 'STD'})

df['report_id'] = df['report_id'].astype(str)
df[['min_conc', 'max_conc']] = df['concurrency'].str.split('-', expand=True).astype(float)

# 2. Setup Visualization
sns.set_theme(style="white", context="talk")

fig = plt.figure(figsize=(16, 12))
gs = fig.add_gridspec(2, 2)
fig.suptitle('Email Consumer Performance Report-RQUEUE', fontsize=16, weight='bold', y=0.98)

ax1 = fig.add_subplot(gs[0, 0])
ax2 = fig.add_subplot(gs[0, 1])
ax3 = fig.add_subplot(gs[1, 0])
ax4 = fig.add_subplot(gs[1, 1])

# Helper function to style legends
def style_legend(ax):
    # loc='upper right': Anchor the top-right corner of the legend...
    # bbox_to_anchor=(1, -0.12): ...to a point slightly BELOW the x-axis (y < 0).
    # This places it nicely under the axis labels on the right side.
    ax.legend(loc='upper right', bbox_to_anchor=(1, -0.12), 
              prop={'size': 15}, title=None, frameon=False, ncol=2) 
              # Added ncol=2 to make it flatter/wider if needed, or remove for vertical

# --- Graph 1: Max Time ---
sns.barplot(data=df, x='report_id', y='max_time(sc)', hue='subject', 
            palette='icefire', edgecolor='white', linewidth=1.5, ax=ax1)
ax1.set_title('Max Execution Time')
ax1.set_ylabel('Seconds')
ax1.grid(axis='y', linestyle='--', alpha=0.5)
style_legend(ax1)
sns.despine(left=True, bottom=True)

# --- Graph 2: Avg Time ---
sns.barplot(data=df, x='report_id', y='avg_time(sc)', hue='subject', 
            palette='inferno', edgecolor='chartreuse', linewidth=1.5, ax=ax2)
ax2.set_title('Avg Execution Time')
ax2.set_ylabel('Seconds')
ax2.grid(axis='y', linestyle='--', alpha=0.5)
style_legend(ax2)
sns.despine(left=True, bottom=True)

# --- Graph 3: Total Mails ---
sns.barplot(data=df, x='report_id', y='total_mails', hue='subject', 
            palette='viridis', edgecolor='white', linewidth=1.5, ax=ax3)
ax3.set_title('Total Volume')
ax3.set_ylabel('Count')
ax3.grid(axis='y', linestyle='--', alpha=0.5)
style_legend(ax3)
sns.despine(left=True, bottom=True)

# --- Graph 4: Concurrency Range ---
# 1. Plot MAX
sns.barplot(data=df, x='report_id', y='max_conc', hue='subject', 
            palette='magma', edgecolor='white', linewidth=0, ax=ax4, dodge=True)

# 2. Plot MIN (Mask)
unique_subjects = df['subject'].unique()
white_palette = {subj: 'white' for subj in unique_subjects}

sns.barplot(data=df, x='report_id', y='min_conc', hue='subject', 
            palette=white_palette, edgecolor='white', linewidth=0, ax=ax4, dodge=True, legend=False)

ax4.set_title('Concurrency Range')
ax4.set_ylabel('Threads')
ax4.grid(axis='y', linestyle='--', alpha=0.5)
style_legend(ax4)
sns.despine(left=True, bottom=True)

# --- Final Layout Adjustments ---
plt.tight_layout()
# Increased hspace to 0.5 to give the legends under the top charts enough room
plt.subplots_adjust(top=0.9, hspace=0.5, wspace=0.2) 

plt.show()