import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path
import json
import warnings
warnings.filterwarnings('ignore')

class TUGSeverityAnalyzer:
    """
    Comprehensive TUG (Timed Up and Go) Test Severity Analysis Tool
    
    This tool analyzes TUG test results and provides severity classifications
    based on total time and turning-to-walking ratio as per MDS-UPDRS guidelines.
    """
    
    def __init__(self, csv_path=None):
        """
        Initialize the analyzer
        
        Args:
            csv_path: Path to the CSV file containing gait features with severity data
        """
        self.csv_path = csv_path
        self.df = None
        self.severity_levels = ['Normal', 'Slight', 'Mild', 'Moderate', 'Severe']
        self.severity_colors = {
            'Normal': '#2E8B57',    # Sea Green
            'Slight': '#32CD32',    # Lime Green
            'Mild': '#FFD700',      # Gold
            'Moderate': '#FF8C00',  # Dark Orange
            'Severe': '#DC143C'     # Crimson
        }
        
        if csv_path:
            self.load_data(csv_path)
    
    def load_data(self, csv_path):
        """Load and validate the TUG analysis data"""
        try:
            self.df = pd.read_csv(csv_path)
            print(f"✅ Loaded {len(self.df)} records from {csv_path}")
            
            # Validate required columns
            required_cols = ['video', 'total_time', 'turn_walk_ratio', 'severity_level']
            missing_cols = [col for col in required_cols if col not in self.df.columns]
            if missing_cols:
                print(f"⚠️  Missing columns: {missing_cols}")
            
            return True
        except Exception as e:
            print(f"❌ Error loading data: {e}")
            return False
    
    def classify_single_test(self, total_time, turn_walk_ratio, walk_straight_time, turn_time):
        """
        Classify a single TUG test result
        
        Args:
            total_time: Total time to complete TUG test in seconds
            turn_walk_ratio: Ratio of turning time to walking straight time
            walk_straight_time: Time spent walking straight in seconds
            turn_time: Time spent turning in seconds
        
        Returns:
            dict: Classification result with severity level and rationale
        """
        
        # Calculate additional metrics for better classification
        has_walking_issues = walk_straight_time > 4.0  # Normal walking should be ~2-3 seconds
        has_turning_issues = turn_time > 4.0  # Normal turning should be ~2-3 seconds
        
        classification = {
            'severity_level': 'Normal',
            'severity_score': 0,
            'total_time': total_time,
            'turn_walk_ratio': turn_walk_ratio,
            'walk_straight_time': walk_straight_time,
            'turn_time': turn_time,
            'rationale': ''
        }
        
        if total_time <= 7.0:
            # Normal - healthy individuals (like us walking)
            classification['severity_level'] = 'Normal'
            classification['severity_score'] = 0
            classification['rationale'] = f"Completed in {total_time:.1f}s (≤7s), indicating normal mobility"
            
        elif total_time <= 13.0 and turn_walk_ratio < 1.0:
            # Slight - least severe
            classification['severity_level'] = 'Slight'
            classification['severity_score'] = 1
            classification['rationale'] = f"Completed in {total_time:.1f}s (≤13s) with turning ratio {turn_walk_ratio:.2f} (<1.0), indicating slight mobility issues"
            
        elif total_time <= 13.0 and 1.0 <= turn_walk_ratio <= 1.2:
            # Mild - slightly more severe, low risk of falling
            classification['severity_level'] = 'Mild'
            classification['severity_score'] = 2
            classification['rationale'] = f"Completed in {total_time:.1f}s (≤13s) with turning ratio {turn_walk_ratio:.2f} (≈1.0), indicating mild mobility issues with prolonged turning"
            
        elif total_time <= 13.0 and turn_walk_ratio > 1.2:
            # Moderate - turning takes significantly longer than walking
            classification['severity_level'] = 'Moderate'
            classification['severity_score'] = 3
            classification['rationale'] = f"Completed in {total_time:.1f}s (≤13s) but turning ratio {turn_walk_ratio:.2f} (>1.2), indicating moderate issues with turning"
            
        elif total_time > 13.0:
            # Severe - takes more than 13s
            if has_walking_issues and has_turning_issues:
                classification['severity_level'] = 'Severe'
                classification['severity_score'] = 4
                classification['rationale'] = f"Completed in {total_time:.1f}s (>13s) with issues in both walking ({walk_straight_time:.1f}s) and turning ({turn_time:.1f}s)"
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
        """Generate a comprehensive summary report"""
        if self.df is None:
            print("❌ No data loaded. Please load data first.")
            return
        
        print("\n" + "="*60)
        print("📊 TUG TEST SEVERITY ANALYSIS REPORT")
        print("="*60)
        
        # Overall statistics
        total_tests = len(self.df)
        print(f"\n📈 OVERALL STATISTICS:")
        print(f"   Total Tests Analyzed: {total_tests}")
        print(f"   Average Total Time: {self.df['total_time'].mean():.2f}s")
        print(f"   Average Turn-Walk Ratio: {self.df['turn_walk_ratio'].mean():.2f}")
        
        # Severity distribution
        severity_counts = self.df['severity_level'].value_counts()
        print(f"\n🎯 SEVERITY DISTRIBUTION:")
        for level in self.severity_levels:
            count = severity_counts.get(level, 0)
            percentage = (count / total_tests) * 100
            print(f"   {level:10}: {count:3d} tests ({percentage:5.1f}%)")
        
        # Detailed statistics by severity
        print(f"\n📋 DETAILED STATISTICS BY SEVERITY:")
        for level in self.severity_levels:
            subset = self.df[self.df['severity_level'] == level]
            if len(subset) > 0:
                print(f"\n   {level.upper()}:")
                print(f"      Count: {len(subset)}")
                print(f"      Avg Total Time: {subset['total_time'].mean():.2f}s (±{subset['total_time'].std():.2f})")
                print(f"      Avg Turn-Walk Ratio: {subset['turn_walk_ratio'].mean():.2f} (±{subset['turn_walk_ratio'].std():.2f})")
                if 'total_walking_time' in subset.columns:
                    print(f"      Avg Walking Time: {subset['total_walking_time'].mean():.2f}s")
                if 'total_turning_time' in subset.columns:
                    print(f"      Avg Turning Time: {subset['total_turning_time'].mean():.2f}s")
        
        # Risk assessment
        print(f"\n⚠️  RISK ASSESSMENT:")
        high_risk = len(self.df[self.df['severity_level'].isin(['Moderate', 'Severe'])])
        high_risk_pct = (high_risk / total_tests) * 100
        print(f"   High Risk (Moderate/Severe): {high_risk} tests ({high_risk_pct:.1f}%)")
        
        fall_risk = len(self.df[self.df['total_time'] > 13.0])
        fall_risk_pct = (fall_risk / total_tests) * 100
        print(f"   Fall Risk (>13s): {fall_risk} tests ({fall_risk_pct:.1f}%)")
        
        print("\n" + "="*60)
    
    def create_visualizations(self, save_path=None):
        """Create comprehensive visualizations"""
        if self.df is None:
            print("❌ No data loaded. Please load data first.")
            return
        
        # Set up the plotting style
        plt.style.use('seaborn-v0_8')
        fig = plt.figure(figsize=(20, 15))
        
        # 1. Severity Distribution (Pie Chart)
        ax1 = plt.subplot(2, 4, 1)
        severity_counts = self.df['severity_level'].value_counts()
        colors = [self.severity_colors[level] for level in severity_counts.index]
        wedges, texts, autotexts = ax1.pie(severity_counts.values, labels=severity_counts.index, 
                                          autopct='%1.1f%%', colors=colors, startangle=90)
        ax1.set_title('Severity Distribution', fontsize=14, fontweight='bold')
        
        # 2. Total Time Distribution by Severity
        ax2 = plt.subplot(2, 4, 2)
        for level in self.severity_levels:
            subset = self.df[self.df['severity_level'] == level]
            if len(subset) > 0:
                ax2.hist(subset['total_time'], alpha=0.7, label=level, 
                        color=self.severity_colors[level], bins=15)
        ax2.set_xlabel('Total Time (seconds)')
        ax2.set_ylabel('Frequency')
        ax2.set_title('Total Time Distribution by Severity')
        ax2.legend()
        ax2.axvline(x=7, color='green', linestyle='--', alpha=0.7, label='Normal threshold')
        ax2.axvline(x=13, color='red', linestyle='--', alpha=0.7, label='Fall risk threshold')
        
        # 3. Turn-Walk Ratio Distribution
        ax3 = plt.subplot(2, 4, 3)
        for level in self.severity_levels:
            subset = self.df[self.df['severity_level'] == level]
            if len(subset) > 0:
                ax3.hist(subset['turn_walk_ratio'], alpha=0.7, label=level, 
                        color=self.severity_colors[level], bins=15)
        ax3.set_xlabel('Turn-Walk Ratio')
        ax3.set_ylabel('Frequency')
        ax3.set_title('Turn-Walk Ratio Distribution by Severity')
        ax3.legend()
        ax3.axvline(x=1.0, color='orange', linestyle='--', alpha=0.7, label='1:1 ratio')
        
        # 4. Scatter Plot: Total Time vs Turn-Walk Ratio
        ax4 = plt.subplot(2, 4, 4)
        for level in self.severity_levels:
            subset = self.df[self.df['severity_level'] == level]
            if len(subset) > 0:
                ax4.scatter(subset['total_time'], subset['turn_walk_ratio'], 
                           c=self.severity_colors[level], label=level, alpha=0.7, s=50)
        ax4.set_xlabel('Total Time (seconds)')
        ax4.set_ylabel('Turn-Walk Ratio')
        ax4.set_title('Classification Map: Time vs Turn-Walk Ratio')
        ax4.legend()
        ax4.axhline(y=1.0, color='orange', linestyle='--', alpha=0.5)
        ax4.axvline(x=7, color='green', linestyle='--', alpha=0.5)
        ax4.axvline(x=13, color='red', linestyle='--', alpha=0.5)
        
        # 5. Box Plot: Total Time by Severity
        ax5 = plt.subplot(2, 4, 5)
        severity_order = [level for level in self.severity_levels if level in self.df['severity_level'].values]
        box_colors = [self.severity_colors[level] for level in severity_order]
        bp = ax5.boxplot([self.df[self.df['severity_level'] == level]['total_time'] for level in severity_order],
                        labels=severity_order, patch_artist=True)
        for patch, color in zip(bp['boxes'], box_colors):
            patch.set_facecolor(color)
            patch.set_alpha(0.7)
        ax5.set_ylabel('Total Time (seconds)')
        ax5.set_title('Total Time Distribution by Severity')
        ax5.tick_params(axis='x', rotation=45)
        
        # 6. Box Plot: Turn-Walk Ratio by Severity
        ax6 = plt.subplot(2, 4, 6)
        bp2 = ax6.boxplot([self.df[self.df['severity_level'] == level]['turn_walk_ratio'] for level in severity_order],
                         labels=severity_order, patch_artist=True)
        for patch, color in zip(bp2['boxes'], box_colors):
            patch.set_facecolor(color)
            patch.set_alpha(0.7)
        ax6.set_ylabel('Turn-Walk Ratio')
        ax6.set_title('Turn-Walk Ratio by Severity')
        ax6.tick_params(axis='x', rotation=45)
        
        # 7. Walking vs Turning Time Comparison
        ax7 = plt.subplot(2, 4, 7)
        if 'total_walking_time' in self.df.columns and 'total_turning_time' in self.df.columns:
            for level in self.severity_levels:
                subset = self.df[self.df['severity_level'] == level]
                if len(subset) > 0:
                    ax7.scatter(subset['total_walking_time'], subset['total_turning_time'], 
                               c=self.severity_colors[level], label=level, alpha=0.7, s=50)
            ax7.set_xlabel('Total Walking Time (seconds)')
            ax7.set_ylabel('Total Turning Time (seconds)')
            ax7.set_title('Walking vs Turning Time')
            ax7.legend()
            # Add diagonal line for 1:1 ratio
            max_val = max(ax7.get_xlim()[1], ax7.get_ylim()[1])
            ax7.plot([0, max_val], [0, max_val], 'k--', alpha=0.5, label='1:1 line')
        
        # 8. Severity Score Distribution
        ax8 = plt.subplot(2, 4, 8)
        if 'severity_score' in self.df.columns:
            score_counts = self.df['severity_score'].value_counts().sort_index()
            bars = ax8.bar(score_counts.index, score_counts.values, 
                          color=[self.severity_colors[self.severity_levels[i]] for i in score_counts.index])
            ax8.set_xlabel('Severity Score')
            ax8.set_ylabel('Count')
            ax8.set_title('Severity Score Distribution')
            ax8.set_xticks(range(5))
            ax8.set_xticklabels(self.severity_levels, rotation=45)
        
        plt.tight_layout()
        
        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            print(f"📊 Visualizations saved to: {save_path}")
        
        plt.show()
    
    def export_detailed_analysis(self, output_path):
        """Export detailed analysis to JSON"""
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
                'high_risk_count': len(self.df[self.df['severity_level'].isin(['Moderate', 'Severe'])]),
                'fall_risk_count': len(self.df[self.df['total_time'] > 13.0])
            }
        }
        
        # Detailed statistics by severity
        for level in self.severity_levels:
            subset = self.df[self.df['severity_level'] == level]
            if len(subset) > 0:
                analysis['severity_details'][level] = {
                    'count': len(subset),
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
    """Main function to demonstrate the TUG Severity Analyzer"""
    
    # Example usage
    print("🚀 TUG Severity Analysis Tool")
    print("="*40)
    
    # Initialize analyzer
    analyzer = TUGSeverityAnalyzer()
    
    # Example: Classify a single test
    print("\n📝 Example: Single Test Classification")
    result = analyzer.classify_single_test(
        total_time=15.5,
        turn_walk_ratio=1.3,
        walk_straight_time=6.0,
        turn_time=7.8
    )
    
    print(f"Severity Level: {result['severity_level']}")
    print(f"Severity Score: {result['severity_score']}")
    print(f"Rationale: {result['rationale']}")
    
    # Example: Load and analyze data from CSV
    csv_path = "./computervision/keypoints_and_durations/gait_features_with_severity.csv"
    
    if Path(csv_path).exists():
        print(f"\n📊 Loading data from: {csv_path}")
        analyzer.load_data(csv_path)
        
        # Generate summary report
        analyzer.generate_summary_report()
        
        # Create visualizations
        analyzer.create_visualizations(save_path="tug_severity_analysis.png")
        
        # Export detailed analysis
        analyzer.export_detailed_analysis("tug_detailed_analysis.json")
    else:
        print(f"⚠️  CSV file not found at: {csv_path}")
        print("Please ensure you have run the main analysis script first.")

if __name__ == "__main__":
    main()