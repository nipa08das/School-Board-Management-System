package com.school.sba.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.school.sba.entity.User;
import com.school.sba.enums.UserRole;
import com.school.sba.exception.AdminAlreadyExistsException;
import com.school.sba.exception.InvalidUserRoleException;
import com.school.sba.repository.UserRepository;
import com.school.sba.request_dto.UserRequest;
import com.school.sba.response_dto.UserResponse;
import com.school.sba.service.UserService;
import com.school.sba.util.ResponseEntityProxy;
import com.school.sba.util.ResponseStructure;

import jakarta.validation.Valid;

@Service
public class UserServiceImpl implements UserService {
	@Autowired
	private UserRepository userRepository;

	//Mapper Methods
	private User mapToUser(@Valid UserRequest userRequest) 
	{
		return User.builder()
				.username(userRequest.getUsername())
				.password(userRequest.getPassword())
				.firstName(userRequest.getFirstName())
				.lastName(userRequest.getLastName())
				.contactNo(userRequest.getContactNo())
				.email(userRequest.getEmail())
				.userRole(UserRole.valueOf(userRequest.getUserRole().toUpperCase()))
				.build();
	}
	
	private UserResponse mapToUserResponse(User user) 
	{
		return UserResponse.builder()
				.userId(user.getUserId())
				.username(user.getUsername())
				.firstName(user.getFirstName())
				.lastName(user.getLastName())
				.contactNo(user.getContactNo())
				.email(user.getEmail())
				.userRole(user.getUserRole())
				.build();
	}
	
	private ResponseEntity<ResponseStructure<UserResponse>> saveUser(UserRequest userRequest, UserRole role) {
	    try {
	        User user = userRepository.save(mapToUser(userRequest));
	        return ResponseEntityProxy.getResponseEntity(HttpStatus.CREATED, "User data saved successfully", mapToUserResponse(user));
	    } catch (DataIntegrityViolationException ex) {
	        throw new DataIntegrityViolationException("Uniqueness should me maintained");
	    }
	}
	
	private ResponseEntity<ResponseStructure<UserResponse>> registerAdmin(UserRequest userRequest) {
	    if (!userRepository.existsByUserRole(UserRole.ADMIN)) {
	        return saveUser(userRequest, UserRole.ADMIN);
	    } else {
	        throw new AdminAlreadyExistsException("Admin Already Exists");
	    }
	}
	
	@Override
	public ResponseEntity<ResponseStructure<UserResponse>> register(@Valid UserRequest userRequest) {
	    try {
	        UserRole role = UserRole.valueOf(userRequest.getUserRole().toUpperCase());

	        if (role.equals(UserRole.TEACHER) || role.equals(UserRole.STUDENT)) {
	            return saveUser(userRequest, role);
	        } else if (role.equals(UserRole.ADMIN)) {
	            return registerAdmin(userRequest);
	        } else {
	            throw new InvalidUserRoleException("Invalid User Role");
	        }

	    } catch (IllegalArgumentException | NullPointerException ex) {
	        throw new InvalidUserRoleException("Invalid User Role");
	    }
	}
	
	

	

}
