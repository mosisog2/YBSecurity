import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.concurrent.Task;
import javafx.application.Platform;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class SalesDashboard extends Application {
    
    private List<SmartDataRecord> smartData = new ArrayList<>();
    private List<DataIntelligence.ColumnInfo> columnAnalysis = new ArrayList<>();
    private DataIntelligence.DataDomain detectedDomain = DataIntelligence.DataDomain.GENERIC;
    private String[] currentHeaders;
    
    private ComboBox<String> monthFilter; // Month filter for time selection
    private ComboBox<String> groupByFilter; // New grouping control
    private Spinner<Integer> topNSpinner; // New top-N control
    private Button loadFullDataButton; // New load full data button
    private Button analyzeDataButton; // New intelligent analysis button
    private Label domainLabel; // Shows detected data domain
    private LineChart<String, Number> timeSeriesChart;
    private PieChart departmentPieChart;
    private BarChart<String, Number> storeBarChart;
    private TableView<SmartDataRecord> dataTable; // New data table
    private ProgressBar loadingProgress; // New progress indicator
    private Label totalSalesLabel;
    private Label avgSalesLabel;
    private Label departmentCountLabel;
    private Label recordCountLabel; // New record count label
    private Label targetVsActualLabel; // New target comparison label
    
    // Dynamic target calculation - will be calculated from data
    private Map<String, Double> monthlyTargets = new HashMap<>();
    private double overallAverageTarget = 0.0;
    
    // Data class to hold CSV records
    public static class SmartDataRecord {
        private Map<String, String> data;
        private String[] headers;
        
        public SmartDataRecord(String[] fields, String[] headers) {
            this.headers = headers;
            this.data = new HashMap<>();
            for (int i = 0; i < Math.min(fields.length, headers.length); i++) {
                this.data.put(headers[i], fields[i]);
            }
        }
        
        // Dynamic getter methods for any column
        public String get(String columnName) {
            return data.getOrDefault(columnName, "");
        }
        
        public double getNumeric(String columnName) {
            String value = get(columnName);
            try {
                return Double.parseDouble(value.replace(",", "").replace("$", ""));
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        
        public String[] getHeaders() { return headers; }
        public Map<String, String> getAllData() { return data; }
        
        // Legacy getters for backward compatibility
        public String getDate() { return get("Date"); }
        public String getStore() { return get("Store"); }
        public String getDept() { return get("Dept"); }
        public String getYear() { return get("Year"); }
        public String getMonth() { return get("Month"); }
        public double getWeeklySales() { return getNumeric("Weekly_Sales"); }
    }
    
    @Override
    public void start(Stage stage) {
        // Load data first (without updating filter options)
        loadDataFromCSV();
        
        BorderPane root = new BorderPane();
        
        // Create top panel with filters and KPIs
        VBox topPanel = new VBox(10);
        topPanel.setPadding(new Insets(15));
        topPanel.getChildren().addAll(createFilters(), createKPIs());
        
        // Create center panel with tabs for charts and data table
        TabPane tabPane = new TabPane();
        
        // Charts tab
        Tab chartsTab = new Tab("Charts");
        chartsTab.setClosable(false);
        GridPane chartsPanel = new GridPane();
        chartsPanel.setHgap(15);
        chartsPanel.setVgap(15);
        chartsPanel.setPadding(new Insets(15));
        
        // Make grid responsive
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        col1.setHgrow(Priority.ALWAYS);
        
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        col2.setHgrow(Priority.ALWAYS);
        
        RowConstraints row1 = new RowConstraints();
        row1.setVgrow(Priority.ALWAYS);
        
        RowConstraints row2 = new RowConstraints();
        row2.setVgrow(Priority.ALWAYS);
        
        chartsPanel.getColumnConstraints().addAll(col1, col2);
        chartsPanel.getRowConstraints().addAll(row1, row2);
        
        // Initialize charts and table
        createCharts();
        createDataTable();
        
        // Add charts to grid
        chartsPanel.add(timeSeriesChart, 0, 0);
        chartsPanel.add(departmentPieChart, 1, 0);
        chartsPanel.add(storeBarChart, 0, 1, 2, 1);
        chartsTab.setContent(chartsPanel);
        
        // Data table tab
        Tab tableTab = new Tab("Data Table");
        tableTab.setClosable(false);
        VBox tablePanel = new VBox(10);
        tablePanel.setPadding(new Insets(15));
        VBox.setVgrow(dataTable, Priority.ALWAYS); // Make table grow with panel
        
        // Add progress bar for loading indication
        loadingProgress = new ProgressBar();
        loadingProgress.setPrefWidth(200);
        loadingProgress.setVisible(false);
        Label loadingLabel = new Label("Loading data...");
        loadingLabel.setVisible(false);
        
        HBox loadingBox = new HBox(10);
        loadingBox.getChildren().addAll(loadingLabel, loadingProgress);
        
        tablePanel.getChildren().addAll(loadingBox, dataTable);
        tableTab.setContent(tablePanel);
        
        tabPane.getTabs().addAll(chartsTab, tableTab);
        
        root.setTop(topPanel);
        root.setCenter(tabPane);
        
        Scene scene = new Scene(root, 1400, 900);
        scene.widthProperty().addListener((obs, oldVal, newVal) -> adjustLayoutForSize(newVal.doubleValue(), scene.getHeight()));
        scene.heightProperty().addListener((obs, oldVal, newVal) -> adjustLayoutForSize(scene.getWidth(), newVal.doubleValue()));
        
        stage.setTitle("Sales Dashboard - Amazon Sales Dataset");
        stage.setScene(scene);
        stage.setMinWidth(800);  // Minimum window width
        stage.setMinHeight(600); // Minimum window height
        stage.setMaximized(false); // Allow maximizing
        stage.show();
        
        // Initial data load and update charts after UI is created
        updateCharts();
    }
    
    private HBox createFilters() {
        HBox filters = new HBox(15);
        
        // Month filter (dynamic based on data)
        monthFilter = new ComboBox<>();
        monthFilter.setPromptText("Select Month");
        monthFilter.getItems().add("All Months");
        monthFilter.setValue("All Months");
        monthFilter.setOnAction(e -> Platform.runLater(() -> updateCharts()));
        
        // Group By filter for pie chart
        groupByFilter = new ComboBox<>();
        groupByFilter.getItems().addAll("Department", "Store", "Month", "Store Type");
        groupByFilter.setValue("Department");
        groupByFilter.setOnAction(e -> Platform.runLater(() -> updateCharts()));
        
        // Top N spinner for pie chart
        topNSpinner = new Spinner<>(5, 20, 8);
        topNSpinner.setEditable(true);
        topNSpinner.setPrefWidth(80);
        topNSpinner.valueProperty().addListener((obs, oldVal, newVal) -> Platform.runLater(() -> updateCharts()));
        
        // Load full dataset button
        loadFullDataButton = new Button("Load Full Dataset");
        loadFullDataButton.setOnAction(e -> loadFullDataset());
        
        filters.getChildren().addAll(
            new Label("Month:"), monthFilter,
            new Label("Group By:"), groupByFilter,
            new Label("Top N:"), topNSpinner,
            loadFullDataButton
        );
        
        // Now populate the filter options since controls are created
        updateFilterOptions();
        
        return filters;
    }
    
    private HBox createKPIs() {
        HBox kpis = new HBox(20);
        
        totalSalesLabel = new Label("Total Sales: $0");
        totalSalesLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        avgSalesLabel = new Label("Avg Weekly Sales: $0");
        avgSalesLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        departmentCountLabel = new Label("Departments: 0");
        departmentCountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        recordCountLabel = new Label("Records: 0");
        recordCountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Target vs Actual label (similar to dashboard image)
        targetVsActualLabel = new Label("Target vs Actual: -");
        targetVsActualLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Domain detection label
        domainLabel = new Label("Analyzing data...");
        domainLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #0066cc;");
        
        kpis.getChildren().addAll(totalSalesLabel, avgSalesLabel, departmentCountLabel, recordCountLabel, targetVsActualLabel, domainLabel);
        return kpis;
    }
    
    private void createCharts() {
        // Time series chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        timeSeriesChart = new LineChart<>(xAxis, yAxis);
        timeSeriesChart.setTitle("Sales Over Time");
        timeSeriesChart.setPrefSize(600, 400);
        timeSeriesChart.setMaxWidth(Double.MAX_VALUE);
        timeSeriesChart.setMaxHeight(Double.MAX_VALUE);
        
        // Department pie chart
        departmentPieChart = new PieChart();
        departmentPieChart.setTitle("Sales by Department");
        departmentPieChart.setPrefSize(600, 400);
        departmentPieChart.setMaxWidth(Double.MAX_VALUE);
        departmentPieChart.setMaxHeight(Double.MAX_VALUE);
        
        // Store bar chart
        CategoryAxis storeXAxis = new CategoryAxis();
        NumberAxis storeYAxis = new NumberAxis();
        storeBarChart = new BarChart<>(storeXAxis, storeYAxis);
        storeBarChart.setTitle("Sales by Store");
        storeBarChart.setPrefSize(1200, 400);
        storeBarChart.setMaxWidth(Double.MAX_VALUE);
        storeBarChart.setMaxHeight(Double.MAX_VALUE);
    }
    
    private void createDataTable() {
        dataTable = new TableView<>();
        dataTable.setPrefHeight(500);
        dataTable.setMaxWidth(Double.MAX_VALUE);
        dataTable.setMaxHeight(Double.MAX_VALUE);
        
        // Create dynamic columns based on available headers
        if (currentHeaders != null) {
            createDynamicTableColumns();
        } else {
            // Create default columns for backward compatibility
            createDefaultTableColumns();
        }
        
        // Make table sortable and selectable
        dataTable.setSortPolicy(table -> {
            FXCollections.sort(dataTable.getItems(), dataTable.getComparator());
            return true;
        });
        
        // Make table resize columns automatically
        dataTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
    
    private void createDynamicTableColumns() {
        for (String header : currentHeaders) {
            TableColumn<SmartDataRecord, String> column = new TableColumn<>(header);
            column.setCellValueFactory(cellData -> {
                return new javafx.beans.property.SimpleStringProperty(cellData.getValue().get(header));
            });
            column.setPrefWidth(100);
            column.setMaxWidth(Double.MAX_VALUE);
            
            // Format numeric columns
            if (isNumericColumn(header)) {
                column.setCellFactory(col -> new TableCell<SmartDataRecord, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null || item.isEmpty()) {
                            setText(null);
                        } else {
                            try {
                                double value = Double.parseDouble(item.replace(",", "").replace("$", ""));
                                setText(String.format("$%.2f", value));
                            } catch (NumberFormatException e) {
                                setText(item);
                            }
                        }
                    }
                });
            }
            
            dataTable.getColumns().add(column);
        }
    }
    
    private void createDefaultTableColumns() {
        // Legacy table structure for backward compatibility
        TableColumn<SmartDataRecord, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDate()));
        dataTable.getColumns().add(dateCol);
        
        TableColumn<SmartDataRecord, String> storeCol = new TableColumn<>("Store");
        storeCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStore()));
        dataTable.getColumns().add(storeCol);
        
        TableColumn<SmartDataRecord, String> deptCol = new TableColumn<>("Department");
        deptCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDept()));
        dataTable.getColumns().add(deptCol);
    }
    
    private boolean isNumericColumn(String columnName) {
        return columnAnalysis.stream()
            .anyMatch(col -> col.name.equals(columnName) && 
                     (col.type == DataIntelligence.ColumnType.NUMERIC_MEASURE || 
                      col.type == DataIntelligence.ColumnType.NUMERIC_DIMENSION));
    }
    
    private void adjustLayoutForSize(double width, double height) {
        // Adjust chart sizes based on window size
        Platform.runLater(() -> {
            if (timeSeriesChart != null && departmentPieChart != null && storeBarChart != null) {
                // Calculate appropriate sizes based on window dimensions
                double chartWidth = Math.max(300, width / 2.5);
                double chartHeight = Math.max(250, height / 3.0);
                double barChartHeight = Math.max(200, height / 4.5);
                
                // Update chart preferred sizes
                timeSeriesChart.setPrefSize(chartWidth, chartHeight);
                departmentPieChart.setPrefSize(chartWidth, chartHeight);
                storeBarChart.setPrefSize(width * 0.8, barChartHeight);
                
                // Adjust table height for better space usage
                if (dataTable != null) {
                    dataTable.setPrefHeight(Math.max(300, height * 0.6));
                }
                
                // Adjust font sizes for smaller windows
                String fontSize = width < 1000 ? "-fx-font-size: 12px;" : "-fx-font-size: 16px;";
                if (totalSalesLabel != null) {
                    totalSalesLabel.setStyle(fontSize + " -fx-font-weight: bold;");
                    avgSalesLabel.setStyle(fontSize + " -fx-font-weight: bold;");
                    departmentCountLabel.setStyle(fontSize + " -fx-font-weight: bold;");
                    recordCountLabel.setStyle(fontSize + " -fx-font-weight: bold;");
                }
            }
        });
    }
    
    private void loadDataFromCSV() {
        loadDataFromCSV(false); // Load limited dataset by default
    }
    
    private void loadDataFromCSV(boolean loadFull) {
        smartData.clear();
        List<String[]> rawData = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader("nft_sales.csv"))) {
            String headerLine = br.readLine(); // Read header
            if (headerLine != null) {
                currentHeaders = headerLine.split(",");
                rawData.add(currentHeaders);
            }
            
            String line;
            int count = 0;
            int maxRecords = loadFull ? Integer.MAX_VALUE : 10000;
            
            while ((line = br.readLine()) != null && count < maxRecords) {
                String[] fields = line.split(",");
                if (fields.length >= currentHeaders.length) {
                    try {
                        smartData.add(new SmartDataRecord(fields, currentHeaders));
                        rawData.add(fields);
                        count++;
                        
                        // Update progress for large datasets
                        if (loadFull && count % 50000 == 0) {
                            System.out.println("Loaded " + count + " records...");
                        }
                    } catch (Exception e) {
                        // Skip invalid records
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading CSV: " + e.getMessage());
        }
        
        // Perform intelligent analysis
        if (!rawData.isEmpty()) {
            performIntelligentAnalysis(rawData);
        }
        
        String datasetInfo = loadFull ? " (Full Dataset)" : " (Limited: 10K)";
        System.out.println("Loaded " + smartData.size() + " records" + datasetInfo);
        
        // Only update filter options if UI controls exist
        if (monthFilter != null) {
            updateFilterOptions();
        }
    }
    
    private void performIntelligentAnalysis(List<String[]> rawData) {
        // Analyze columns intelligently
        columnAnalysis = DataIntelligence.analyzeColumns(rawData);
        
        // Detect business domain
        detectedDomain = DataIntelligence.detectDataDomain(columnAnalysis);
        
        // Generate intelligent metrics and targets
        Map<String, Double> intelligentTargets = SmartAnalytics.generateIntelligentTargets(columnAnalysis, rawData);
        monthlyTargets.clear();
        monthlyTargets.putAll(intelligentTargets);
        
        // Calculate new overall average target
        overallAverageTarget = intelligentTargets.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        // Update UI with analysis results
        Platform.runLater(() -> {
            if (domainLabel != null) {
                domainLabel.setText("Detected Domain: " + detectedDomain.toString().replace("_", " "));
            }
            
            // Update grouping options based on intelligent analysis
            updateIntelligentGroupingOptions();
        });
        
        System.out.println("✓ Intelligent Analysis Complete:");
        System.out.println("  - Domain: " + detectedDomain);
        System.out.println("  - Columns analyzed: " + columnAnalysis.size());
        System.out.println("  - Key measures: " + columnAnalysis.stream()
            .filter(col -> col.type == DataIntelligence.ColumnType.NUMERIC_MEASURE)
            .count());
        System.out.println("  - Key dimensions: " + columnAnalysis.stream()
            .filter(col -> col.type == DataIntelligence.ColumnType.CATEGORICAL)
            .count());
    }
    
    private void loadFullDataset() {
        loadFullDataButton.setText("Loading...");
        loadFullDataButton.setDisable(true);
        
        // Show progress indicator
        Platform.runLater(() -> {
            loadingProgress.setVisible(true);
            loadingProgress.setProgress(-1); // Indeterminate progress
        });
        
        // Run in background thread to avoid UI freezing
        Task<Void> loadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                loadDataFromCSV(true);
                return null;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    loadFullDataButton.setText("Full Dataset Loaded (" + smartData.size() + " records)");
                    loadFullDataButton.setDisable(false);
                    loadingProgress.setVisible(false);
                    updateCharts();
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    loadFullDataButton.setText("Load Full Dataset");
                    loadFullDataButton.setDisable(false);
                    loadingProgress.setVisible(false);
                    System.err.println("Failed to load full dataset: " + getException().getMessage());
                });
            }
        };
        
        new Thread(loadTask).start();
    }
    
    private void updateFilterOptions() {
        // Calculate dynamic targets based on actual data
        calculateDynamicTargets();
        
        // Find the main date column intelligently
        Optional<DataIntelligence.ColumnInfo> dateColumn = columnAnalysis.stream()
            .filter(col -> col.type == DataIntelligence.ColumnType.DATE_TIME)
            .findFirst();
        
        if (dateColumn.isPresent() && monthFilter != null) {
            String dateColName = dateColumn.get().name;
            
            // Update month filter (dynamic based on data)
            Set<String> months = smartData.stream()
                .map(r -> r.get(dateColName))
                .filter(date -> !date.isEmpty())
                .map(date -> date.length() >= 7 ? date.substring(0, 7) : date)
                .collect(Collectors.toSet());
            
            String selectedMonth = monthFilter.getValue();
            monthFilter.getItems().clear();
            monthFilter.getItems().addAll(months.stream().sorted().collect(Collectors.toList()));
            monthFilter.getItems().add(0, "All Months");
            monthFilter.setValue(selectedMonth != null ? selectedMonth : "All Months");
        }
    }
    
    private void calculateDynamicTargets() {
        try {
            // Find the main numeric column for target calculation
            String mainNumericColumn = null;
            String dateColumn = null;
            
            // Look for common numeric columns
            for (String header : currentHeaders) {
                if (header.toLowerCase().contains("sales") || 
                    header.toLowerCase().contains("revenue") || 
                    header.toLowerCase().contains("amount")) {
                    mainNumericColumn = header;
                    break;
                }
            }
            
            // Look for date columns
            for (String header : currentHeaders) {
                if (header.toLowerCase().contains("date") || 
                    header.toLowerCase().contains("time")) {
                    dateColumn = header;
                    break;
                }
            }
            
            if (mainNumericColumn == null) {
                // If no sales column found, use first numeric column
                for (String header : currentHeaders) {
                    try {
                        double testValue = smartData.get(0).getNumeric(header);
                        mainNumericColumn = header;
                        break;
                    } catch (Exception e) {
                        // Not a numeric column, continue
                    }
                }
            }
            
            if (mainNumericColumn != null) {
                // Calculate target as average of the main numeric column
                final String finalNumericColumn = mainNumericColumn;
                double totalValue = smartData.stream()
                    .mapToDouble(r -> r.getNumeric(finalNumericColumn))
                    .sum();
                double avgValue = totalValue / smartData.size();
                
                // Set target as 110% of average (stretch goal)
                overallAverageTarget = avgValue * 1.1;
                System.out.println("Dynamic target calculated: $" + String.format("%.2f", overallAverageTarget));
            } else {
                // Fallback target if no numeric column found
                overallAverageTarget = 1000000.0;
                System.out.println("Using fallback target: $" + String.format("%.2f", overallAverageTarget));
            }
        } catch (Exception e) {
            // Fallback in case of any error
            overallAverageTarget = 1000000.0;
            System.out.println("Error calculating target, using fallback: " + e.getMessage());
        }
    }
    
    private void updateIntelligentGroupingOptions() {
        if (groupByFilter == null) return;
        
        // Get smart grouping suggestions
        List<String> suggestions = SmartAnalytics.suggestGroupingStrategies(columnAnalysis);
        
        String currentValue = groupByFilter.getValue();
        groupByFilter.getItems().clear();
        
        // Add intelligent suggestions
        groupByFilter.getItems().addAll(suggestions);
        
        // Add legacy options for backward compatibility
        groupByFilter.getItems().addAll("Department", "Store", "Month", "Store Type");
        
        // Remove duplicates
        Set<String> uniqueItems = new LinkedHashSet<>(groupByFilter.getItems());
        groupByFilter.getItems().clear();
        groupByFilter.getItems().addAll(uniqueItems);
        
        // Restore selection or set intelligent default
        if (groupByFilter.getItems().contains(currentValue)) {
            groupByFilter.setValue(currentValue);
        } else if (!suggestions.isEmpty()) {
            groupByFilter.setValue(suggestions.get(0)); // Use most recommended
        } else {
            groupByFilter.setValue("Department");
        }
    }
    
    private void updateCharts() {
        String selectedMonth = monthFilter != null ? monthFilter.getValue() : "All Months";
        
        // Find the main date column intelligently
        Optional<DataIntelligence.ColumnInfo> dateColumn = columnAnalysis.stream()
            .filter(col -> col.type == DataIntelligence.ColumnType.DATE_TIME)
            .findFirst();
        
        // Filter data based on month selection only
        List<SmartDataRecord> filteredData;
        if (dateColumn.isPresent()) {
            String dateColName = dateColumn.get().name;
            filteredData = smartData.stream()
                .filter(r -> selectedMonth.equals("All Months") || r.get(dateColName).startsWith(selectedMonth))
                .collect(Collectors.toList());
        } else {
            filteredData = new ArrayList<>(smartData);
        }
        
        // Update KPIs
        updateKPIs(filteredData);
        
        // Update time series chart
        updateTimeSeriesChart(filteredData);
        
        // Update pie chart (with improved handling for large datasets)
        updateDepartmentPieChart(filteredData);
        
        // Update bar chart
        updateStoreBarChart(filteredData);
        
        // Update data table
        updateDataTable(filteredData);
    }
    
    private void updateKPIs(List<SmartDataRecord> data) {
        double totalSales = data.stream().mapToDouble(r -> r.getNumeric("Weekly_Sales")).sum();
        double avgSales = data.isEmpty() ? 0 : totalSales / data.size();
        
        // Count unique items based on current grouping
        String groupBy = groupByFilter != null ? groupByFilter.getValue() : "Department";
        long uniqueCount = 0;
        
        switch (groupBy) {
            case "Department":
                uniqueCount = data.stream().map(r -> r.get("Dept")).distinct().count();
                departmentCountLabel.setText("Departments: " + uniqueCount);
                break;
            case "Store":
                uniqueCount = data.stream().map(r -> r.get("Store")).distinct().count();
                departmentCountLabel.setText("Stores: " + uniqueCount);
                break;
            case "Month":
                uniqueCount = data.stream().map(r -> r.get("Date").length() >= 7 ? r.get("Date").substring(0, 7) : r.get("Date")).distinct().count();
                departmentCountLabel.setText("Months: " + uniqueCount);
                break;
            case "Store Type":
                uniqueCount = data.stream().map(r -> {
                    try {
                        int storeNum = Integer.parseInt(r.get("Store"));
                        return storeNum % 3 == 0 ? "A" : (storeNum % 2 == 0 ? "B" : "C");
                    } catch (NumberFormatException e) {
                        return "Unknown";
                    }
                }).distinct().count();
                departmentCountLabel.setText("Store Types: " + uniqueCount);
                break;
        }
        
        totalSalesLabel.setText(String.format("Total Sales: $%.2f", totalSales));
        avgSalesLabel.setText(String.format("Avg Weekly Sales: $%.2f", avgSales));
        recordCountLabel.setText("Records: " + data.size() + " / " + smartData.size());
        
        // Calculate target vs actual (using dynamic average-based targets)
        String selectedMonth = monthFilter != null ? monthFilter.getValue() : "All Months";
        if (!selectedMonth.equals("All Months") && monthlyTargets.containsKey(selectedMonth)) {
            double target = monthlyTargets.get(selectedMonth);
            double actual = totalSales;
            double gap = actual - target;
            double gapPercentage = target > 0 ? (gap / target) * 100 : 0;
            
            String gapText = gap >= 0 ? "+" : "";
            String color = gap >= 0 ? "-fx-text-fill: green;" : "-fx-text-fill: red;";
            
            targetVsActualLabel.setText(String.format("Target: $%.0f | Actual: $%.0f | Gap: %s%.0f (%.1f%%)", 
                target, actual, gapText, gap, gapPercentage));
            targetVsActualLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; " + color);
        } else {
            // Show overall performance when "All Months" is selected
            double totalTarget = overallAverageTarget * monthlyTargets.size();
            double gap = totalSales - totalTarget;
            double gapPercentage = totalTarget > 0 ? (gap / totalTarget) * 100 : 0;
            
            String gapText = gap >= 0 ? "+" : "";
            String color = gap >= 0 ? "-fx-text-fill: green;" : "-fx-text-fill: red;";
            
            targetVsActualLabel.setText(String.format("Avg Target: $%.0f | Actual: $%.0f | Variance: %s%.0f (%.1f%%)", 
                overallAverageTarget, totalSales / Math.max(1, monthlyTargets.size()), gapText, gap / Math.max(1, monthlyTargets.size()), gapPercentage));
            targetVsActualLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; " + color);
        }
    }
    
    private void updateTimeSeriesChart(List<SmartDataRecord> data) {
        // Ensure this runs on JavaFX Application Thread
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateTimeSeriesChart(data));
            return;
        }
        
        // Group by month and sum sales
        Map<String, Double> monthlySales = data.stream()
            .collect(Collectors.groupingBy(
                r -> r.get("Date").length() >= 7 ? r.get("Date").substring(0, 7) : r.get("Date"), // Use YYYY-MM format
                Collectors.summingDouble(r -> r.getNumeric("Weekly_Sales"))
            ));
        
        // Actual sales series
        XYChart.Series<String, Number> actualSeries = new XYChart.Series<>();
        actualSeries.setName("Actual Sales");
        
        // Target sales series (like in the dashboard image)
        XYChart.Series<String, Number> targetSeries = new XYChart.Series<>();
        targetSeries.setName("Target Sales");
        
        // Get all months and sort them
        Set<String> allMonths = new TreeSet<>();
        allMonths.addAll(monthlySales.keySet());
        allMonths.addAll(monthlyTargets.keySet());
        
        for (String month : allMonths) {
            // Add actual sales data
            double actualSales = monthlySales.getOrDefault(month, 0.0);
            if (actualSales > 0) {
                actualSeries.getData().add(new XYChart.Data<>(month, actualSales));
            }
            
            // Add target data
            double targetSales = monthlyTargets.getOrDefault(month, 0.0);
            if (targetSales > 0) {
                targetSeries.getData().add(new XYChart.Data<>(month, targetSales));
            }
        }
        
        timeSeriesChart.getData().clear();
        timeSeriesChart.getData().addAll(actualSeries, targetSeries);
        timeSeriesChart.setTitle("Sales Performance: Target vs Actual");
    }
    
    private void updateDepartmentPieChart(List<SmartDataRecord> data) {
        // Ensure this runs on JavaFX Application Thread
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateDepartmentPieChart(data));
            return;
        }
        
        String groupBy = groupByFilter != null ? groupByFilter.getValue() : "Department";
        int topN = topNSpinner != null ? topNSpinner.getValue() : 8;
        
        // Group data based on selection
        Map<String, Double> groupedSales = new LinkedHashMap<>();
        
        switch (groupBy) {
            case "Department":
                groupedSales = data.stream().collect(Collectors.groupingBy(
                    r -> "Dept " + r.get("Dept"),
                    Collectors.summingDouble(r -> r.getNumeric("Weekly_Sales"))
                ));
                departmentPieChart.setTitle("Sales by Department (Top " + topN + ")");
                break;
            case "Store":
                groupedSales = data.stream().collect(Collectors.groupingBy(
                    r -> "Store " + r.get("Store"),
                    Collectors.summingDouble(r -> r.getNumeric("Weekly_Sales"))
                ));
                departmentPieChart.setTitle("Sales by Store (Top " + topN + ")");
                break;
            case "Month":
                groupedSales = data.stream().collect(Collectors.groupingBy(
                    r -> r.get("Date").length() >= 7 ? r.get("Date").substring(0, 7) : r.get("Date"),
                    Collectors.summingDouble(r -> r.getNumeric("Weekly_Sales"))
                ));
                departmentPieChart.setTitle("Sales by Month (Top " + topN + ")");
                break;
            case "Store Type":
                groupedSales = data.stream().collect(Collectors.groupingBy(
                    r -> {
                        try {
                            int storeNum = Integer.parseInt(r.get("Store"));
                            return "Type " + (storeNum % 3 == 0 ? "A" : (storeNum % 2 == 0 ? "B" : "C"));
                        } catch (NumberFormatException e) {
                            return "Type Unknown";
                        }
                    },
                    Collectors.summingDouble(r -> r.getNumeric("Weekly_Sales"))
                ));
                departmentPieChart.setTitle("Sales by Store Type (Top " + topN + ")");
                break;
        }
        
        // Calculate total for percentage calculations
        double totalSales = groupedSales.values().stream().mapToDouble(Double::doubleValue).sum();
        
        // Sort and get top N entries
        List<Map.Entry<String, Double>> sortedEntries = groupedSales.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .collect(Collectors.toList());
        
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        
        // Add top N entries
        double topNTotal = 0;
        for (int i = 0; i < Math.min(topN, sortedEntries.size()); i++) {
            Map.Entry<String, Double> entry = sortedEntries.get(i);
            double percentage = (entry.getValue() / totalSales) * 100;
            String label = entry.getKey() + String.format(" (%.1f%%)", percentage);
            pieChartData.add(new PieChart.Data(label, entry.getValue()));
            topNTotal += entry.getValue();
        }
        
        // Add "Other" category if there are more entries
        if (sortedEntries.size() > topN) {
            double otherTotal = totalSales - topNTotal;
            double otherPercentage = (otherTotal / totalSales) * 100;
            String otherLabel = String.format("Other (%d items, %.1f%%)", 
                sortedEntries.size() - topN, otherPercentage);
            pieChartData.add(new PieChart.Data(otherLabel, otherTotal));
        }
        
        departmentPieChart.setData(pieChartData);
        
        // Add tooltips with detailed information
        departmentPieChart.getData().forEach(pieData -> {
            Tooltip tooltip = new Tooltip(String.format("%s\nValue: $%.2f", 
                pieData.getName(), pieData.getPieValue()));
            Tooltip.install(pieData.getNode(), tooltip);
        });
    }
    
    private void updateStoreBarChart(List<SmartDataRecord> data) {
        // Ensure this runs on JavaFX Application Thread
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateStoreBarChart(data));
            return;
        }
        
        // Group by store and sum sales
        Map<String, Double> storeSales = data.stream()
            .collect(Collectors.groupingBy(
                r -> "Store " + r.get("Store"),
                Collectors.summingDouble(r -> r.getNumeric("Weekly_Sales"))
            ));
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Store Sales");
        
        storeSales.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(15) // Show top 15 stores
            .forEach(entry -> series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue())));
        
        storeBarChart.getData().clear();
        storeBarChart.getData().add(series);
    }
    
    private void updateDataTable(List<SmartDataRecord> data) {
        // Use background thread to update table for large datasets
        if (data.size() > 50000) {
            // Show progress for very large datasets
            Platform.runLater(() -> {
                loadingProgress.setVisible(true);
                loadingProgress.setProgress(0);
            });
            
            Task<ObservableList<SmartDataRecord>> task = new Task<ObservableList<SmartDataRecord>>() {
                @Override
                protected ObservableList<SmartDataRecord> call() throws Exception {
                    ObservableList<SmartDataRecord> tableData = FXCollections.observableArrayList();
                    
                    // Process data in chunks to show progress
                    int totalSize = data.size();
                    int chunkSize = 10000;
                    
                    for (int i = 0; i < totalSize; i += chunkSize) {
                        int end = Math.min(i + chunkSize, totalSize);
                        tableData.addAll(data.subList(i, end));
                        
                        // Update progress
                        final double progress = (double) end / totalSize;
                        Platform.runLater(() -> loadingProgress.setProgress(progress));
                        
                        // Small delay to prevent UI freezing
                        Thread.sleep(10);
                    }
                    
                    return tableData;
                }
                
                @Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        dataTable.setItems(getValue());
                        loadingProgress.setVisible(false);
                    });
                }
                
                @Override
                protected void failed() {
                    Platform.runLater(() -> {
                        loadingProgress.setVisible(false);
                        System.err.println("Failed to update data table: " + getException().getMessage());
                    });
                }
            };
            
            new Thread(task).start();
        } else {
            // For smaller datasets, update directly
            ObservableList<SmartDataRecord> tableData = FXCollections.observableArrayList(data);
            dataTable.setItems(tableData);
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
