import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
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
import javafx.scene.Node;

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
    // try multiple date formats
    private DateTimeFormatter[] csvDateFormats = new DateTimeFormatter[]{
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("M/d/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("M/d/yy")
    };
    // Totals per store->dept
    private Map<String, Map<String, Double>> storeDeptTotals = new HashMap<>();
    // Reference to main UI container so we can safely replace chart nodes
    private VBox mainContainer;
    // convenience: keep full store list for filtering
    private List<String> allStores = new ArrayList<>();
    // moving average window
    private int maWindow = 7;

    @Override
    public void start(Stage stage) {
        // Set up the filename from the variable
        filename = System.getProperty("filename", "data/sales_data.csv");

        // Create the filtering controls
        storeComboBox = new ComboBox<>();
        storeComboBox.setEditable(true); // allow typing to filter
        // when user types, we'll filter available stores
        storeComboBox.getEditor().textProperty().addListener((obs, oldV, newV) -> filterStoreItems(newV));

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
        viewComboBox.getItems().addAll("Time Series", "By Department", "Moving Average", "Monthly Totals", "Dept Stacked", "Percent Change (MoM)");
        viewComboBox.setValue("Time Series");

        // Use a BorderPane so the chart expands and controls stay at top
        BorderPane root = new BorderPane();
        this.mainContainer = new VBox();

        HBox topFilters = new HBox(8);
        topFilters.getChildren().addAll(new Label("View:"), viewComboBox, new Label("Store:"), storeComboBox, new Label("Start:"), startDatePicker, new Label("End:"), endDatePicker);

        // Add file chooser and a moving average slider
        Button loadBtn = new Button("Load dataset...");
        Label maLabel = new Label("MA window: 7");
        Slider maSlider = new Slider(1, 30, 7);
        maSlider.setMajorTickUnit(5);
        maSlider.setMinorTickCount(4);
        maSlider.setShowTickMarks(true);
        maSlider.setShowTickLabels(true);
        maSlider.valueProperty().addListener((o, oldV, newV) -> { maLabel.setText("MA window: " + newV.intValue()); maWindow = newV.intValue(); });
        topFilters.getChildren().addAll(loadBtn, maLabel, maSlider);

        VBox controlsWrap = new VBox(6, topFilters);
        controlsWrap.setPadding(new javafx.geometry.Insets(6));

        // Place the chart in center and allow it to grow
        StackPane chartHolder = new StackPane(chart);
        VBox.setVgrow(chartHolder, Priority.ALWAYS);
        root.setTop(controlsWrap);
        root.setCenter(chartHolder);

        // Wire load button to a file chooser and parse in background
        loadBtn.setOnAction(ev -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Open sales CSV");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                final File chosen = f;
                Task<Void> t = new Task<>() { @Override protected Void call() throws Exception { loadDataFromCSV(chosen); return null; } };
                new Thread(t).start();
            }
        });

        this.mainContainer.getChildren().add(root);

        // Event listeners for filters
        storeComboBox.setOnAction(e -> updateChartData());
        startDatePicker.setOnAction(e -> updateChartData());
        endDatePicker.setOnAction(e -> updateChartData());
        viewComboBox.setOnAction(e -> updateChartData());
        maSlider.valueProperty().addListener((o,oldV,newV)-> updateChartData());

        // Load the data and initialize
        loadDataAndInitializeChart();

        // Set up the scene and show the window
        Scene scene = new Scene(this.mainContainer, 1000, 700);
        stage.setTitle("Sales Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    private void loadDataAndInitializeChart() {
        // Load the sales data from the provided CSV filename (async)
        try {
            File dataFile = new File(filename);
            if (!dataFile.exists()) {
                File alt = new File("amazon_sales_dataset.csv");
                if (alt.exists()) { dataFile = alt; System.out.println("Using dataset: " + alt.getName()); }
            }
            if (dataFile.exists()) {
                final File df = dataFile;
                Task<Void> t = new Task<>() { @Override protected Void call() throws Exception { loadDataFromCSV(df); return null; } };
                new Thread(t).start();
            } else {
                System.out.println("File not found: " + filename);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private LocalDate tryParseDate(String s) {
        for (DateTimeFormatter fmt : csvDateFormats) {
            try { return LocalDate.parse(s, fmt); } catch (DateTimeParseException ex) { /* try next */ }
        }
        // As a final fallback, try parsing ISO
        try { return LocalDate.parse(s); } catch (Exception e) { return null; }
    }

    // Simulate loading data from a CSV (can be connected to real data source)
    private void loadDataFromCSV(File file) throws IOException {
        System.out.println("Loading data from: " + file.getAbsolutePath());
        // clear previous data
        storeDateSales.clear();
        storeDeptTotals.clear();
        allStores.clear();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String header = br.readLine();
            if (header == null) return;

            String[] cols = header.split(",", -1);
            int idxDate = -1, idxStore = -1, idxWeekly = -1, idxDept = -1;
            for (int i = 0; i < cols.length; i++) {
                String c = cols[i].trim().replaceAll("[\" ]", "");
                if (c.equalsIgnoreCase("Date")) idxDate = i;
                else if (c.equalsIgnoreCase("Store")) idxStore = i;
                else if (c.equalsIgnoreCase("Weekly_Sales") || c.equalsIgnoreCase("Weekly Sales") || c.equalsIgnoreCase("WeeklySales")) idxWeekly = i;
                else if (c.equalsIgnoreCase("Dept") || c.equalsIgnoreCase("Department")) idxDept = i;
            }

            if (idxDate == -1 || idxStore == -1 || idxWeekly == -1) {
                System.out.println("CSV missing required columns (Date, Store, Weekly_Sales). Found headers: " + header);
                return;
            }

            String line; int rowCount = 0; LocalDate minDate = null, maxDate = null;
            while ((line = br.readLine()) != null) {
                rowCount++;
                String[] parts = parseCsvLine(line);
                if (parts.length <= Math.max(idxDate, Math.max(idxStore, idxWeekly))) continue;
                String dateStr = parts[idxDate].trim();
                String storeStr = parts[idxStore].trim();
                String weeklyStr = parts[idxWeekly].trim();
                String deptStr = "";
                if (idxDept != -1 && idxDept < parts.length) deptStr = parts[idxDept].trim();
                if (dateStr.isEmpty() || storeStr.isEmpty() || weeklyStr.isEmpty()) continue;

                LocalDate date = tryParseDate(dateStr);
                if (date == null) continue;

                double weekly;
                try { weekly = Double.parseDouble(weeklyStr.replaceAll("[$,]", "")); }
                catch (NumberFormatException e) { continue; }

                String storeKey = storeStr;
                NavigableMap<LocalDate, Double> dateMap = storeDateSales.get(storeKey);
                if (dateMap == null) { dateMap = new TreeMap<>(); storeDateSales.put(storeKey, dateMap); }
                dateMap.put(date, dateMap.getOrDefault(date, 0.0) + weekly);

                if (!deptStr.isEmpty()) {
                    Map<String, Double> deptMap = storeDeptTotals.get(storeKey);
                    if (deptMap == null) { deptMap = new HashMap<>(); storeDeptTotals.put(storeKey, deptMap); }
                    deptMap.put(deptStr, deptMap.getOrDefault(deptStr, 0.0) + weekly);
                }
                allStores.add(storeKey);
                if (minDate == null || date.isBefore(minDate)) minDate = date;
                if (maxDate == null || date.isAfter(maxDate)) maxDate = date;
            }
            System.out.println("Loaded " + rowCount + " rows. Date range: " + minDate + " to " + maxDate);

            // Update UI with store list (all unique stores)
            Platform.runLater(() -> {
                storeComboBox.getItems().clear();
                allStores.sort(String::compareToIgnoreCase);
                storeComboBox.getItems().addAll(allStores);
                storeComboBox.getItems().add(0, "All Stores"); // add "All Stores" option
                storeComboBox.setValue("All Stores");
            });

            updateChartData(); // refresh chart with new data
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // CSV parsing helper: handle quoted fields and commas in numbers
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentField = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '\"') {
                inQuotes = !inQuotes; // toggle quote state
            } else if (c == ',' && !inQuotes) {
                result.add(currentField.toString().trim());
                currentField.setLength(0);
            } else {
                currentField.append(c);
            }
        }
        result.add(currentField.toString().trim()); // add last field
        return result.toArray(new String[0]);
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

    // Aggregation helper: monthly totals per store
    private NavigableMap<String, Double> buildMonthlyTotals(String store) {
        NavigableMap<String, Double> out = new TreeMap<>();
        if (store == null) return out;
        if (store.equals("All Stores")) {
            // aggregate across all stores into monthly buckets
            Map<LocalDate, Double> summed = new HashMap<>();
            for (NavigableMap<LocalDate, Double> m : storeDateSales.values()) {
                for (Map.Entry<LocalDate, Double> e : m.entrySet()) summed.put(e.getKey(), summed.getOrDefault(e.getKey(), 0.0) + e.getValue());
            }
            for (Map.Entry<LocalDate, Double> e : summed.entrySet()) {
                String ym = String.format("%04d-%02d", e.getKey().getYear(), e.getKey().getMonthValue());
                out.put(ym, out.getOrDefault(ym, 0.0) + e.getValue());
            }
            return out;
        }
        NavigableMap<LocalDate, Double> map = storeDateSales.get(store);
        if (map == null) return out;
        for (Map.Entry<LocalDate, Double> e : map.entrySet()) {
            String ym = String.format("%04d-%02d", e.getKey().getYear(), e.getKey().getMonthValue());
            out.put(ym, out.getOrDefault(ym, 0.0) + e.getValue());
        }
        return out;
    }
    
    // Function to update chart data based on filters
    private void updateChartData() {
        String selectedStore = storeComboBox.getValue();
        if (selectedStore == null) return;
        LocalDate start = startDatePicker.getValue() == null ? LocalDate.of(2021,1,1) : startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue() == null ? LocalDate.now() : endDatePicker.getValue();

        System.out.println("Fetching data for Store: " + selectedStore + " between " + start + " and " + end);

        // Decide which view to render
        String view = viewComboBox.getValue();
        if (view == null) view = "Time Series";

        if (view.equals("Time Series")) {
            ensureLineChart("Sales Over Time", "Date", "Sales ($)");
            LineChart<String,Number> lc = (LineChart<String,Number>) chart;
            lc.getData().clear();
            if (selectedStore.equals("All Stores")) {
                // aggregate across all stores
                XYChart.Series<String, Number> agg = new XYChart.Series<>(); agg.setName("All Stores");
                TreeMap<LocalDate, Double> summed = new TreeMap<>();
                for (NavigableMap<LocalDate, Double> m : storeDateSales.values()) {
                    for (Map.Entry<LocalDate, Double> e : m.entrySet()) summed.put(e.getKey(), summed.getOrDefault(e.getKey(), 0.0) + e.getValue());
                }
                for (Map.Entry<LocalDate, Double> e : summed.subMap(start, true, end, true).entrySet()) agg.getData().add(new XYChart.Data<>(e.getKey().toString(), e.getValue()));
                lc.getData().add(agg);
            } else {
                XYChart.Series<String, Number> s = buildTimeSeriesSeries(selectedStore, start, end);
                lc.getData().add(s);
            }
        } else if (view.equals("By Department")) {
            ensureBarChart("Sales by Department", "Dept", "Sales");
            BarChart<String,Number> bc = (BarChart<String,Number>) chart;
            bc.getData().clear();
            if (selectedStore.equals("All Stores")) {
                Map<String, Double> totals = new HashMap<>();
                for (Map<String, Double> m : storeDeptTotals.values()) {
                    for (Map.Entry<String, Double> e : m.entrySet()) totals.put(e.getKey(), totals.getOrDefault(e.getKey(), 0.0) + e.getValue());
                }
                XYChart.Series<String,Number> s = new XYChart.Series<>(); s.setName("All Stores");
                for (Map.Entry<String, Double> e : totals.entrySet()) s.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
                bc.getData().add(s);
            } else {
                List<XYChart.Series<String, Number>> deptSeries = buildDeptSeries(selectedStore, start, end);
                for (XYChart.Series<String, Number> ds : deptSeries) bc.getData().add(ds);
            }
        } else if (view.equals("Moving Average")) {
            ensureLineChart("Moving Average", "Date", "Sales ($)");
            LineChart<String,Number> lc = (LineChart<String,Number>) chart;
            lc.getData().clear();
            if (selectedStore.equals("All Stores")) {
                // compute aggregated series then moving average
                TreeMap<LocalDate, Double> summed = new TreeMap<>();
                for (NavigableMap<LocalDate, Double> m : storeDateSales.values()) for (Map.Entry<LocalDate, Double> e : m.entrySet()) summed.put(e.getKey(), summed.getOrDefault(e.getKey(), 0.0) + e.getValue());
                List<Map.Entry<LocalDate, Double>> entries = new ArrayList<>(summed.subMap(start, true, end, true).entrySet());
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                for (int i = 0; i < entries.size(); i++) {
                    int from = Math.max(0, i - maWindow + 1);
                    double sum = 0; for (int j = from; j <= i; j++) sum += entries.get(j).getValue();
                    double avg = sum / (i - from + 1);
                    series.getData().add(new XYChart.Data<>(entries.get(i).getKey().toString(), avg));
                }
                series.setName("All Stores MA("+maWindow+")"); lc.getData().add(series);
            } else {
                XYChart.Series<String, Number> s = buildMovingAverageSeries(selectedStore, start, end, maWindow);
                s.setName("MA("+maWindow+")");
                lc.getData().add(s);
            }
        } else if (view.equals("Monthly Totals")) {
            ensureLineChart("Monthly Totals", "Month", "Sales");
            LineChart<String,Number> lc = (LineChart<String,Number>) chart;
            lc.getData().clear();
            NavigableMap<String, Double> monthly = buildMonthlyTotals(selectedStore);
            XYChart.Series<String, Number> s = new XYChart.Series<>(); s.setName("Monthly Sales");
            for (Map.Entry<String, Double> e : monthly.entrySet()) s.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
            lc.getData().add(s);
        } else if (view.equals("Dept Stacked")) {
            ensureBarChart("Dept Stacked", "Dept", "Sales");
            BarChart<String, Number> bc = (BarChart<String, Number>) chart;
            bc.getData().clear();
            Map<String, Double> deptTotals = storeDeptTotals.getOrDefault(selectedStore, Collections.emptyMap());
            XYChart.Series<String, Number> s = new XYChart.Series<>(); s.setName("Dept Totals");
            for (Map.Entry<String, Double> e : deptTotals.entrySet()) s.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
            bc.getData().add(s);
        } else if (view.equals("Percent Change (MoM)")) {
            ensureLineChart("Percent Change (MoM)", "Month", "%");
            LineChart<String,Number> lc = (LineChart<String,Number>) chart;
            lc.getData().clear();
            XYChart.Series<String, Number> pct = buildPercentChangeSeries(selectedStore);
            lc.getData().add(pct);
        }
    }

    private void ensureLineChart(String title, String xlabel, String ylabel) {
        if (!(chart instanceof LineChart)) {
            LineChart<String, Number> newLine = new LineChart<>(new CategoryAxis(), new NumberAxis());
            newLine.setTitle(title);
            newLine.getXAxis().setLabel(xlabel);
            newLine.getYAxis().setLabel(ylabel);
            chart = newLine;
            // replace center node of root
            if (!mainContainer.getChildren().isEmpty()) {
                Node rootnode = mainContainer.getChildren().get(0);
                if (rootnode instanceof BorderPane) {
                    BorderPane bp = (BorderPane) rootnode;
                    bp.setCenter(newLine);
                }
            }
        }
    }

    private void ensureBarChart(String title, String xlabel, String ylabel) {
        if (!(chart instanceof BarChart)) {
            BarChart<String, Number> bar = new BarChart<>(new CategoryAxis(), new NumberAxis());
            bar.setTitle(title);
            bar.getXAxis().setLabel(xlabel);
            bar.getYAxis().setLabel(ylabel);
            chart = bar;
            if (!mainContainer.getChildren().isEmpty()) {
                Node rootnode = mainContainer.getChildren().get(0);
                if (rootnode instanceof BorderPane) {
                    BorderPane bp = (BorderPane) rootnode;
                    bp.setCenter(bar);
                }
            }
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

    // Simple store filter helper (filters items shown in editable combo)
    private void filterStoreItems(String typed) {
        if (typed == null) return;
        String t = typed.trim().toLowerCase();
        Platform.runLater(() -> {
            storeComboBox.getItems().clear();
            if (t.isEmpty()) storeComboBox.getItems().addAll(allStores);
            else {
                for (String s : allStores) if (s.toLowerCase().contains(t)) storeComboBox.getItems().add(s);
            }
        });
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
