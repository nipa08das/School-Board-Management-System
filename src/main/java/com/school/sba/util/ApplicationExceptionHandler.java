package com.school.sba.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.school.sba.exception.AdminAlreadyExistsException;
import com.school.sba.exception.InvalidUserRoleException;
import com.school.sba.exception.UserNotFoundByIdException;

@RestControllerAdvice
public class ApplicationExceptionHandler extends ResponseEntityExceptionHandler{

	private ResponseEntity<Object> exceptionStructure(HttpStatus status, String message, Object rootcause)
	{
		return new ResponseEntity<Object>(
				Map.of("status",status.value(),
						"message",message,
						"rootcause",rootcause),status);
	}
	
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) 
	{
		List<ObjectError> allErrors = ex.getAllErrors();
		Map<String, String> errors = new HashMap<String, String>();
		
		allErrors.forEach(error ->{
							FieldError fieldError = (FieldError) error;
							errors.put(fieldError.getField(), fieldError.getDefaultMessage());
									});
		return exceptionStructure(HttpStatus.BAD_REQUEST, "Validations failed for some inputs, please check your fields properly", errors);
	}
	
	@ExceptionHandler(AdminAlreadyExistsException.class)
	public ResponseEntity<Object> handleAdminAlreadyExists(AdminAlreadyExistsException ex)
	{
		return exceptionStructure(HttpStatus.BAD_REQUEST, ex.getMessage(), "Admin already exist, cannot create more than one admin");
	}
	
	@ExceptionHandler(InvalidUserRoleException.class)
	public ResponseEntity<Object> handleInvalidUserRole(InvalidUserRoleException ex)
	{
		return exceptionStructure(HttpStatus.BAD_REQUEST, ex.getMessage(), "Invalid user Role, please provide User Role as Teacher, Student or Admin");
	}
	
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex)
	{
		return exceptionStructure(HttpStatus.BAD_REQUEST, ex.getMessage(), "username, email or password is not unique");
	}
	
	@ExceptionHandler(UserNotFoundByIdException.class)
	public ResponseEntity<Object> handleUserNotFoundById(UserNotFoundByIdException ex)
	{
		return exceptionStructure(HttpStatus.NOT_FOUND, ex.getMessage(), "User with given Id not found, please provide a valid User Id");
	}
}
