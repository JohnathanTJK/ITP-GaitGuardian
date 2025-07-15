import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
import json
from pathlib import Path
import warnings
warnings.filterwarnings('ignore')

class TUGSeverityAnalyzer:
    """
    TUG (Timed Up and Go) Test Severity Analysis Tool
    Analyzes TUG test results and provides severity classifications and visualizations.
    """

    def __init__(self, csv_path=None):
        self.csv_path = csv_path
        self.df = None
        self.severity_levels = ['Normal', 'Slight', 'Mild', 'Moderate', 'Severe']
        self.severity_colors = {
            'Normal': '#2E8B57',
            'Slight': '#32CD32',
            'Mild': '#FFD700',
            'Moderate': '#FF8C00',
            'Severe': '#DC143C'
        }
        if csv_path:
            self.load_data(csv_path)

    def load_data(self, csv_path):
        """Load and preprocess the gait features CSV."""
        try:
            self.df = pd.read_csv(csv_path)
            print(f"✅ Loaded {len(self.df)} records from {csv_path}")

            # Add turn_walk_ratio and severity_level columns if missing
            if 'turn1_duration' in self.df.columns and 'turn2_duration' in self.df.columns:
                self.df['turn_time'] = self.df['turn1_duration'] + self.df['turn2_duration']
            else:
                self.df['turn_time'] = 0.0

            self.df['walk_time'] = self.df['total_time'] - self.df['turn_time']
            self.df['turn_walk_ratio'] = self.df['turn_time'] / self.df['walk_time'].replace(0, np.nan)
            self.df['turn_walk_ratio'] = self.df['turn_walk_ratio'].fillna(0)

            # Rename severity column if needed
            if 'severity' in self.df.columns:
                self.df['severity_level'] = self.df['severity']
            elif 'severity_level' not in self.df.columns:
                self.df['severity_level'] = 'Unknown'

            return True
        except Exception as e:
            print(f"❌ Error loading data: {e}")
            return False

    def classify_single_test(self, total_time, turn_walk_ratio, walk_time, turn_time):
        """Classify a single TUG test result."""
        classification = {
            'severity_level': 'Normal',
            'severity_score': 0,
            'total_time': total_time,
            'turn_walk_ratio': turn_walk_ratio,
            'walk_time': walk_time,
            'turn_time': turn_time,
            'rationale': ''
        }

        if total_time <= 7.0:
            classification['severity_level'] = 'Normal'
            classification['severity_score'] = 0
            classification['rationale'] = f"Completed in {total_time:.1f}s (≤7s), indicating normal mobility"
        elif total_time <= 13.0 and turn_walk_ratio < 1.0:
            classification['severity_level'] = 'Slight'
            classification['severity_score'] = 1
            classification['rationale'] = f"Completed in {total_time:.1f}s (≤13s) with turning ratio {turn_walk_ratio:.2f} (<1.0), indicating slight mobility issues"
        elif total_time <= 13.0 and 1.0 <= turn_walk_ratio <= 1.2:
            classification['severity_level'] = 'Mild'
            classification['severity_score'] = 2
            classification['rationale'] = f"Completed in {total_time:.1f}s (≤13s) with turning ratio {turn_walk_ratio:.2f} (≈1.0), indicating mild mobility issues with prolonged turning"
        elif total_time <= 13.0 and turn_walk_ratio > 1.2:
            classification['severity_level'] = 'Moderate'
            classification['severity_score'] = 3
            classification['rationale'] = f"Completed in {total_time:.1f}s (≤13s) but turning ratio {turn_walk_ratio:.2f} (>1.2), indicating moderate issues with turning"
        elif total_time > 13.0:
            if walk_time > 4.0 and turn_time > 4.0:
                classification['severity_level'] = 'Severe'
                classification['severity_score'] = 4
                classification['rationale'] = f"Completed in {total_time:.1f}s (>13s) with issues in both walking ({walk_time:.1f}s) and turning ({turn_time:.1f}s)"
            elif turn_walk_ratio > 1.0:
                classification['severity_level'] = 'Moderate'
                classification['severity_score'] = 3
                classification['rationale'] = f"Completed in {total_time:.1f}s (>13s) with turning ratio {turn_walk_ratio:.2f} (>1.0), indicating moderate issues primarily with turning"
            else:
                classification['severity_level'] = 'Severe'
                classification['severity_score'] = 4
                classification['rationale'] = f"Completed in {total_time:.1f}s (>13s), indicating severe mobility issues"
        return classification

    def generate_summary_report(self):
        """Generate a summary report of the loaded data."""
        if self.df is None:
            print("❌ No data loaded. Please load data first.")
            return

        print("\n" + "="*60)
        print("📊 TUG TEST SEVERITY ANALYSIS REPORT")
        print("="*60)

        total_tests = len(self.df)
        print(f"\n📈 OVERALL STATISTICS:")
        print(f"   Total Tests Analyzed: {total_tests}")
        print(f"   Average Total Time: {self.df['total_time'].mean():.2f}s")
        print(f"   Average Turn-Walk Ratio: {self.df['turn_walk_ratio'].mean():.2f}")

        severity_counts = self.df['severity_level'].value_counts()
        print(f"\n🎯 SEVERITY DISTRIBUTION:")
        for level in self.severity_levels:
            count = severity_counts.get(level, 0)
            percentage = (count / total_tests) * 100
            print(f"   {level:10}: {count:3d} tests ({percentage:5.1f}%)")

        print(f"\n📋 DETAILED STATISTICS BY SEVERITY:")
        for level in self.severity_levels:
            subset = self.df[self.df['severity_level'] == level]
            if len(subset) > 0:
                print(f"\n   {level.upper()}:")
                print(f"      Count: {len(subset)}")
                print(f"      Avg Total Time: {subset['total_time'].mean():.2f}s (±{subset['total_time'].std():.2f})")
                print(f"      Avg Turn-Walk Ratio: {subset['turn_walk_ratio'].mean():.2f} (±{subset['turn_walk_ratio'].std():.2f})")

        print(f"\n⚠️  RISK ASSESSMENT:")
        high_risk = len(self.df[self.df['severity_level'].isin(['Moderate', 'Severe'])])
        high_risk_pct = (high_risk / total_tests) * 100
        print(f"   High Risk (Moderate/Severe): {high_risk} tests ({high_risk_pct:.1f}%)")

        fall_risk = len(self.df[self.df['total_time'] > 13.0])
        fall_risk_pct = (fall_risk / total_tests) * 100
        print(f"   Fall Risk (>13s): {fall_risk} tests ({fall_risk_pct:.1f}%)")

        print("\n" + "="*60)

    def create_visualizations(self, save_path=None):
        """Create visualizations for severity analysis."""
        if self.df is None:
            print("❌ No data loaded. Please load data first.")
            return

        plt.style.use('seaborn-v0_8')
        fig = plt.figure(figsize=(18, 12))

        # 1. Severity Distribution (Pie Chart)
        ax1 = plt.subplot(2, 3, 1)
        severity_counts = self.df['severity_level'].value_counts()
        colors = [self.severity_colors.get(level, '#cccccc') for level in severity_counts.index]
        ax1.pie(severity_counts.values, labels=severity_counts.index, autopct='%1.1f%%', colors=colors, startangle=90)
        ax1.set_title('Severity Distribution', fontsize=14, fontweight='bold')

        # 2. Total Time Distribution by Severity
        ax2 = plt.subplot(2, 3, 2)
        for level in self.severity_levels:
            subset = self.df[self.df['severity_level'] == level]
            if len(subset) > 0:
                ax2.hist(subset['total_time'], alpha=0.7, label=level, color=self.severity_colors[level], bins=15)
        ax2.set_xlabel('Total Time (seconds)')
        ax2.set_ylabel('Frequency')
        ax2.set_title('Total Time by Severity')
        ax2.legend()
        ax2.axvline(x=7, color='green', linestyle='--', alpha=0.7, label='Normal threshold')
        ax2.axvline(x=13, color='red', linestyle='--', alpha=0.7, label='Fall risk threshold')

        # 3. Turn-Walk Ratio Distribution
        ax3 = plt.subplot(2, 3, 3)
        for level in self.severity_levels:
            subset = self.df[self.df['severity_level'] == level]
            if len(subset) > 0:
                ax3.hist(subset['turn_walk_ratio'], alpha=0.7, label=level, color=self.severity_colors[level], bins=15)
        ax3.set_xlabel('Turn-Walk Ratio')
        ax3.set_ylabel('Frequency')
        ax3.set_title('Turn-Walk Ratio by Severity')
        ax3.legend()
        ax3.axvline(x=1.0, color='orange', linestyle='--', alpha=0.7, label='1:1 ratio')

        # 4. Scatter Plot: Total Time vs Turn-Walk Ratio
        ax4 = plt.subplot(2, 3, 4)
        for level in self.severity_levels:
            subset = self.df[self.df['severity_level'] == level]
            if len(subset) > 0:
                ax4.scatter(subset['total_time'], subset['turn_walk_ratio'], c=self.severity_colors[level], label=level, alpha=0.7, s=50)
        ax4.set_xlabel('Total Time (seconds)')
        ax4.set_ylabel('Turn-Walk Ratio')
        ax4.set_title('Time vs Turn-Walk Ratio')
        ax4.legend()
        ax4.axhline(y=1.0, color='orange', linestyle='--', alpha=0.5)
        ax4.axvline(x=7, color='green', linestyle='--', alpha=0.5)
        ax4.axvline(x=13, color='red', linestyle='--', alpha=0.5)

        # 5. Box Plot: Total Time by Severity
        ax5 = plt.subplot(2, 3, 5)
        severity_order = [level for level in self.severity_levels if level in self.df['severity_level'].values]
        box_colors = [self.severity_colors[level] for level in severity_order]
        bp = ax5.boxplot([self.df[self.df['severity_level'] == level]['total_time'] for level in severity_order],
                         labels=severity_order, patch_artist=True)
        for patch, color in zip(bp['boxes'], box_colors):
            patch.set_facecolor(color)
            patch.set_alpha(0.7)
        ax5.set_ylabel('Total Time (seconds)')
        ax5.set_title('Total Time by Severity')
        ax5.tick_params(axis='x', rotation=45)

        # 6. Box Plot: Turn-Walk Ratio by Severity
        ax6 = plt.subplot(2, 3, 6)
        bp2 = ax6.boxplot([self.df[self.df['severity_level'] == level]['turn_walk_ratio'] for level in severity_order],
                          labels=severity_order, patch_artist=True)
        for patch, color in zip(bp2['boxes'], box_colors):
            patch.set_facecolor(color)
            patch.set_alpha(0.7)
        ax6.set_ylabel('Turn-Walk Ratio')
        ax6.set_title('Turn-Walk Ratio by Severity')
        ax6.tick_params(axis='x', rotation=45)

        plt.tight_layout()
        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            print(f"📊 Visualizations saved to: {save_path}")
        plt.show()

    def export_detailed_analysis(self, output_path):
        """Export detailed analysis to JSON."""
        if self.df is None:
            print("❌ No data loaded. Please load data first.")
            return

        analysis = {
            'summary': {
                'total_tests': len(self.df),
                'average_total_time': float(self.df['total_time'].mean()),
                'average_turn_walk_ratio': float(self.df['turn_walk_ratio'].mean()),
                'severity_distribution': self.df['severity_level'].value_counts().to_dict()
            },
            'severity_details': {},
            'risk_assessment': {
                'high_risk_count': int(len(self.df[self.df['severity_level'].isin(['Moderate', 'Severe'])])),
                'fall_risk_count': int(len(self.df[self.df['total_time'] > 13.0]))
            }
        }

        for level in self.severity_levels:
            subset = self.df[self.df['severity_level'] == level]
            if len(subset) > 0:
                analysis['severity_details'][level] = {
                    'count': int(len(subset)),
                    'total_time_stats': {
                        'mean': float(subset['total_time'].mean()),
                        'std': float(subset['total_time'].std()),
                        'min': float(subset['total_time'].min()),
                        'max': float(subset['total_time'].max())
                    },
                    'turn_walk_ratio_stats': {
                        'mean': float(subset['turn_walk_ratio'].mean()),
                        'std': float(subset['turn_walk_ratio'].std()),
                        'min': float(subset['turn_walk_ratio'].min()),
                        'max': float(subset['turn_walk_ratio'].max())
                    }
                }

        with open(output_path, 'w') as f:
            json.dump(analysis, f, indent=4)
        print(f"📄 Detailed analysis exported to: {output_path}")

def main():
    print("🚀 TUG Severity Analysis Tool")
    print("="*40)

    csv_path = "./computervision_test/data/gait_features_with_severity_ml.csv"
    analyzer = TUGSeverityAnalyzer(csv_path)

    # Generate summary report
    analyzer.generate_summary_report()

    # Create visualizations
    analyzer.create_visualizations(save_path="./computervision_test/data/tug_severity_analysis_ml.png")

    # Export detailed analysis
    analyzer.export_detailed_analysis("./computervision_test/data/tug_detailed_analysis_ml.json")

if __name__ == "__main__":
    main()