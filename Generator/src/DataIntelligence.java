import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Intelligent data analysis system that automatically classifies columns,
 * generates metrics, and suggests visualizations for any dataset
 */
public class DataIntelligence {
    
    // Column classification types
    public enum ColumnType {
        NUMERIC_MEASURE,     // Revenue, quantity, score - for aggregation
        NUMERIC_DIMENSION,   // ID, year, category code - for grouping
        CATEGORICAL,         // Department, region, status - for grouping
        DATE_TIME,          // Dates and timestamps - for time series
        TEXT_ID,            // Unique identifiers - usually ignore in viz
        TEXT_DESCRIPTION,   // Long text fields - usually ignore in viz
        BOOLEAN             // Yes/no, true/false - for filtering
    }
    
    // Data domain detection
    public enum DataDomain {
        SALES_COMMERCE,     // Sales, revenue, products, customers
        FINANCIAL,          // Accounting, budgets, expenses, profit
        OPERATIONAL,        // Performance, efficiency, quality metrics
        HR_PEOPLE,          // Employee data, performance, demographics
        MARKETING,          // Campaigns, engagement, conversion metrics
        GENERIC            // Unknown or mixed domain
    }
    
    // Column metadata after analysis
    public static class ColumnInfo {
        public String name;
        public ColumnType type;
        public boolean isKey;           // Primary measure or dimension
        public boolean isTimeColumn;    // Main time dimension
        public double uniquenessRatio;  // Unique values / total values
        public List<String> sampleValues;
        public Map<String, Integer> valueCounts; // For categorical analysis
        public double minValue, maxValue, avgValue; // For numeric analysis
        public String detectedPattern;   // Date format, ID pattern, etc.
        
        public ColumnInfo(String name) {
            this.name = name;
            this.sampleValues = new ArrayList<>();
            this.valueCounts = new HashMap<>();
        }
    }
    
    // Smart metrics suggestions
    public static class MetricSuggestion {
        public String name;
        public String description;
        public String[] requiredColumns;
        public String aggregationType; // SUM, AVG, COUNT, etc.
        public String formula;
        
        public MetricSuggestion(String name, String desc, String[] cols, String aggType) {
            this.name = name;
            this.description = desc;
            this.requiredColumns = cols;
            this.aggregationType = aggType;
        }
    }
    
    // Visualization recommendations
    public static class VisualizationSuggestion {
        public String chartType;
        public String title;
        public String[] xColumns;
        public String[] yColumns;
        public String groupByColumn;
        public int priority; // Higher = more recommended
        
        public VisualizationSuggestion(String type, String title, String[] x, String[] y, int priority) {
            this.chartType = type;
            this.title = title;
            this.xColumns = x;
            this.yColumns = y;
            this.priority = priority;
        }
    }
    
    /**
     * Analyze all columns in the dataset and classify them intelligently
     */
    public static List<ColumnInfo> analyzeColumns(List<String[]> data) {
        if (data.isEmpty()) return new ArrayList<>();
        
        String[] headers = data.get(0);
        List<ColumnInfo> columnInfos = new ArrayList<>();
        
        for (int i = 0; i < headers.length; i++) {
            ColumnInfo info = analyzeColumn(headers[i], data, i);
            columnInfos.add(info);
        }
        
        // Post-process to identify key columns and relationships
        identifyKeyColumns(columnInfos);
        
        return columnInfos;
    }
    
    private static ColumnInfo analyzeColumn(String columnName, List<String[]> data, int columnIndex) {
        ColumnInfo info = new ColumnInfo(columnName);
        
        // Collect sample values (skip header)
        List<String> values = data.stream()
            .skip(1)
            .map(row -> row.length > columnIndex ? row[columnIndex] : "")
            .filter(val -> !val.trim().isEmpty())
            .collect(Collectors.toList());
        
        if (values.isEmpty()) {
            info.type = ColumnType.TEXT_DESCRIPTION;
            return info;
        }
        
        // Calculate uniqueness ratio
        Set<String> uniqueValues = new HashSet<>(values);
        info.uniquenessRatio = (double) uniqueValues.size() / values.size();
        
        // Store sample values and counts
        info.sampleValues = values.stream().limit(10).collect(Collectors.toList());
        info.valueCounts = values.stream()
            .collect(Collectors.groupingBy(v -> v, Collectors.summingInt(v -> 1)));
        
        // Classify column type
        info.type = classifyColumnType(columnName, values, info);
        
        // Additional analysis based on type
        if (info.type == ColumnType.NUMERIC_MEASURE || info.type == ColumnType.NUMERIC_DIMENSION) {
            analyzeNumericColumn(info, values);
        } else if (info.type == ColumnType.DATE_TIME) {
            analyzeDateColumn(info, values);
        }
        
        return info;
    }
    
    private static ColumnType classifyColumnType(String columnName, List<String> values, ColumnInfo info) {
        String lowerName = columnName.toLowerCase();
        
        // Check for date/time patterns
        if (isDateColumn(lowerName, values)) {
            info.isTimeColumn = true;
            return ColumnType.DATE_TIME;
        }
        
        // Check for numeric values
        if (isNumericColumn(values)) {
            // Distinguish between measures and dimensions
            if (isMeasureColumn(lowerName, info.uniquenessRatio)) {
                return ColumnType.NUMERIC_MEASURE;
            } else {
                return ColumnType.NUMERIC_DIMENSION;
            }
        }
        
        // Check for ID columns
        if (isIdColumn(lowerName, info.uniquenessRatio)) {
            return ColumnType.TEXT_ID;
        }
        
        // Check for boolean values
        if (isBooleanColumn(values)) {
            return ColumnType.BOOLEAN;
        }
        
        // Check for long text descriptions
        if (isDescriptionColumn(lowerName, values)) {
            return ColumnType.TEXT_DESCRIPTION;
        }
        
        // Default to categorical
        return ColumnType.CATEGORICAL;
    }
    
    private static boolean isDateColumn(String columnName, List<String> values) {
        // Check column name patterns
        String[] dateKeywords = {"date", "time", "created", "updated", "timestamp", "day", "month", "year"};
        for (String keyword : dateKeywords) {
            if (columnName.contains(keyword)) {
                return true;
            }
        }
        
        // Check value patterns
        String[] datePatterns = {
            "\\d{4}-\\d{2}-\\d{2}",           // 2019-01-01
            "\\d{2}/\\d{2}/\\d{4}",           // 01/01/2019
            "\\d{4}/\\d{2}/\\d{2}",           // 2019/01/01
            "\\d{2}-\\d{2}-\\d{4}",           // 01-01-2019
            "\\d{4}\\d{2}\\d{2}"              // 20190101
        };
        
        for (String pattern : datePatterns) {
            if (values.stream().limit(5).anyMatch(v -> Pattern.matches(pattern, v))) {
                return true;
            }
        }
        
        return false;
    }
    
    private static boolean isNumericColumn(List<String> values) {
        return values.stream()
            .limit(10)
            .allMatch(v -> {
                try {
                    Double.parseDouble(v.replace(",", "").replace("$", ""));
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            });
    }
    
    private static boolean isMeasureColumn(String columnName, double uniquenessRatio) {
        // High uniqueness ratio suggests measures (sales amounts, quantities)
        if (uniquenessRatio > 0.7) return true;
        
        // Check for measure keywords
        String[] measureKeywords = {"sales", "revenue", "amount", "total", "sum", "value", "price", 
                                   "cost", "profit", "quantity", "count", "score", "rating"};
        
        String lowerName = columnName.toLowerCase();
        return Arrays.stream(measureKeywords).anyMatch(lowerName::contains);
    }
    
    private static boolean isIdColumn(String columnName, double uniquenessRatio) {
        // Very high uniqueness suggests ID
        if (uniquenessRatio > 0.95) return true;
        
        String[] idKeywords = {"id", "key", "code", "number", "ref", "identifier"};
        String lowerName = columnName.toLowerCase();
        return Arrays.stream(idKeywords).anyMatch(lowerName::contains);
    }
    
    private static boolean isBooleanColumn(List<String> values) {
        Set<String> uniqueValues = values.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
        
        return uniqueValues.size() <= 3 && 
               uniqueValues.stream().allMatch(v -> 
                   v.matches("true|false|yes|no|y|n|1|0|active|inactive"));
    }
    
    private static boolean isDescriptionColumn(String columnName, List<String> values) {
        String[] descKeywords = {"description", "comment", "note", "detail", "summary"};
        String lowerName = columnName.toLowerCase();
        
        if (Arrays.stream(descKeywords).anyMatch(lowerName::contains)) {
            return true;
        }
        
        // Check average length - descriptions tend to be longer
        double avgLength = values.stream()
            .mapToInt(String::length)
            .average()
            .orElse(0);
        
        return avgLength > 50;
    }
    
    private static void analyzeNumericColumn(ColumnInfo info, List<String> values) {
        List<Double> numericValues = values.stream()
            .map(v -> {
                try {
                    return Double.parseDouble(v.replace(",", "").replace("$", ""));
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            })
            .collect(Collectors.toList());
        
        info.minValue = numericValues.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        info.maxValue = numericValues.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        info.avgValue = numericValues.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }
    
    private static void analyzeDateColumn(ColumnInfo info, List<String> values) {
        // Try to detect date format
        String sample = values.get(0);
        if (Pattern.matches("\\d{4}-\\d{2}-\\d{2}", sample)) {
            info.detectedPattern = "yyyy-MM-dd";
        } else if (Pattern.matches("\\d{2}/\\d{2}/\\d{4}", sample)) {
            info.detectedPattern = "MM/dd/yyyy";
        }
        // Add more patterns as needed
    }
    
    private static void identifyKeyColumns(List<ColumnInfo> columnInfos) {
        // Find the main time column
        columnInfos.stream()
            .filter(col -> col.type == ColumnType.DATE_TIME)
            .findFirst()
            .ifPresent(col -> col.isTimeColumn = true);
        
        // Find key measure columns (highest uniqueness among measures)
        columnInfos.stream()
            .filter(col -> col.type == ColumnType.NUMERIC_MEASURE)
            .max(Comparator.comparing(col -> col.uniquenessRatio))
            .ifPresent(col -> col.isKey = true);
        
        // Find key categorical dimension (most balanced distribution)
        columnInfos.stream()
            .filter(col -> col.type == ColumnType.CATEGORICAL)
            .filter(col -> col.uniquenessRatio > 0.1 && col.uniquenessRatio < 0.8)
            .findFirst()
            .ifPresent(col -> col.isKey = true);
    }
    
    /**
     * Detect the business domain of the dataset
     */
    public static DataDomain detectDataDomain(List<ColumnInfo> columnInfos) {
        Set<String> allColumnNames = columnInfos.stream()
            .map(col -> col.name.toLowerCase())
            .collect(Collectors.toSet());
        
        // Sales/Commerce indicators
        String[] salesKeywords = {"sales", "revenue", "product", "customer", "order", "purchase", "store"};
        if (Arrays.stream(salesKeywords).anyMatch(allColumnNames::contains)) {
            return DataDomain.SALES_COMMERCE;
        }
        
        // Financial indicators  
        String[] financeKeywords = {"budget", "expense", "profit", "cost", "financial", "accounting"};
        if (Arrays.stream(financeKeywords).anyMatch(allColumnNames::contains)) {
            return DataDomain.FINANCIAL;
        }
        
        // HR indicators
        String[] hrKeywords = {"employee", "salary", "department", "manager", "hr", "staff"};
        if (Arrays.stream(hrKeywords).anyMatch(allColumnNames::contains)) {
            return DataDomain.HR_PEOPLE;
        }
        
        return DataDomain.GENERIC;
    }
}