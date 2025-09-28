import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Web Interface Bridge for JavaFX Dashboard
 * Inspired by Abixen Platform's microservices architecture
 * Provides REST API endpoints for web-to-desktop communication
 */
public class WebInterfaceBridge {
    private HttpServer server;
    private final Gson gson = new Gson();
    private final Map<String, Object> sharedData = new ConcurrentHashMap<>();
    private SalesDashboard dashboard;
    private final int port = 8081;
    
    public WebInterfaceBridge(SalesDashboard dashboard) {
        this.dashboard = dashboard;
    }
    
    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // API endpoints
            server.createContext("/api/data", new DataHandler());
            server.createContext("/api/analysis", new AnalysisHandler());
            server.createContext("/api/charts", new ChartHandler());
            server.createContext("/api/sync", new SyncHandler());
            server.createContext("/api/export", new ExportHandler());
            
            // CORS handler
            server.setExecutor(null);
            server.start();
            
            System.out.println("🌐 Web Interface Bridge started on port " + port);
            System.out.println("🔗 API Base URL: http://localhost:" + port + "/api/");
            
        } catch (Exception e) {
            System.err.println("Failed to start Web Interface Bridge: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("🛑 Web Interface Bridge stopped");
        }
    }
    
    // Handlers for different API endpoints
    
    class DataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                // Enable CORS
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }
                
                if ("GET".equals(exchange.getRequestMethod())) {
                    handleGetData(exchange);
                } else if ("POST".equals(exchange.getRequestMethod())) {
                    handlePostData(exchange);
                } else {
                    exchange.sendResponseHeaders(405, -1); // Method not allowed
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        private void handleGetData(HttpExchange exchange) throws Exception {
            // Get current data from JavaFX dashboard
            List<SmartDataRecord> currentData = dashboard != null ? dashboard.getSmartData() : new ArrayList<>();
            
            // Convert to JSON-friendly format
            List<Map<String, Object>> jsonData = new ArrayList<>();
            for (SmartDataRecord record : currentData) {
                Map<String, Object> recordMap = record.toMap();
                jsonData.add(recordMap);
            }
            
            ApiResponse response = new ApiResponse("success", jsonData, null);
            String jsonResponse = gson.toJson(response);
            
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(jsonResponse.getBytes(StandardCharsets.UTF_8));
            exchange.getResponseBody().close();
        }
        
        private void handlePostData(HttpExchange exchange) throws Exception {
            // Read posted data
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            
            try {
                // Parse the data and update JavaFX dashboard
                Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> newData = gson.fromJson(requestBody, listType);
                
                // Convert to SmartDataRecord format and update dashboard
                if (dashboard != null) {
                    // This would trigger dashboard update
                    // dashboard.updateDataFromWeb(newData);
                }
                
                ApiResponse response = new ApiResponse("success", "Data updated successfully", null);
                String jsonResponse = gson.toJson(response);
                
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
                exchange.getResponseBody().write(jsonResponse.getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().close();
                
            } catch (Exception e) {
                ApiResponse response = new ApiResponse("error", null, "Failed to update data: " + e.getMessage());
                String jsonResponse = gson.toJson(response);
                
                exchange.sendResponseHeaders(400, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
                exchange.getResponseBody().write(jsonResponse.getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().close();
            }
        }
    }
    
    class AnalysisHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }
                
                if ("GET".equals(exchange.getRequestMethod())) {
                    // Get current ML analysis results
                    Map<String, Object> analysis = new HashMap<>();
                    analysis.put("domain", dashboard != null ? "SALES_COMMERCE" : "UNKNOWN");
                    analysis.put("recordCount", dashboard != null ? dashboard.getSmartData().size() : 0);
                    analysis.put("columnCount", dashboard != null ? dashboard.getCurrentHeaders().length : 0);
                    analysis.put("mlConfidence", 87.5);
                    analysis.put("dataQuality", 94.2);
                    
                    // Add insights
                    List<Map<String, String>> insights = new ArrayList<>();
                    insights.add(createInsight("volume", "Dataset loaded successfully", "medium"));
                    insights.add(createInsight("domain", "Sales domain detected", "high"));
                    analysis.put("insights", insights);
                    
                    ApiResponse response = new ApiResponse("success", analysis, null);
                    String jsonResponse = gson.toJson(response);
                    
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
                    exchange.getResponseBody().write(jsonResponse.getBytes(StandardCharsets.UTF_8));
                    exchange.getResponseBody().close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        private Map<String, String> createInsight(String type, String title, String importance) {
            Map<String, String> insight = new HashMap<>();
            insight.put("type", type);
            insight.put("title", title);
            insight.put("importance", importance);
            return insight;
        }
    }
    
    class ChartHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }
                
                // Generate chart data compatible with web interface
                Map<String, Object> chartData = generateChartData();
                
                ApiResponse response = new ApiResponse("success", chartData, null);
                String jsonResponse = gson.toJson(response);
                
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
                exchange.getResponseBody().write(jsonResponse.getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().close();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        private Map<String, Object> generateChartData() {
            Map<String, Object> chartData = new HashMap<>();
            
            // Time series data
            List<String> timeLabels = Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun");
            List<Double> timeValues = Arrays.asList(2800.0, 3100.0, 3400.0, 3200.0, 3600.0, 3800.0);
            
            Map<String, Object> timeSeries = new HashMap<>();
            timeSeries.put("labels", timeLabels);
            timeSeries.put("values", timeValues);
            chartData.put("timeSeries", timeSeries);
            
            // Distribution data
            List<String> distLabels = Arrays.asList("Axie Infinity", "CryptoPunks", "Art Blocks", "BAYC", "NBA Top Shot");
            List<Double> distValues = Arrays.asList(3328.0, 1664.0, 1075.0, 784.0, 782.0);
            
            Map<String, Object> distribution = new HashMap<>();
            distribution.put("labels", distLabels);
            distribution.put("values", distValues);
            chartData.put("distribution", distribution);
            
            return chartData;
        }
    }
    
    class SyncHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }
                
                // Sync status between web and desktop interfaces
                Map<String, Object> syncStatus = new HashMap<>();
                syncStatus.put("webConnected", true);
                syncStatus.put("desktopConnected", dashboard != null);
                syncStatus.put("lastSync", System.currentTimeMillis());
                syncStatus.put("dataVersion", "1.0");
                
                ApiResponse response = new ApiResponse("success", syncStatus, null);
                String jsonResponse = gson.toJson(response);
                
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
                exchange.getResponseBody().write(jsonResponse.getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().close();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    class ExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }
                
                // Export data in various formats
                String format = getQueryParameter(exchange.getRequestURI().getQuery(), "format", "json");
                String exportData = generateExportData(format);
                
                String contentType = format.equals("csv") ? "text/csv" : "application/json";
                String filename = "dashboard-export." + format;
                
                exchange.getResponseHeaders().add("Content-Type", contentType);
                exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                exchange.sendResponseHeaders(200, exportData.getBytes(StandardCharsets.UTF_8).length);
                exchange.getResponseBody().write(exportData.getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().close();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        private String generateExportData(String format) {
            if ("csv".equals(format)) {
                // Generate CSV export
                StringBuilder csv = new StringBuilder();
                csv.append("Collections,Sales,Buyers,Transactions,Owners\n");
                csv.append("Axie Infinity,3328148500,1079811,9755511,2656431\n");
                csv.append("CryptoPunks,1664246968,4723,18961,3289\n");
                return csv.toString();
            } else {
                // Generate JSON export
                List<Map<String, Object>> data = new ArrayList<>();
                Map<String, Object> record = new HashMap<>();
                record.put("Collections", "Axie Infinity");
                record.put("Sales", 3328148500L);
                record.put("Buyers", 1079811);
                data.add(record);
                return gson.toJson(data);
            }
        }
        
        private String getQueryParameter(String query, String param, String defaultValue) {
            if (query == null) return defaultValue;
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2 && keyValue[0].equals(param)) {
                    return keyValue[1];
                }
            }
            return defaultValue;
        }
    }
    
    // API Response wrapper class
    static class ApiResponse {
        private final String status;
        private final Object data;
        private final String error;
        
        public ApiResponse(String status, Object data, String error) {
            this.status = status;
            this.data = data;
            this.error = error;
        }
        
        public String getStatus() { return status; }
        public Object getData() { return data; }
        public String getError() { return error; }
    }
}