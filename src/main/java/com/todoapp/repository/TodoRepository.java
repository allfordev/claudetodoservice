package com.todoapp.repository;

import com.todoapp.entity.Todo;
import com.todoapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    List<Todo> findByUserOrderByCreatedAtDesc(User user);
    
    List<Todo> findByUserAndCompletedOrderByCreatedAtDesc(User user, boolean completed);
    
    Optional<Todo> findByIdAndUser(Long id, User user);
    
    @Query("SELECT t FROM Todo t WHERE t.user = :user ORDER BY " +
           "CASE t.priority WHEN 'high' THEN 1 WHEN 'medium' THEN 2 WHEN 'low' THEN 3 END, " +
           "t.createdAt DESC")
    List<Todo> findByUserOrderByPriority(@Param("user") User user);
    
    long countByUserAndCompleted(User user, boolean completed);

    @Query("SELECT t FROM Todo t WHERE t.user = :user AND t.dueDate < CURRENT_TIMESTAMP AND t.completed = false")
    List<Todo> findOverdueTodos(@Param("user") User user);
}
