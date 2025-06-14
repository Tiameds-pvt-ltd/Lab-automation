package tiameds.com.tiameds.controller.auth;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.entity.Role;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.utils.ApiResponseHelper;


import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin")
@Tag(name = "Admin Controller", description = "Operations pertaining to admin management")
public class AdminController {

    private final UserService userService;
    @Autowired
    public AdminController(UserService userService) {
        this.userService = userService;
    }


    // ========================================= USER CRUD OPERATION  this is for super admin=========================================
    // 1. get all users
    // 2. get a user by ID
    // 3. update a user
    // 4. delete a user

    @GetMapping("/users")
    public ResponseEntity<List<User>> users() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable Long userId, @RequestBody User user) {
        try {
            User updatedUser = userService.updateUser(userId, user);
            return ResponseEntity.ok(updatedUser);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating the user.");
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok("User deleted successfully");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while deleting the user.");
        }
    }




    // ========================================= ROLE CRUD OPERATION =========================================
    // 1. Create a new role
    // 2. Assign a role to a user
    // 3. Remove a role from a user
    // 4. Delete a role
    // 5. Get a role by ID
    // 6. Get all roles


    // 1. Create a new role
    @PostMapping("/roles")
    public ResponseEntity<Role> createRole(@RequestBody Role role) {
        Role newRole = userService.saveRole(role);
        return ResponseEntity.ok(newRole);
    }

    // 2. Assign a role to a user
    @PostMapping("/users/{userId}/roles/{roleId}")
    public ResponseEntity<?> assignRole(@PathVariable Long userId, @PathVariable Long roleId) {
        try {
            User updatedUser = userService.assignRole(userId, Math.toIntExact(roleId));
            // If the role assignment is successful, return the updated user
            return ResponseEntity.ok(updatedUser);
        } catch (EntityNotFoundException e) {
            // Handle the case where either the user or role is not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User or Role not found.");
        } catch (Exception e) {
            // Handle any other exceptions that might occur
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while assigning the role.");
        }
    }


    // 3. Remove a role from a user
    @DeleteMapping("/users/{userId}/roles/{roleId}")
    public ResponseEntity<?> removeRole(@PathVariable Long userId, @PathVariable Long roleId) {
        try {
            User updatedUser = userService.removeRole(userId, Math.toIntExact(roleId));
            // If the role removal is successful, return the updated user
            return ResponseEntity.ok(updatedUser);
        } catch (EntityNotFoundException e) {
            // Handle the case where either the user or role is not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User or Role not found.");
        } catch (Exception e) {
            // Handle any other exceptions that might occur
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while removing the role.");
        }
    }



    // 5. Get a role by ID
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUser(@PathVariable Long userId) {
        try {
            User user = userService.getUser(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
            return ApiResponseHelper.successResponse("User fetched successfully", user);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching the user.");
        }
    }

    // 6. Get all roles
    @GetMapping("/roles")
    public ResponseEntity<?> roles() {
        try {
            List<Role> roles = userService.getAllRoles();
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            // Log the exception (optional)
            System.err.println("Error fetching roles: " + e.getMessage());
            // Return a response entity with a generic error message and HTTP status
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching roles.");
        }
    }
}
