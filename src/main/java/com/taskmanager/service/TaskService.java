package com.taskmanager.service;

import com.taskmanager.model.Task;
import com.taskmanager.repository.TaskRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for managing tasks.
 * Tasks are persisted in PostgreSQL so every replica shares the same state.
 */
@Service
public class TaskService {

    private static final Logger LOG = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository repository;
    private final Counter createdCounter;
    private final Counter deletedCounter;

    /**
     * Creates the service.
     *
     * @param repository    task repository
     * @param meterRegistry registry used for business metrics
     */
    public TaskService(TaskRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.createdCounter = Counter.builder("tasks_created")
                .description("Tasks created").register(meterRegistry);
        this.deletedCounter = Counter.builder("tasks_deleted")
                .description("Tasks deleted").register(meterRegistry);
    }

    /**
     * Get all tasks.
     *
     * @return list of all tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getAllTasks() {
        return repository.findAll();
    }

    /**
     * Get a task by ID.
     *
     * @param id the task ID
     * @return optional containing the task if found
     */
    @Transactional(readOnly = true)
    public Optional<Task> getTaskById(Long id) {
        return repository.findById(id);
    }

    /**
     * Create a new task.
     *
     * @param task the task to create
     * @return the created task with generated ID
     */
    @Transactional
    public Task createTask(Task task) {
        task.setId(null);
        Task saved = repository.save(task);
        createdCounter.increment();
        LOG.info("Created task id={}", saved.getId());
        return saved;
    }

    /**
     * Update an existing task.
     *
     * @param id          the task ID
     * @param updatedTask the updated task data
     * @return optional containing the updated task if found
     */
    @Transactional
    public Optional<Task> updateTask(Long id, Task updatedTask) {
        return repository.findById(id).map(existingTask -> {
            existingTask.setTitle(updatedTask.getTitle());
            existingTask.setDescription(updatedTask.getDescription());
            existingTask.setCompleted(updatedTask.isCompleted());
            return repository.save(existingTask);
        });
    }

    /**
     * Delete a task by ID.
     *
     * @param id the task ID
     * @return true if task was deleted, false otherwise
     */
    @Transactional
    public boolean deleteTask(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        deletedCounter.increment();
        LOG.info("Deleted task id={}", id);
        return true;
    }

    /**
     * Get total count of tasks.
     *
     * @return the total number of tasks
     */
    @Transactional(readOnly = true)
    public long getTaskCount() {
        return repository.count();
    }

    /**
     * Clear all tasks.
     */
    @Transactional
    public void clearAllTasks() {
        repository.deleteAll();
    }
}
