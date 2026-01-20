package com.taskmanager.service;

import com.taskmanager.model.Task;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service class for managing tasks.
 * Uses in-memory storage for simplicity.
 */
@Service
public class TaskService {

    private final List<Task> tasks = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * Get all tasks.
     *
     * @return list of all tasks
     */
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    /**
     * Get a task by ID.
     *
     * @param id the task ID
     * @return optional containing the task if found
     */
    public Optional<Task> getTaskById(Long id) {
        return tasks.stream()
                .filter(task -> task.getId().equals(id))
                .findFirst();
    }

    /**
     * Create a new task.
     *
     * @param task the task to create
     * @return the created task with generated ID
     */
    public Task createTask(Task task) {
        task.setId(idGenerator.getAndIncrement());
        tasks.add(task);
        return task;
    }

    /**
     * Update an existing task.
     *
     * @param id          the task ID
     * @param updatedTask the updated task data
     * @return optional containing the updated task if found
     */
    public Optional<Task> updateTask(Long id, Task updatedTask) {
        return getTaskById(id).map(existingTask -> {
            existingTask.setTitle(updatedTask.getTitle());
            existingTask.setDescription(updatedTask.getDescription());
            existingTask.setCompleted(updatedTask.isCompleted());
            return existingTask;
        });
    }

    /**
     * Delete a task by ID.
     *
     * @param id the task ID
     * @return true if task was deleted, false otherwise
     */
    public boolean deleteTask(Long id) {
        return tasks.removeIf(task -> task.getId().equals(id));
    }

    /**
     * Get total count of tasks.
     *
     * @return the total number of tasks
     */
    public int getTaskCount() {
        return tasks.size();
    }

    /**
     * Clear all tasks.
     */
    public void clearAllTasks() {
        tasks.clear();
        idGenerator.set(1);
    }
}
