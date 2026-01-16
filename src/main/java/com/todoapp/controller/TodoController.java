package com.todoapp.controller;

import com.todoapp.dto.TodoDto;
import com.todoapp.entity.User;
import com.todoapp.service.TodoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {
    
    private final TodoService todoService;
    
    @GetMapping
    public ResponseEntity<List<TodoDto.Response>> getAllTodos(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false, defaultValue = "date") String sortBy
    ) {
        if (completed != null) {
            return ResponseEntity.ok(todoService.getTodosByStatus(user, completed));
        }
        
        if ("priority".equals(sortBy)) {
            return ResponseEntity.ok(todoService.getTodosByPriority(user));
        }
        
        return ResponseEntity.ok(todoService.getAllTodos(user));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TodoDto.Response> getTodoById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(todoService.getTodoById(id, user));
    }
    
    @PostMapping
    public ResponseEntity<TodoDto.Response> createTodo(
            @Valid @RequestBody TodoDto.CreateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(todoService.createTodo(request, user));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<TodoDto.Response> updateTodo(
            @PathVariable Long id,
            @Valid @RequestBody TodoDto.UpdateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(todoService.updateTodo(id, request, user));
    }
    
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<TodoDto.Response> toggleTodo(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(todoService.toggleTodo(id, user));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        todoService.deleteTodo(id, user);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/stats")
    public ResponseEntity<TodoDto.StatsResponse> getStats(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(todoService.getStats(user));
    }
}
