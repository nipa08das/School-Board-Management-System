package com.school.sba.serviceImpl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.school.sba.entity.School;
import com.school.sba.entity.User;
import com.school.sba.enums.UserRole;
import com.school.sba.exception.AdminAlreadyExistsException;
import com.school.sba.exception.InvalidUserRoleException;
import com.school.sba.exception.SchoolNotFoundException;
import com.school.sba.exception.UniqueConstraintViolationException;
import com.school.sba.exception.UserNotFoundByIdException;
import com.school.sba.repository.AcademicProgramRepository;
import com.school.sba.repository.ClassHourRepository;
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
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private ClassHourRepository classHourRepository;
	@Autowired
	private AcademicProgramRepository academicProgramRepository;

	//Mapper Methods
	private User mapToUser(@Valid UserRequest userRequest) 
	{
		return User.builder()
				.username(userRequest.getUsername())
				.password(passwordEncoder.encode(userRequest.getPassword()))
				.firstName(userRequest.getFirstName())
				.lastName(userRequest.getLastName())
				.contactNo(userRequest.getContactNo())
				.email(userRequest.getEmail())
				.userRole(UserRole.valueOf(userRequest.getUserRole().toUpperCase()))
				.isDeleted(false)
				.build();
	}

	public UserResponse mapToUserResponse(User user) 
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

	private ResponseEntity<ResponseStructure<UserResponse>> saveUser(UserRequest userRequest, UserRole role, School school) {
		if(school == null)
		{
			throw new SchoolNotFoundException("School is not registered with the admin, please register the School first");
		}
		else {
			try {
				User user = mapToUser(userRequest);
				user.setSchool(school);
				user = userRepository.save(user);
				return ResponseEntityProxy.getResponseEntity(HttpStatus.CREATED, "User data saved successfully", mapToUserResponse(user));
			} catch (DataIntegrityViolationException ex) {
				throw new UniqueConstraintViolationException("username, email or password is not unique");
			}
		}
	}
	
	public void deleteUserIfDeleted()
	{
		List<User> usersToDelete = new ArrayList<User>();
		
		userRepository.findByIsDeleted(true)
		.forEach(user ->
		{
			user.getClassHours().forEach(classHour -> classHour.setUser(null));
			classHourRepository.saveAll(user.getClassHours());
			
			user.getAcademicPrograms().forEach(academicProgram -> academicProgram.setUsers(null));
			academicProgramRepository.saveAll(user.getAcademicPrograms());
			
			usersToDelete.add(user);
		});
		userRepository.deleteAll(usersToDelete);
	}

	@Override
	public ResponseEntity<ResponseStructure<UserResponse>> registerAdmin(UserRequest userRequest) {
		try {
			UserRole role = UserRole.valueOf(userRequest.getUserRole().toUpperCase());

			if (role.equals(UserRole.ADMIN))
			{
				if (!userRepository.existsByUserRole(UserRole.ADMIN)) {
					User user = mapToUser(userRequest);
					user = userRepository.save(user);
					return ResponseEntityProxy.getResponseEntity(HttpStatus.CREATED, "User data saved successfully", mapToUserResponse(user));
				} else {
					throw new AdminAlreadyExistsException("Admin Already Exists");
				}	        	   
			} 
			else {
				throw new InvalidUserRoleException("Invalid user Role, please provide User Role as Admin");
			}

		} catch (IllegalArgumentException | NullPointerException ex) {
			throw new InvalidUserRoleException("Invalid user Role, please provide User Role as Admin");
		}	
	}

	@Override
	public ResponseEntity<ResponseStructure<UserResponse>> registerUser(@Valid UserRequest userRequest) {

		try {
			UserRole role = UserRole.valueOf(userRequest.getUserRole().toUpperCase());

			if (role.equals(UserRole.TEACHER) || role.equals(UserRole.STUDENT))
			{
				String username = SecurityContextHolder.getContext().getAuthentication().getName();

				return userRepository.findByUsername(username)
						.map(user -> saveUser(userRequest, role, user.getSchool()))
						.orElseThrow(() -> new UsernameNotFoundException("Only Admin can create the user"));	        	   
			} 
			else {
				throw new InvalidUserRoleException("Invalid user Role, please provide User Role as Teacher or Student");
			}

		} catch (IllegalArgumentException | NullPointerException ex) {
			throw new InvalidUserRoleException("Invalid User Role, please provide User Role as Teacher or Student");
		}
	}

	@Override
	public ResponseEntity<ResponseStructure<UserResponse>> deleteUser(int userId) 
	{
		return userRepository.findById(userId).map(user -> {

			if(user.getUserRole().equals(UserRole.ADMIN))
				throw new InvalidUserRoleException("Admin cannot be deleted");
			
			if(user.isDeleted())
				throw new UserNotFoundByIdException("Invalid User Id");

			user.setDeleted(true);
			userRepository.save(user);
			return ResponseEntityProxy.getResponseEntity(HttpStatus.OK, "User data deleted successfully", mapToUserResponse(user));

		}).orElseThrow(() -> new UserNotFoundByIdException("Invalid User Id"));
	}

	@Override
	public ResponseEntity<ResponseStructure<UserResponse>> findUserById(int userId)
	{
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundByIdException("Invalid User Id"));
		if(user.isDeleted())
			throw new UserNotFoundByIdException("Invalid User Id");

		return ResponseEntityProxy.getResponseEntity(HttpStatus.FOUND, "User data successfully found", mapToUserResponse(user));
	}






}
