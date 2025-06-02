package tiameds.com.tiameds.controller.auth;


import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.entity.ModuleEntity;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.ModuleRepository;
import tiameds.com.tiameds.repository.UserRepository;
import tiameds.com.tiameds.utils.ApiResponse;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/admin")
@Tag(name = "Module Controller", description = "Operations pertaining to module management")
public class ModuleController {
    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;

    public ModuleController(ModuleRepository moduleRepository, UserRepository userRepository) {
        this.moduleRepository = moduleRepository;
        this.userRepository = userRepository;
    }

    // ========================================= MODULE CRUD OPERATION  this is for super admin=========================================
    // 1. get all modules
    // 2. get a module by ID
    // 3. update a module
    // 4. delete a module

    @GetMapping("/modules")
    public ResponseEntity<ApiResponse<Object>> modules() {
        List<ModuleEntity> modules = moduleRepository.findAll();
        return ApiResponseHelper.successResponse("Modules retrieved successfully", modules);
    }

    @GetMapping("/modules/{moduleId}")
    public ResponseEntity<?> getModuleById(@PathVariable Long moduleId) {
        try {
            ModuleEntity module = moduleRepository.findById(moduleId)
                    .orElseThrow(() -> new EntityNotFoundException("Module not found with id " + moduleId));
            return ApiResponseHelper.successResponse("Module retrieved successfully", module);
        } catch (EntityNotFoundException e) {
            return ApiResponseHelper.errorResponseWithData("Module not found with id " + moduleId, HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/modules")
    public ResponseEntity<?> createModule(@RequestBody ModuleEntity module) {
        try {
            ModuleEntity newModule = moduleRepository.save(module);
            return ApiResponseHelper.successResponse("Module created successfully", newModule);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponseWithData("Error creating module", HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/modules/{moduleId}")
    public ResponseEntity<?> updateModule(@PathVariable Long moduleId, @RequestBody ModuleEntity module) {
        try {
            ModuleEntity updatedModule = moduleRepository.findById(moduleId)
                    .map(existingModule -> {
                        existingModule.setName(module.getName());
                        return moduleRepository.save(existingModule);
                    })
                    .orElseThrow(() -> new EntityNotFoundException("Module not found with id " + moduleId));
            return ApiResponseHelper.successResponse("Module updated successfully", updatedModule);
        } catch (EntityNotFoundException e) {
            return ApiResponseHelper.errorResponseWithData("Module not found with id " + moduleId, HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/modules/{moduleId}")
    public ResponseEntity<?> deleteModule(@PathVariable Long moduleId) {
        try {
            moduleRepository.deleteById(moduleId);
            return ApiResponseHelper.successResponse("Module deleted successfully", null);
        } catch (EmptyResultDataAccessException e) {
            return ApiResponseHelper.errorResponseWithData("Module not found with id " + moduleId, HttpStatus.NOT_FOUND);
        }
    }

    // ========================================= MODULE CRUD OPERATION  this is for super admin=========================================
    // add module to user
    @PostMapping("/users/{userId}/modules/{moduleId}")
    public ResponseEntity<?> addModuleToUser(@PathVariable Long userId, @PathVariable Long moduleId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with id " + userId));
            ModuleEntity module = moduleRepository.findById(moduleId)
                    .orElseThrow(() -> new EntityNotFoundException("Module not found with id " + moduleId));

            //check if module is already added to user
            if(user.getModules().contains(module)){
                return ApiResponseHelper.errorResponseWithData("Module already added to user", HttpStatus.BAD_REQUEST);
            }
            // add module to user
            user.getModules().add(module);
            userRepository.save(user);
            return ApiResponseHelper.successResponse("Module added to user successfully", user);
        } catch (EntityNotFoundException e) {
            return ApiResponseHelper.errorResponseWithData("User or Module not found", HttpStatus.NOT_FOUND);
        }
    }


    // remove module from user
    @DeleteMapping("/users/{userId}/modules/{moduleId}")
    public ResponseEntity<?> removeModuleFromUser(@PathVariable Long userId, @PathVariable Long moduleId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with id " + userId));
            ModuleEntity module = moduleRepository.findById(moduleId)
                    .orElseThrow(() -> new EntityNotFoundException("Module not found with id " + moduleId));
            //check module is already exist in user
            if(!user.getModules().contains(module)){
                return ApiResponseHelper.errorResponseWithData("Module not added to user", HttpStatus.BAD_REQUEST);
            }
            // remove module from user
            user.getModules().remove(module);
            userRepository.save(user);
            return ApiResponseHelper.successResponse("Module removed from user successfully", user);
        } catch (EntityNotFoundException e) {
            return ApiResponseHelper.errorResponseWithData("User or Module not found", HttpStatus.NOT_FOUND);
        }
    }
}
