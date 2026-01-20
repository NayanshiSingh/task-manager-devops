package com.taskmanager;

import com.taskmanager.model.Task;
import com.taskmanager.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for TaskService.
 */
class TaskServiceTest {

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService();
    }

    @Test
    @DisplayName("Should create a task successfully")
    void testCreateTask() {
        // Given
        Task task = new Task("Test Task", "Test Description");

        // When
        Task createdTask = taskService.createTask(task);

        // Then
        assertNotNull(createdTask.getId());
        assertEquals("Test Task", createdTask.getTitle());
        assertEquals("Test Description", createdTask.getDescription());
        assertFalse(createdTask.isCompleted());
    }

    @Test
    @DisplayName("Should get all tasks")
    void testGetAllTasks() {
        // Given
        taskService.createTask(new Task("Task 1", "Description 1"));
        taskService.createTask(new Task("Task 2", "Description 2"));

        // When
        List<Task> tasks = taskService.getAllTasks();

        // Then
        assertEquals(2, tasks.size());
    }

    @Test
    @DisplayName("Should get task by ID")
    void testGetTaskById() {
        // Given
        Task createdTask = taskService.createTask(new Task("Test Task", "Description"));

        // When
        Optional<Task> foundTask = taskService.getTaskById(createdTask.getId());

        // Then
        assertTrue(foundTask.isPresent());
        assertEquals("Test Task", foundTask.get().getTitle());
    }

    @Test
    @DisplayName("Should return empty when task not found")
    void testGetTaskByIdNotFound() {
        // When
        Optional<Task> foundTask = taskService.getTaskById(999L);

        // Then
        assertFalse(foundTask.isPresent());
    }

    @Test
    @DisplayName("Should update task successfully")
    void testUpdateTask() {
        // Given
        Task createdTask = taskService.createTask(new Task("Original", "Original Desc"));
        Task updatedData = new Task("Updated", "Updated Desc");
        updatedData.setCompleted(true);

        // When
        Optional<Task> updatedTask = taskService.updateTask(createdTask.getId(), updatedData);

        // Then
        assertTrue(updatedTask.isPresent());
        assertEquals("Updated", updatedTask.get().getTitle());
        assertEquals("Updated Desc", updatedTask.get().getDescription());
        assertTrue(updatedTask.get().isCompleted());
    }

    @Test
    @DisplayName("Should delete task successfully")
    void testDeleteTask() {
        // Given
        Task createdTask = taskService.createTask(new Task("To Delete", "Description"));

        // When
        boolean deleted = taskService.deleteTask(createdTask.getId());

        // Then
        assertTrue(deleted);
        assertEquals(0, taskService.getTaskCount());
    }

    @Test
    @DisplayName("Should return false when deleting non-existent task")
    void testDeleteTaskNotFound() {
        // When
        boolean deleted = taskService.deleteTask(999L);

        // Then
        assertFalse(deleted);
    }

    @Test
    @DisplayName("Should return correct task count")
    void testGetTaskCount() {
        // Given
        taskService.createTask(new Task("Task 1", "Desc 1"));
        taskService.createTask(new Task("Task 2", "Desc 2"));
        taskService.createTask(new Task("Task 3", "Desc 3"));

        // When
        int count = taskService.getTaskCount();

        // Then
        assertEquals(3, count);
    }

    @Test
    @DisplayName("Should clear all tasks")
    void testClearAllTasks() {
        // Given
        taskService.createTask(new Task("Task 1", "Desc 1"));
        taskService.createTask(new Task("Task 2", "Desc 2"));

        // When
        taskService.clearAllTasks();

        // Then
        assertEquals(0, taskService.getTaskCount());
    }
}
