import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates intelligent metrics and visualization recommendations
 * based on analyzed column data
 */
public class SmartAnalytics {
    
    /**
     * Generate automatic KPIs and metrics based on column analysis
     */
    public static List<DataIntelligence.MetricSuggestion> generateMetrics(
            List<DataIntelligence.ColumnInfo> columnInfos, 
            DataIntelligence.DataDomain domain) {
        
        List<DataIntelligence.MetricSuggestion> metrics = new ArrayList<>();
        
        // Find key columns for metric generation
        List<DataIntelligence.ColumnInfo> measures = columnInfos.stream()
            .filter(col -> col.type == DataIntelligence.ColumnType.NUMERIC_MEASURE)
            .collect(Collectors.toList());
        
        List<DataIntelligence.ColumnInfo> dimensions = columnInfos.stream()
            .filter(col -> col.type == DataIntelligence.ColumnType.CATEGORICAL || 
                          col.type == DataIntelligence.ColumnType.NUMERIC_DIMENSION)
            .collect(Collectors.toList());
        
        Optional<DataIntelligence.ColumnInfo> timeColumn = columnInfos.stream()
            .filter(col -> col.type == DataIntelligence.ColumnType.DATE_TIME)
            .findFirst();
        
        // Generate basic aggregation metrics
        for (DataIntelligence.ColumnInfo measure : measures) {
            metrics.add(new DataIntelligence.MetricSuggestion(
                "Total " + measure.name,
                "Sum of all " + measure.name + " values",
                new String[]{measure.name},
                "SUM"
            ));
            
            metrics.add(new DataIntelligence.MetricSuggestion(
                "Average " + measure.name,
                "Average " + measure.name + " per record",
                new String[]{measure.name},
                "AVG"
            ));
            
            // Add count metrics
            metrics.add(new DataIntelligence.MetricSuggestion(
                "Count of Records",
                "Total number of records",
                new String[]{measure.name},
                "COUNT"
            ));
        }
        
        // Generate domain-specific metrics
        switch (domain) {
            case SALES_COMMERCE:
                generateSalesMetrics(metrics, columnInfos);
                break;
            case FINANCIAL:
                generateFinancialMetrics(metrics, columnInfos);
                break;
            case HR_PEOPLE:
                generateHRMetrics(metrics, columnInfos);
                break;
            default:
                // Generic metrics already added above
                break;
        }
        
        // Generate comparative metrics
        if (timeColumn.isPresent() && !measures.isEmpty()) {
            DataIntelligence.ColumnInfo mainMeasure = measures.get(0);
            metrics.add(new DataIntelligence.MetricSuggestion(
                "Growth Rate",
                "Period-over-period growth in " + mainMeasure.name,
                new String[]{mainMeasure.name, timeColumn.get().name},
                "GROWTH"
            ));
        }
        
        return metrics;
    }
    
    private static void generateSalesMetrics(List<DataIntelligence.MetricSuggestion> metrics, 
                                           List<DataIntelligence.ColumnInfo> columnInfos) {
        // Look for sales-specific columns
        Optional<DataIntelligence.ColumnInfo> salesCol = columnInfos.stream()
            .filter(col -> col.name.toLowerCase().contains("sales") || 
                          col.name.toLowerCase().contains("revenue"))
            .findFirst();
        
        Optional<DataIntelligence.ColumnInfo> quantityCol = columnInfos.stream()
            .filter(col -> col.name.toLowerCase().contains("quantity") || 
                          col.name.toLowerCase().contains("qty"))
            .findFirst();
        
        if (salesCol.isPresent() && quantityCol.isPresent()) {
            metrics.add(new DataIntelligence.MetricSuggestion(
                "Average Sale Price",
                "Average price per unit sold",
                new String[]{salesCol.get().name, quantityCol.get().name},
                "CALCULATED"
            ));
        }
        
        // Add conversion and performance metrics
        metrics.add(new DataIntelligence.MetricSuggestion(
            "Sales Performance Index",
            "Relative performance compared to average",
            new String[]{salesCol.orElse(columnInfos.get(0)).name},
            "INDEX"
        ));
    }
    
    private static void generateFinancialMetrics(List<DataIntelligence.MetricSuggestion> metrics, 
                                               List<DataIntelligence.ColumnInfo> columnInfos) {
        // Financial-specific metrics
        metrics.add(new DataIntelligence.MetricSuggestion(
            "Budget Variance",
            "Difference between actual and budgeted amounts",
            new String[]{"actual", "budget"},
            "VARIANCE"
        ));
        
        metrics.add(new DataIntelligence.MetricSuggestion(
            "Cost Ratio",
            "Cost as percentage of total",
            new String[]{"cost", "total"},
            "RATIO"
        ));
    }
    
    private static void generateHRMetrics(List<DataIntelligence.MetricSuggestion> metrics, 
                                        List<DataIntelligence.ColumnInfo> columnInfos) {
        // HR-specific metrics
        metrics.add(new DataIntelligence.MetricSuggestion(
            "Headcount by Department",
            "Number of employees per department",
            new String[]{"department"},
            "COUNT_BY"
        ));
        
        metrics.add(new DataIntelligence.MetricSuggestion(
            "Average Tenure",
            "Average employee tenure",
            new String[]{"start_date"},
            "TENURE"
        ));
    }
    
    /**
     * Generate smart visualization recommendations
     */
    public static List<DataIntelligence.VisualizationSuggestion> generateVisualizations(
            List<DataIntelligence.ColumnInfo> columnInfos,
            DataIntelligence.DataDomain domain) {
        
        List<DataIntelligence.VisualizationSuggestion> visualizations = new ArrayList<>();
        
        // Find key columns for visualization
        List<DataIntelligence.ColumnInfo> measures = columnInfos.stream()
            .filter(col -> col.type == DataIntelligence.ColumnType.NUMERIC_MEASURE)
            .collect(Collectors.toList());
        
        List<DataIntelligence.ColumnInfo> categories = columnInfos.stream()
            .filter(col -> col.type == DataIntelligence.ColumnType.CATEGORICAL)
            .collect(Collectors.toList());
        
        Optional<DataIntelligence.ColumnInfo> timeColumn = columnInfos.stream()
            .filter(col -> col.type == DataIntelligence.ColumnType.DATE_TIME)
            .findFirst();
        
        // Time series visualization (highest priority if time data exists)
        if (timeColumn.isPresent() && !measures.isEmpty()) {
            DataIntelligence.ColumnInfo mainMeasure = measures.get(0);
            visualizations.add(new DataIntelligence.VisualizationSuggestion(
                "LINE_CHART",
                mainMeasure.name + " Over Time",
                new String[]{timeColumn.get().name},
                new String[]{mainMeasure.name},
                100  // Highest priority
            ));
            
            // Add trend analysis
            visualizations.add(new DataIntelligence.VisualizationSuggestion(
                "AREA_CHART",
                "Trend Analysis: " + mainMeasure.name,
                new String[]{timeColumn.get().name},
                new String[]{mainMeasure.name},
                90
            ));
        }
        
        // Categorical breakdown (pie charts for categories with reasonable distribution)
        for (DataIntelligence.ColumnInfo category : categories) {
            if (category.uniquenessRatio > 0.05 && category.uniquenessRatio < 0.5) {
                String measureName = measures.isEmpty() ? "Count" : measures.get(0).name;
                visualizations.add(new DataIntelligence.VisualizationSuggestion(
                    "PIE_CHART",
                    measureName + " by " + category.name,
                    new String[]{category.name},
                    measures.isEmpty() ? new String[]{"COUNT"} : new String[]{measures.get(0).name},
                    80
                ));
            }
        }
        
        // Bar charts for categorical comparisons
        if (!categories.isEmpty() && !measures.isEmpty()) {
            DataIntelligence.ColumnInfo topCategory = categories.stream()
                .filter(cat -> cat.uniquenessRatio < 0.3)  // Not too many categories
                .findFirst()
                .orElse(categories.get(0));
            
            visualizations.add(new DataIntelligence.VisualizationSuggestion(
                "BAR_CHART",
                measures.get(0).name + " by " + topCategory.name,
                new String[]{topCategory.name},
                new String[]{measures.get(0).name},
                85
            ));
        }
        
        // Correlation analysis (scatter plots for multiple measures)
        if (measures.size() >= 2) {
            visualizations.add(new DataIntelligence.VisualizationSuggestion(
                "SCATTER_PLOT",
                measures.get(0).name + " vs " + measures.get(1).name,
                new String[]{measures.get(0).name},
                new String[]{measures.get(1).name},
                70
            ));
        }
        
        // Domain-specific visualizations
        addDomainSpecificVisualizations(visualizations, columnInfos, domain);
        
        // Sort by priority (highest first)
        visualizations.sort((a, b) -> Integer.compare(b.priority, a.priority));
        
        return visualizations;
    }
    
    private static void addDomainSpecificVisualizations(
            List<DataIntelligence.VisualizationSuggestion> visualizations,
            List<DataIntelligence.ColumnInfo> columnInfos,
            DataIntelligence.DataDomain domain) {
        
        switch (domain) {
            case SALES_COMMERCE:
                // Sales funnel, conversion rates, seasonal analysis
                Optional<DataIntelligence.ColumnInfo> salesCol = columnInfos.stream()
                    .filter(col -> col.name.toLowerCase().contains("sales"))
                    .findFirst();
                
                if (salesCol.isPresent()) {
                    visualizations.add(new DataIntelligence.VisualizationSuggestion(
                        "HEATMAP",
                        "Sales Performance Heatmap",
                        new String[]{"month", "category"},
                        new String[]{salesCol.get().name},
                        75
                    ));
                }
                break;
                
            case FINANCIAL:
                // Budget vs actual, variance analysis
                visualizations.add(new DataIntelligence.VisualizationSuggestion(
                    "WATERFALL_CHART",
                    "Budget Variance Analysis",
                    new String[]{"category"},
                    new String[]{"variance"},
                    80
                ));
                break;
                
            case HR_PEOPLE:
                // Organizational charts, demographic breakdowns
                visualizations.add(new DataIntelligence.VisualizationSuggestion(
                    "TREEMAP",
                    "Employee Distribution",
                    new String[]{"department", "level"},
                    new String[]{"count"},
                    75
                ));
                break;
        }
    }
    
    /**
     * Generate intelligent KPI targets based on data analysis
     */
    public static Map<String, Double> generateIntelligentTargets(
            List<DataIntelligence.ColumnInfo> columnInfos,
            List<String[]> data) {
        
        Map<String, Double> targets = new HashMap<>();
        
        // Find numeric measures for target generation
        for (DataIntelligence.ColumnInfo col : columnInfos) {
            if (col.type == DataIntelligence.ColumnType.NUMERIC_MEASURE) {
                // Set target as 110% of current average (encouraging growth)
                double target = col.avgValue * 1.10;
                targets.put(col.name, target);
                
                // Add stretch target (125% of average)
                targets.put(col.name + "_stretch", col.avgValue * 1.25);
            }
        }
        
        return targets;
    }
    
    /**
     * Suggest optimal grouping and aggregation strategies
     */
    public static List<String> suggestGroupingStrategies(List<DataIntelligence.ColumnInfo> columnInfos) {
        List<String> strategies = new ArrayList<>();
        
        // Find good grouping columns (categorical with reasonable cardinality)
        List<String> goodGroupings = columnInfos.stream()
            .filter(col -> col.type == DataIntelligence.ColumnType.CATEGORICAL)
            .filter(col -> col.uniquenessRatio > 0.01 && col.uniquenessRatio < 0.5)
            .map(col -> col.name)
            .collect(Collectors.toList());
        
        strategies.addAll(goodGroupings);
        
        // Add time-based groupings if time column exists
        Optional<DataIntelligence.ColumnInfo> timeCol = columnInfos.stream()
            .filter(col -> col.type == DataIntelligence.ColumnType.DATE_TIME)
            .findFirst();
        
        if (timeCol.isPresent()) {
            strategies.add("Month");
            strategies.add("Quarter");
            strategies.add("Year");
        }
        
        return strategies;
    }
}