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

public class SalesDashboard extends Application {

    private LineChart<String, Number> lineChart;
    private ComboBox<String> storeComboBox;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private String filename;

    @Override
    public void start(Stage stage) {
        // Set up the filename from the variable
        filename = System.getProperty("filename", "data/sales_data.csv");

        // Create the filtering controls
        storeComboBox = new ComboBox<>();
        storeComboBox.getItems().addAll("Store 1", "Store 2", "Store 3"); // Example stores

        startDatePicker = new DatePicker();
        endDatePicker = new DatePicker();

        // Create the chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        lineChart = new LineChart<>(xAxis, yAxis);

        lineChart.setTitle("Sales Over Time");
        xAxis.setLabel("Date");
        yAxis.setLabel("Sales ($)");

        // Add the layout and UI elements
        VBox vbox = new VBox(10);
        HBox filterBox = new HBox(10, new Label("Select Store:"), storeComboBox, new Label("Start Date:"), startDatePicker, new Label("End Date:"), endDatePicker);
        vbox.getChildren().addAll(filterBox, lineChart);

        // Event listeners for filters
        storeComboBox.setOnAction(e -> updateChartData());
        startDatePicker.setOnAction(e -> updateChartData());
        endDatePicker.setOnAction(e -> updateChartData());

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
        // Here, you would load and parse the data from the file (CSV format)
        // For example, use a CSV parser library like OpenCSV or write your own parser.
        // After loading, we would call `updateChartData()` to refresh the chart with actual data.
        System.out.println("Loading data from: " + file.getName());

        // Simulate the chart update with dummy data
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Sales");

        series.getData().add(new XYChart.Data<>("2021-01-01", 5000));
        series.getData().add(new XYChart.Data<>("2021-02-01", 7000));

        lineChart.getData().add(series);
    }

    // Function to update chart data based on filters
    private void updateChartData() {
        // Fetch filtered data based on selected store, date range, etc.
        String selectedStore = storeComboBox.getValue();
        String startDate = startDatePicker.getValue().toString();
        String endDate = endDatePicker.getValue().toString();

        // Simulate fetching and filtering data (replace with actual query logic)
        System.out.println("Fetching data for Store: " + selectedStore + " between " + startDate + " and " + endDate);

        // Clear the current data in the chart
        lineChart.getData().clear();

        // Example of adding new data (this is just an example)
        XYChart.Series<String, Number> newSeries = new XYChart.Series<>();
        newSeries.setName("Sales");

        // Add filtered data points here (this is just an example)
        newSeries.getData().add(new XYChart.Data<>("2021-03-01", 8000));
        newSeries.getData().add(new XYChart.Data<>("2021-04-01", 6500));
        lineChart.getData().add(newSeries);
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
