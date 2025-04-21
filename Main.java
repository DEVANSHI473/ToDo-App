// Updated Main.java with fixes for task description handling
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Main {
    private static List<Task> tasks = new ArrayList<>();
    private static long taskIdCounter = 1;
    private static final String DATA_FILE = "tasks.txt";

    public static void main(String[] args) throws IOException {
        // Load existing tasks from file if available
        loadTasks();

        // Create HTTP server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // Set up handlers for different endpoints
        server.createContext("/", new StaticFileHandler("index.html"));
        server.createContext("/styles.css", new StaticFileHandler("styles.css"));
        server.createContext("/script.js", new StaticFileHandler("script.js"));
        
        // API endpoints
        server.createContext("/api/tasks", new TasksHandler());
        server.createContext("/api/tasks/add", new AddTaskHandler());
        server.createContext("/api/tasks/toggle", new ToggleTaskHandler());
        server.createContext("/api/tasks/delete", new DeleteTaskHandler());
        
        // Set up thread pool
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        
        System.out.println("Server started on port 8080. Open http://localhost:8080 in your browser.");
    }
    
    // Convert task to JSON string
    private static String taskToJson(Task task) {
        // Make sure description isn't null
        String description = task.getDescription();
        if (description == null) description = "";
        
        // Escape special characters for JSON
        description = description.replace("\\", "\\\\")
                                 .replace("\"", "\\\"")
                                 .replace("\n", "\\n")
                                 .replace("\r", "\\r")
                                 .replace("\t", "\\t");
        
        return String.format(
            "{\"id\":%d,\"description\":\"%s\",\"completed\":%b}",
            task.getId(), 
            description, 
            task.isCompleted()
        );
    }
    
    // Convert list of tasks to JSON array string
    private static String tasksToJson(List<Task> tasks) {
        return "[" + 
            tasks.stream()
                .map(Main::taskToJson)
                .collect(Collectors.joining(",")) + 
            "]";
    }
    
    // Extract task ID from simple JSON string
    private static long extractTaskId(String json) {
        // Simple JSON parsing for: {"id":123,...}
        int idStart = json.indexOf("\"id\":");
        if (idStart >= 0) {
            idStart += 5; // Move past "id":
            int idEnd = json.indexOf(",", idStart);
            if (idEnd < 0) idEnd = json.indexOf("}", idStart);
            if (idEnd >= 0) {
                String idStr = json.substring(idStart, idEnd).trim();
                try {
                    return Long.parseLong(idStr);
                } catch (NumberFormatException e) {
                    System.err.println("Failed to parse ID: " + idStr);
                }
            }
        }
        return -1;
    }
    
    // Extract task description from simple JSON string
    private static String extractTaskDescription(String json) {
        System.out.println("Parsing JSON: " + json); // Debug output
        
        // Simple JSON parsing for: {"description":"text",...}
        int descStart = json.indexOf("\"description\":\"");
        if (descStart >= 0) {
            descStart += 15; // Move past "description":"
            int descEnd = -1;
            boolean inEscape = false;
            
            // Find the closing quote, handling escaped characters
            for (int i = descStart; i < json.length(); i++) {
                char c = json.charAt(i);
                if (inEscape) {
                    inEscape = false;
                } else if (c == '\\') {
                    inEscape = true;
                } else if (c == '"') {
                    descEnd = i;
                    break;
                }
            }
            
            if (descEnd >= 0) {
                String description = json.substring(descStart, descEnd);
                // Unescape special characters
                description = description.replace("\\\"", "\"")
                                         .replace("\\\\", "\\")
                                         .replace("\\n", "\n")
                                         .replace("\\r", "\r")
                                         .replace("\\t", "\t");
                System.out.println("Extracted description: " + description); // Debug output
                return description;
            }
        }
        System.out.println("Failed to extract description"); // Debug output
        return "";
    }
    
    // Load tasks from file
    private static void loadTasks() {
        try {
            if (Files.exists(Paths.get(DATA_FILE))) {
                List<String> lines = Files.readAllLines(Paths.get(DATA_FILE));
                for (String line : lines) {
                    String[] parts = line.split("\\|", 3);
                    if (parts.length == 3) {
                        try {
                            long id = Long.parseLong(parts[0]);
                            String description = parts[1];
                            boolean completed = Boolean.parseBoolean(parts[2]);
                            
                            Task task = new Task(id, description);
                            task.setCompleted(completed);
                            tasks.add(task);
                            
                            if (id >= taskIdCounter) {
                                taskIdCounter = id + 1;
                            }
                        } catch (Exception e) {
                            System.err.println("Error parsing task line: " + line);
                        }
                    }
                }
                System.out.println("Loaded " + tasks.size() + " tasks from file.");
            }
        } catch (IOException e) {
            System.err.println("Error loading tasks: " + e.getMessage());
        }
    }
    
    // Save tasks to file
    private static void saveTasks() {
        try {
            List<String> lines = tasks.stream()
                .map(task -> {
                    String description = task.getDescription();
                    if (description == null) description = "";
                    // Replace | with a safe character since we use it as delimiter
                    description = description.replace("|", "&#124;");
                    return task.getId() + "|" + description + "|" + task.isCompleted();
                })
                .collect(Collectors.toList());
            Files.write(Paths.get(DATA_FILE), lines);
        } catch (IOException e) {
            System.err.println("Error saving tasks: " + e.getMessage());
        }
    }
    
    // Static file handler (for HTML, CSS, JS)
    static class StaticFileHandler implements HttpHandler {
        private final String filename;
        
        public StaticFileHandler(String filename) {
            this.filename = filename;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String content = Files.readString(Paths.get(filename));
            
            // Set content type based on file extension
            String contentType = "text/plain";
            if (filename.endsWith(".html")) contentType = "text/html";
            if (filename.endsWith(".css")) contentType = "text/css";
            if (filename.endsWith(".js")) contentType = "text/javascript";
            
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, content.length());
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content.getBytes());
            }
        }
    }
    
    // Handler for getting all tasks
    static class TasksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = tasksToJson(tasks);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method not allowed
            }
        }
    }
    
    // Handler for adding a new task
    static class AddTaskHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // Read request body
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String description = extractTaskDescription(requestBody);
                
                if (description == null || description.isEmpty()) {
                    System.err.println("WARNING: Empty task description received");
                    description = "Untitled Task";
                }
                
                // Create and add the new task
                Task newTask = new Task(taskIdCounter++, description);
                tasks.add(newTask);
                saveTasks();
                
                // Return the created task
                String response = taskToJson(newTask);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(201, response.length());
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method not allowed
            }
        }
    }
    
    // Handler for toggling task completion status
    static class ToggleTaskHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // Read task ID from request body
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                long taskId = extractTaskId(requestBody);
                
                // Find and toggle the task
                Task foundTask = tasks.stream()
                    .filter(t -> t.getId() == taskId)
                    .findFirst()
                    .orElse(null);
                    
                if (foundTask != null) {
                    foundTask.setCompleted(!foundTask.isCompleted());
                    saveTasks();
                    
                    String response = taskToJson(foundTask);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.length());
                    
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                } else {
                    exchange.sendResponseHeaders(404, -1); // Not found
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method not allowed
            }
        }
    }
    
    // Handler for deleting a task
    static class DeleteTaskHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // Read task ID from request body
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                long taskId = extractTaskId(requestBody);
                
                // Remove the task
                boolean removed = tasks.removeIf(t -> t.getId() == taskId);
                
                if (removed) {
                    saveTasks();
                    exchange.sendResponseHeaders(204, -1); // No content
                } else {
                    exchange.sendResponseHeaders(404, -1); // Not found
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method not allowed
            }
        }
    }
}