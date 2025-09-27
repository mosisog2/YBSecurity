import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public class AutoVisualizer {

    // --- Schema Inference ---
    static String inferType(List<String> columnValues) {
        int numericCount = 0, dateCount = 0, total = 0;
        Set<String> unique = new HashSet<>();

        for (String val : columnValues) {
            if (val == null || val.isEmpty()) continue;
            total++;
            unique.add(val);

            try {
                Double.parseDouble(val);
                numericCount++;
                continue;
            } catch (NumberFormatException ignored) {}

            try {
                LocalDate.parse(val);
                dateCount++;
                continue;
            } catch (DateTimeParseException ignored) {}
        }

        if (dateCount > total / 2) return "date";
        if (numericCount > total / 2) return "num";
        if (unique.size() < total / 2) return "cat";
        return "cat";
    }

    // --- CSV Loader ---
    public static List<String[]> loadCSV(String filePath) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {
            String header = br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                rows.add(line.split(","));
            }
        }
        return rows;
    }

    // --- Chart Decision + Rendering ---
    static void autoChart(String filePath) throws IOException {
        List<String[]> rows = loadCSV(filePath);
        if (rows.isEmpty()) {
            System.out.println("No data found.");
            return;
        }

        int colCount = rows.get(0).length;
        List<String> schema = new ArrayList<>();
        for (int col = 0; col < colCount; col++) {
            List<String> values = new ArrayList<>();
            for (String[] row : rows) {
                if (col < row.length) values.add(row[col].trim());
            }
            schema.add(inferType(values));
        }

        System.out.println("Detected schema: " + schema);

        // --- Match Grammar Rules ---
        if (schema.size() == 1 && schema.get(0).equals("num")) {
            List<Double> values = new ArrayList<>();
            for (String[] row : rows) values.add(Double.parseDouble(row[0]));
            Histogram histogram = new Histogram(values, 10);
            CategoryChart chart = new CategoryChartBuilder()
                    .width(800).height(600).title("Auto Histogram").xAxisTitle("Bins").yAxisTitle("Count").build();
            chart.addSeries("Distribution", histogram.getxAxisData(), histogram.getyAxisData());
            new SwingWrapper<>(chart).displayChart();
            System.out.println("Histogram chosen: single numeric column shows distribution.");
        }
        else if (schema.size() >= 2 && schema.contains("date") && schema.contains("num")) {
            int dateIdx = schema.indexOf("date");
            int numIdx = schema.indexOf("num");
            List<Date> x = new ArrayList<>();
            List<Double> y = new ArrayList<>();
            for (String[] row : rows) {
                try {
                    x.add(java.sql.Date.valueOf(LocalDate.parse(row[dateIdx])));
                    y.add(Double.parseDouble(row[numIdx]));
                } catch (Exception ignored) {}
            }
            XYChart chart = new XYChartBuilder().width(800).height(600)
                    .title("Auto Line Chart").xAxisTitle("Date").yAxisTitle("Value").build();
            chart.addSeries("Series", x, y);
            new SwingWrapper<>(chart).displayChart();
            System.out.println("Line chart chosen: numeric values over time.");
        }
        else if (schema.size() >= 2 && schema.contains("cat") && schema.contains("num")) {
            int catIdx = schema.indexOf("cat");
            int numIdx = schema.indexOf("num");
            Map<String, Double> agg = new HashMap<>();
            for (String[] row : rows) {
                String cat = row[catIdx];
                double val = Double.parseDouble(row[numIdx]);
                agg.put(cat, agg.getOrDefault(cat, 0.0) + val);
            }
            CategoryChart chart = new CategoryChartBuilder().width(800).height(600)
                    .title("Auto Bar Chart").xAxisTitle("Category").yAxisTitle("Value").build();
            chart.addSeries("Values", new ArrayList<>(agg.keySet()), new ArrayList<>(agg.values()));
            new SwingWrapper<>(chart).displayChart();
            System.out.println("Bar chart chosen: categories compared by numeric values.");
        }
        else if (schema.size() == 2 && schema.get(0).equals("num") && schema.get(1).equals("num")) {
            List<Double> x = new ArrayList<>(), y = new ArrayList<>();
            for (String[] row : rows) {
                x.add(Double.parseDouble(row[0]));
                y.add(Double.parseDouble(row[1]));
            }
            XYChart chart = new XYChartBuilder().width(800).height(600)
                    .title("Auto Scatter Plot").xAxisTitle("X").yAxisTitle("Y").build();
            chart.addSeries("Points", x, y);
            chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
            new SwingWrapper<>(chart).displayChart();
            System.out.println("Scatter plot chosen: two numeric columns reveal correlations.");
        }
        else {
            System.out.println("No strong chart recommendation, fallback to table.");
        }
    }

    public static void main(String[] args) throws IOException {
        String filePath = "amazon_sales_dataset.csv"; // replace with your dataset
        autoChart(filePath);
    }
}

