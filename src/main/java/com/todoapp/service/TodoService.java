package com.todoapp.service;

import com.todoapp.dto.TodoDto;
import com.todoapp.entity.Todo;
import com.todoapp.entity.User;
import com.todoapp.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TodoService {
    
    private final TodoRepository todoRepository;
    
    public List<TodoDto.Response> getAllTodos(User user) {
        return todoRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public List<TodoDto.Response> getTodosByStatus(User user, boolean completed) {
        return todoRepository.findByUserAndCompletedOrderByCreatedAtDesc(user, completed)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public List<TodoDto.Response> getTodosByPriority(User user) {
        return todoRepository.findByUserOrderByPriority(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public TodoDto.Response getTodoById(Long id, User user) {
        Todo todo = todoRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Todo not found"));
        return mapToResponse(todo);
    }
    
    @Transactional
    public TodoDto.Response createTodo(TodoDto.CreateRequest request, User user) {
        Todo todo = Todo.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : "medium")
                .dueDate(request.getDueDate())
                .completed(false)
                .user(user)
                .build();
        
        todo = todoRepository.save(todo);
        return mapToResponse(todo);
    }
    
    @Transactional
    public TodoDto.Response updateTodo(Long id, TodoDto.UpdateRequest request, User user) {
        Todo todo = todoRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Todo not found"));
        
        if (request.getTitle() != null) {
            todo.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            todo.setDescription(request.getDescription());
        }
        if (request.getCompleted() != null) {
            todo.setCompleted(request.getCompleted());
        }
        if (request.getPriority() != null) {
            todo.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            todo.setDueDate(request.getDueDate());
        }
        
        todo = todoRepository.save(todo);
        return mapToResponse(todo);
    }
    
    @Transactional
    public TodoDto.Response toggleTodo(Long id, User user) {
        Todo todo = todoRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Todo not found"));
        
        todo.setCompleted(!todo.isCompleted());
        todo = todoRepository.save(todo);
        return mapToResponse(todo);
    }
    
    @Transactional
    public void deleteTodo(Long id, User user) {
        Todo todo = todoRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Todo not found"));
        todoRepository.delete(todo);
    }
    
    public TodoDto.StatsResponse getStats(User user) {
        long completed = todoRepository.countByUserAndCompleted(user, true);
        long pending = todoRepository.countByUserAndCompleted(user, false);
        
        return TodoDto.StatsResponse.builder()
                .total(completed + pending)
                .completed(completed)
                .pending(pending)
                .build();
    }
    
    private TodoDto.Response mapToResponse(Todo todo) {
        return TodoDto.Response.builder()
                .id(todo.getId())
                .title(todo.getTitle())
                .description(todo.getDescription())
                .completed(todo.isCompleted())
                .priority(todo.getPriority())
                .dueDate(todo.getDueDate())
                .createdAt(todo.getCreatedAt())
                .updatedAt(todo.getUpdatedAt())
                .build();
    }
}
