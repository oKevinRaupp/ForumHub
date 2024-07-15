package com.kevinraupp.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import com.kevinraupp.domain.user.User;
import com.kevinraupp.domain.user.UserRepository;
import com.kevinraupp.domain.user.dto.CreateUserDTO;
import com.kevinraupp.domain.user.dto.UserResponseDTO;
import com.kevinraupp.domain.user.dto.UserUpdateDTO;
import com.kevinraupp.infra.exceptions.NotFound;
import com.kevinraupp.infra.exceptions.Forbidden;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@SecurityRequirement(name = "bearer-key")
public class UserController {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public UserController(PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<UserResponseDTO> create(@RequestBody @Valid CreateUserDTO userDTO) {
        String encodedPass = passwordEncoder.encode(userDTO.password());
        var user = new User(userDTO, encodedPass);
        userRepository.save(user);

        return ResponseEntity.ok().body(new UserResponseDTO(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getById(@PathVariable Long id) {
        var user = userRepository.findById(id).orElseThrow(() -> new NotFound("User not found"));
        return ResponseEntity.ok().body(new UserResponseDTO(user));
    }

    @GetMapping
    public ResponseEntity<Page<UserResponseDTO>> getUsers(@PageableDefault(size = 10, sort = {"name"}) Pageable pagination) {
        var page = userRepository.findAllByIsActiveTrue(pagination).map(UserResponseDTO::new);
        return ResponseEntity.ok(page);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<UserResponseDTO> update(@RequestBody @Valid UserUpdateDTO userData, @PathVariable Long id, Authentication authentication) {
        var userRequestId = authentication.getCredentials().toString();
        var isAdmin = authentication.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        var user = userRepository.findById(id).orElseThrow(() -> new NotFound("User not found"));

        if (!user.getId().toString().equals(userRequestId) && !isAdmin) {
            throw new Forbidden("You do not have permission to perform this action");
        }

        if(userData.password() != null){
            String encodedPassword = passwordEncoder.encode(userData.password());
            user.setHash(encodedPassword);
        }
        user.update(userData);
        return ResponseEntity.ok(new UserResponseDTO(user));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        var user = userRepository.findById(id).orElseThrow(() -> new NotFound("User not found!"));
        user.delete();

        return ResponseEntity.noContent().build();
    }

}
