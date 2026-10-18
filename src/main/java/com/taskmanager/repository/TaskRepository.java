package com.taskmanager.repository;

import com.taskmanager.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for tasks stored in PostgreSQL.
 */
public interface TaskRepository extends JpaRepository<Task, Long> {
}
