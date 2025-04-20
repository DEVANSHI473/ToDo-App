public class Task {
    private long id;
    private String description;
    private boolean completed;
    
    public Task() {
        // Default constructor for Gson
    }
    
    public Task(long id, String description) {
        this.id = id;
        this.description = description;
        this.completed = false;
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}