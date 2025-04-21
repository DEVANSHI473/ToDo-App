// Fixed script.js
document.addEventListener('DOMContentLoaded', function() {
    const taskForm = document.getElementById('task-form');
    const taskInput = document.getElementById('task-input');
    const taskList = document.getElementById('task-list');
    const emptyState = document.getElementById('empty-state');
    
    // Load tasks from the server when the page loads
    loadTasks();
    
    // Add task form submission handler
    taskForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        const description = taskInput.value.trim();
        if (description) {
            addTask(description);
            taskInput.value = '';
        }
    });
    
    // Function to load all tasks from the server
    function loadTasks() {
        fetch('/api/tasks')
            .then(response => response.json())
            .then(tasks => {
                console.log("Loaded tasks:", tasks); // Debug: Log the received tasks
                renderTasks(tasks);
            })
            .catch(error => {
                console.error('Error loading tasks:', error);
            });
    }
    
    // Function to render tasks to the UI
    function renderTasks(tasks) {
        taskList.innerHTML = '';
        
        if (tasks.length === 0) {
            taskList.style.display = 'none';
            emptyState.style.display = 'block';
            return;
        }
        
        taskList.style.display = 'block';
        emptyState.style.display = 'none';
        
        tasks.forEach(task => {
            const taskItem = document.createElement('li');
            taskItem.className = `task-item ${task.completed ? 'completed' : ''}`;
            taskItem.dataset.id = task.id;
            
            const taskText = document.createElement('span');
            taskText.className = 'task-text';
            taskText.textContent = task.description || "Untitled Task";
            
            const taskActions = document.createElement('div');
            taskActions.className = 'task-actions';
            
            const toggleBtn = document.createElement('button');
            toggleBtn.className = 'toggle-btn';
            toggleBtn.textContent = task.completed ? 'Mark Incomplete' : 'Mark Complete';
            toggleBtn.addEventListener('click', () => toggleTask(task.id));
            
            const deleteBtn = document.createElement('button');
            deleteBtn.className = 'delete-btn';
            deleteBtn.textContent = 'Delete';
            deleteBtn.addEventListener('click', () => deleteTask(task.id));
            
            taskActions.appendChild(toggleBtn);
            taskActions.appendChild(deleteBtn);
            
            taskItem.appendChild(taskText);
            taskItem.appendChild(taskActions);
            
            taskList.appendChild(taskItem);
        });
    }
    
    // Function to add a new task
    function addTask(description) {
        console.log("Adding task with description:", description); // Debug: Log the description being sent
        
        fetch('/api/tasks/add', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ description: description })
        })
        .then(response => response.json())
        .then(task => {
            console.log("Task added:", task); // Debug: Log the response
            // Reload all tasks to ensure we have the latest state
            loadTasks();
        })
        .catch(error => {
            console.error('Error adding task:', error);
        });
    }
    
    // Function to toggle task completion status
    function toggleTask(taskId) {
        fetch('/api/tasks/toggle', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ id: taskId })
        })
        .then(response => {
            if (response.ok) {
                // Reload all tasks to ensure we have the latest state
                loadTasks();
            }
        })
        .catch(error => {
            console.error('Error toggling task:', error);
        });
    }
    
    // Function to delete a task
    function deleteTask(taskId) {
        fetch('/api/tasks/delete', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ id: taskId })
        })
        .then(response => {
            if (response.ok) {
                // Reload all tasks to ensure we have the latest state
                loadTasks();
            }
        })
        .catch(error => {
            console.error('Error deleting task:', error);
        });
    }
});