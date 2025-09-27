import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.*;
import javafx.collections.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;

public class SalesDashboard extends Application {

    // Use generic XYChart so we can swap between LineChart and BarChart
    private XYChart<String, Number> chart;
    private ComboBox<String> storeComboBox;
    private ComboBox<String> viewComboBox;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private String filename;
    // Aggregated data: store -> (date -> total weekly sales)
    private Map<String, NavigableMap<LocalDate, Double>> storeDateSales = new HashMap<>();
    private DateTimeFormatter csvDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    // Totals per store->dept
    private Map<String, Map<String, Double>> storeDeptTotals = new HashMap<>();
    // Reference to main UI container so we can safely replace chart nodes
    private VBox mainContainer;

    @Override
    public void start(Stage stage) {
        // Set up the filename from the variable
        filename = System.getProperty("filename", "data/sales_data.csv");

        // Create the filtering controls
        storeComboBox = new ComboBox<>();
        storeComboBox.getItems().addAll("Store 1", "Store 2", "Store 3"); // Example stores

    startDatePicker = new DatePicker();
    endDatePicker = new DatePicker();
    // Set sensible defaults to avoid null pointer exceptions
    startDatePicker.setValue(LocalDate.of(2021, 1, 1));
    endDatePicker.setValue(LocalDate.now());

    // Create the chart (default: time series line chart)
    CategoryAxis xAxis = new CategoryAxis();
    NumberAxis yAxis = new NumberAxis();
    LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
    chart = lineChart;

    chart.setTitle("Sales Over Time");
    xAxis.setLabel("Date");
    yAxis.setLabel("Sales ($)");

        // Add the layout and UI elements
    // View selection (time series / by department / moving average / monthly totals / stacked dept / percent change)
    viewComboBox = new ComboBox<>();
    viewComboBox.getItems().addAll("Time Series", "By Department", "Moving Average (7)", "Monthly Totals", "Dept Stacked", "Percent Change (MoM)");
    viewComboBox.setValue("Time Series");

    VBox vbox = new VBox(10);
    this.mainContainer = vbox;
    HBox filterBox = new HBox(10, new Label("View:"), viewComboBox, new Label("Select Store:"), storeComboBox, new Label("Start Date:"), startDatePicker, new Label("End Date:"), endDatePicker);
    vbox.getChildren().addAll(filterBox, chart);

        // Event listeners for filters
    storeComboBox.setOnAction(e -> updateChartData());
    startDatePicker.setOnAction(e -> updateChartData());
    endDatePicker.setOnAction(e -> updateChartData());
    viewComboBox.setOnAction(e -> updateChartData());

        // Load the data and initialize
        loadDataAndInitializeChart();

        // Set up the scene and show the window
        Scene scene = new Scene(vbox, 800, 600);
        stage.setTitle("Sales Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    private void loadDataAndInitializeChart() {
        // Load the sales data from the provided CSV filename
        try {
            File dataFile = new File(filename);
            // If default data file not found, try the included dataset
            if (!dataFile.exists()) {
                File alt = new File("amazon_sales_dataset.csv");
                if (alt.exists()) {
                    dataFile = alt;
                    System.out.println("Using dataset: " + alt.getName());
                }
            }

            if (dataFile.exists()) {
                // Read CSV data and initialize chart
                loadDataFromCSV(dataFile);
            } else {
                System.out.println("File not found: " + filename);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Simulate loading data from a CSV (can be connected to real data source)
    private void loadDataFromCSV(File file) throws IOException {
        System.out.println("Loading data from: " + file.getName());

        // We'll do a simple CSV parse assuming no embedded commas or quoted fields.
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String header = br.readLine();
            if (header == null) return;

            String[] cols = header.split(",", -1);
            // Map common column names to indices
            int idxDate = -1, idxStore = -1, idxWeekly = -1;
            for (int i = 0; i < cols.length; i++) {
                String c = cols[i].trim();
                if (c.equalsIgnoreCase("Date")) idxDate = i;
                else if (c.equalsIgnoreCase("Store")) idxStore = i;
                else if (c.equalsIgnoreCase("Weekly_Sales") || c.equalsIgnoreCase("Weekly Sales") ) idxWeekly = i;
            }

            if (idxDate == -1 || idxStore == -1 || idxWeekly == -1) {
                System.out.println("CSV missing required columns (Date, Store, Weekly_Sales). Found headers: " + header);
                return;
            }

            String line;
            int rowCount = 0;
            while ((line = br.readLine()) != null) {
                rowCount++;
                String[] parts = parseCsvLine(line);
                if (parts.length <= Math.max(idxDate, Math.max(idxStore, idxWeekly))) continue;
                String dateStr = parts[idxDate].trim();
                String storeStr = parts[idxStore].trim();
                String weeklyStr = parts[idxWeekly].trim();
                // attempt to get department column if present
                String deptStr = "";
                for (int k = 0; k < parts.length; k++) {
                    if (k != idxDate && k != idxStore && k != idxWeekly) {
                        // try to detect Dept column by header name
                    }
                }
                // If header contains Dept index, try to read it
                int idxDept = -1;
                for (int i = 0; i < cols.length; i++) {
                    if (cols[i].trim().equalsIgnoreCase("Dept")) { idxDept = i; break; }
                }
                if (idxDept != -1 && idxDept < parts.length) deptStr = parts[idxDept].trim();

                if (dateStr.isEmpty() || storeStr.isEmpty() || weeklyStr.isEmpty()) continue;

                LocalDate date;
                try {
                    date = LocalDate.parse(dateStr, csvDateFormat);
                } catch (DateTimeParseException e) {
                    // skip malformed dates
                    continue;
                }

                double weekly;
                try {
                    weekly = Double.parseDouble(weeklyStr);
                } catch (NumberFormatException e) {
                    continue;
                }

                NavigableMap<LocalDate, Double> dateMap = storeDateSales.get(storeStr);
                if (dateMap == null) {
                    dateMap = new TreeMap<>();
                    storeDateSales.put(storeStr, dateMap);
                }

                dateMap.put(date, dateMap.getOrDefault(date, 0.0) + weekly);

                // accumulate dept totals
                if (!deptStr.isEmpty()) {
                    Map<String, Double> deptMap = storeDeptTotals.get(storeStr);
                    if (deptMap == null) { deptMap = new HashMap<>(); storeDeptTotals.put(storeStr, deptMap); }
                    deptMap.put(deptStr, deptMap.getOrDefault(deptStr, 0.0) + weekly);
                }
            }

            System.out.println("Loaded rows: " + rowCount + ", stores: " + storeDateSales.size());

            // Populate store combo box from keys and select first
            List<String> stores = new ArrayList<>(storeDateSales.keySet());
            stores.sort((a,b)-> {
                try { return Integer.compare(Integer.parseInt(a), Integer.parseInt(b)); }
                catch (Exception e) { return a.compareTo(b); }
            });
            storeComboBox.getItems().clear();
            storeComboBox.getItems().addAll(stores);
            if (!stores.isEmpty()) {
                storeComboBox.setValue(stores.get(0));
            }

            // Initialize department totals map from parsed rows (already filled during parsing)
            // Initialize chart with selected store
            updateChartData();
        }
    }

    // Parse CSV line honoring quoted fields
    private String[] parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                // handle double quote escape
                if (inQuotes && i+1 < line.length() && line.charAt(i+1) == '"') {
                    cur.append('"'); i++; continue;
                }
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString()); cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    // Aggregation helper: monthly totals per store
    private NavigableMap<String, Double> buildMonthlyTotals(String store) {
        NavigableMap<String, Double> out = new TreeMap<>();
        NavigableMap<LocalDate, Double> map = storeDateSales.get(store);
        if (map == null) return out;
        for (Map.Entry<LocalDate, Double> e : map.entrySet()) {
            String ym = String.format("%04d-%02d", e.getKey().getYear(), e.getKey().getMonthValue());
            out.put(ym, out.getOrDefault(ym, 0.0) + e.getValue());
        }
        return out;
    }

    // Aggregation helper: percent change month-over-month
    private XYChart.Series<String, Number> buildPercentChangeSeries(String store) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        NavigableMap<String, Double> monthly = buildMonthlyTotals(store);
        String prevKey = null; Double prevVal = null;
        for (Map.Entry<String, Double> e : monthly.entrySet()) {
            if (prevVal != null && prevVal != 0) {
                double pct = (e.getValue() - prevVal) / prevVal * 100.0;
                series.getData().add(new XYChart.Data<>(e.getKey(), pct));
            } else {
                series.getData().add(new XYChart.Data<>(e.getKey(), 0.0));
            }
            prevVal = e.getValue();
            prevKey = e.getKey();
        }
        series.setName("MoM %");
        return series;
    }

    // Function to update chart data based on filters
    private void updateChartData() {
        String selectedStore = storeComboBox.getValue();
        LocalDate start = startDatePicker.getValue() == null ? LocalDate.of(2021,1,1) : startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue() == null ? LocalDate.now() : endDatePicker.getValue();

        System.out.println("Fetching data for Store: " + selectedStore + " between " + start + " and " + end);

        // Decide which view to render
        String view = viewComboBox.getValue();
        if (view == null) view = "Time Series";

        if (view.equals("Time Series")) {
            // ensure we have a LineChart in the UI
            if (!(chart instanceof LineChart)) {
                LineChart<String, Number> newLine = new LineChart<>(new CategoryAxis(), new NumberAxis());
                newLine.setTitle("Sales Over Time");
                chart = newLine;
                if (mainContainer != null) mainContainer.getChildren().set(1, newLine);
            }
            LineChart<String,Number> lc = (LineChart<String,Number>) chart;
            lc.getData().clear();
            XYChart.Series<String, Number> s = buildTimeSeriesSeries(selectedStore, start, end);
            lc.getData().add(s);
        } else if (view.equals("By Department")) {
            // ensure we have a BarChart in the UI
            if (!(chart instanceof BarChart)) {
                BarChart<String, Number> bar = new BarChart<>(new CategoryAxis(), new NumberAxis());
                bar.setTitle("Sales by Department");
                chart = bar;
                if (mainContainer != null) mainContainer.getChildren().set(1, bar);
            }
            BarChart<String,Number> bc = (BarChart<String,Number>) chart;
            bc.getData().clear();
            List<XYChart.Series<String, Number>> deptSeries = buildDeptSeries(selectedStore, start, end);
            for (XYChart.Series<String, Number> ds : deptSeries) bc.getData().add(ds);
        } else if (view.startsWith("Moving Average")) {
            // ensure LineChart
            if (!(chart instanceof LineChart)) {
                LineChart<String, Number> newLine = new LineChart<>(new CategoryAxis(), new NumberAxis());
                newLine.setTitle("Moving Average");
                chart = newLine;
                if (mainContainer != null) mainContainer.getChildren().set(1, newLine);
            }
            LineChart<String,Number> lc = (LineChart<String,Number>) chart;
            lc.getData().clear();
            XYChart.Series<String, Number> s = buildMovingAverageSeries(selectedStore, start, end, 7);
            lc.getData().add(s);
        } else if (view.equals("Monthly Totals")) {
            // monthly totals - show as line chart
            if (!(chart instanceof LineChart)) {
                LineChart<String, Number> newLine = new LineChart<>(new CategoryAxis(), new NumberAxis());
                newLine.setTitle("Monthly Totals");
                chart = newLine;
                if (mainContainer != null) mainContainer.getChildren().set(1, newLine);
            }
            LineChart<String,Number> lc = (LineChart<String,Number>) chart;
            lc.getData().clear();
            NavigableMap<String, Double> monthly = buildMonthlyTotals(selectedStore);
            XYChart.Series<String, Number> s = new XYChart.Series<>(); s.setName("Monthly Sales");
            for (Map.Entry<String, Double> e : monthly.entrySet()) s.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
            lc.getData().add(s);
        } else if (view.equals("Dept Stacked")) {
            // stacked dept chart (bar chart with multiple series)
            if (!(chart instanceof BarChart)) {
                BarChart<String, Number> bar = new BarChart<>(new CategoryAxis(), new NumberAxis());
                bar.setTitle("Dept Stacked");
                chart = bar;
                if (mainContainer != null) mainContainer.getChildren().set(1, bar);
            }
            BarChart<String, Number> bc = (BarChart<String, Number>) chart;
            bc.getData().clear();
            // For stacked view we'll add each dept as a series (may be many)
            Map<String, Double> deptTotals = storeDeptTotals.getOrDefault(selectedStore, Collections.emptyMap());
            XYChart.Series<String, Number> s = new XYChart.Series<>(); s.setName("Dept Totals");
            for (Map.Entry<String, Double> e : deptTotals.entrySet()) s.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
            bc.getData().add(s);
        } else if (view.equals("Percent Change (MoM)")) {
            if (!(chart instanceof LineChart)) {
                LineChart<String, Number> newLine = new LineChart<>(new CategoryAxis(), new NumberAxis());
                newLine.setTitle("Percent Change (MoM)");
                chart = newLine;
                if (mainContainer != null) mainContainer.getChildren().set(1, newLine);
            }
            LineChart<String,Number> lc = (LineChart<String,Number>) chart;
            lc.getData().clear();
            XYChart.Series<String, Number> pct = buildPercentChangeSeries(selectedStore);
            lc.getData().add(pct);
        }
    }

    // Helper: build a time series series for store between dates
    private XYChart.Series<String, Number> buildTimeSeriesSeries(String store, LocalDate start, LocalDate end) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Sales");
        NavigableMap<LocalDate, Double> map = storeDateSales.get(store);
        if (map == null) return series;
        for (Map.Entry<LocalDate, Double> e : map.subMap(start, true, end, true).entrySet()) {
            series.getData().add(new XYChart.Data<>(e.getKey().toString(), e.getValue()));
        }
        return series;
    }

    // Helper: build per-department series (single series with dept categories)
    private List<XYChart.Series<String, Number>> buildDeptSeries(String store, LocalDate start, LocalDate end) {
        List<XYChart.Series<String, Number>> out = new ArrayList<>();
        Map<String, Double> deptTotals = storeDeptTotals.get(store);
        if (deptTotals == null) return out;
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Dept Totals");
        for (Map.Entry<String, Double> e : deptTotals.entrySet()) {
            s.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        }
        out.add(s);
        return out;
    }

    // Helper: moving average series
    private XYChart.Series<String, Number> buildMovingAverageSeries(String store, LocalDate start, LocalDate end, int window) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        NavigableMap<LocalDate, Double> map = storeDateSales.get(store);
        if (map == null) return series;
        List<Map.Entry<LocalDate, Double>> entries = new ArrayList<>(map.subMap(start, true, end, true).entrySet());
        for (int i = 0; i < entries.size(); i++) {
            int from = Math.max(0, i - window + 1);
            double sum = 0;
            for (int j = from; j <= i; j++) sum += entries.get(j).getValue();
            double avg = sum / (i - from + 1);
            series.getData().add(new XYChart.Data<>(entries.get(i).getKey().toString(), avg));
        }
        return series;
    }

    public static void main(String[] args) {
        // Pass the filename as a system property
        if (args.length > 0) {
            System.setProperty("filename", args[0]);
        } else {
            System.setProperty("filename", "data/sales_data.csv");
        }

        launch(args);
    }
}
