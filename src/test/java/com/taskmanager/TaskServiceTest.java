package com.taskmanager;

import com.taskmanager.model.Task;
import com.taskmanager.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for TaskService against an in-memory database migrated by Flyway.
 */
@SpringBootTest
class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService.clearAllTasks();
    }

    @Test
    @DisplayName("Should create a task successfully")
    void testCreateTask() {
        Task createdTask = taskService.createTask(new Task("Test Task", "Test Description"));

        assertNotNull(createdTask.getId());
        assertEquals("Test Task", createdTask.getTitle());
        assertEquals("Test Description", createdTask.getDescription());
        assertFalse(createdTask.isCompleted());
    }

    @Test
    @DisplayName("Should get all tasks")
    void testGetAllTasks() {
        taskService.createTask(new Task("Task 1", "Description 1"));
        taskService.createTask(new Task("Task 2", "Description 2"));

        List<Task> tasks = taskService.getAllTasks();

        assertEquals(2, tasks.size());
    }

    @Test
    @DisplayName("Should get task by ID")
    void testGetTaskById() {
        Task createdTask = taskService.createTask(new Task("Test Task", "Description"));

        Optional<Task> foundTask = taskService.getTaskById(createdTask.getId());

        assertTrue(foundTask.isPresent());
        assertEquals("Test Task", foundTask.get().getTitle());
    }

    @Test
    @DisplayName("Should return empty when task not found")
    void testGetTaskByIdNotFound() {
        assertFalse(taskService.getTaskById(999L).isPresent());
    }

    @Test
    @DisplayName("Should update task successfully")
    void testUpdateTask() {
        Task createdTask = taskService.createTask(new Task("Original", "Original Desc"));
        Task updatedData = new Task("Updated", "Updated Desc");
        updatedData.setCompleted(true);

        Optional<Task> updatedTask = taskService.updateTask(createdTask.getId(), updatedData);

        assertTrue(updatedTask.isPresent());
        assertEquals("Updated", updatedTask.get().getTitle());
        assertEquals("Updated Desc", updatedTask.get().getDescription());
        assertTrue(updatedTask.get().isCompleted());
    }

    @Test
    @DisplayName("Should delete task successfully")
    void testDeleteTask() {
        Task createdTask = taskService.createTask(new Task("To Delete", "Description"));

        assertTrue(taskService.deleteTask(createdTask.getId()));
        assertEquals(0, taskService.getTaskCount());
    }

    @Test
    @DisplayName("Should return false when deleting non-existent task")
    void testDeleteTaskNotFound() {
        assertFalse(taskService.deleteTask(999L));
    }

    @Test
    @DisplayName("Should return correct task count")
    void testGetTaskCount() {
        taskService.createTask(new Task("Task 1", "Desc 1"));
        taskService.createTask(new Task("Task 2", "Desc 2"));
        taskService.createTask(new Task("Task 3", "Desc 3"));

        assertEquals(3, taskService.getTaskCount());
    }

    @Test
    @DisplayName("Should clear all tasks")
    void testClearAllTasks() {
        taskService.createTask(new Task("Task 1", "Desc 1"));
        taskService.createTask(new Task("Task 2", "Desc 2"));

        taskService.clearAllTasks();

        assertEquals(0, taskService.getTaskCount());
    }
}
