package com.school.sba.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.school.sba.request_dto.AcademicProgramRequest;
import com.school.sba.response_dto.AcademicProgramResponse;
import com.school.sba.response_dto.UserResponse;
import com.school.sba.service.AcademicProgramService;
import com.school.sba.util.ResponseStructure;

import jakarta.validation.Valid;

@RestController
public class AcademicProgramController {
	@Autowired
	private AcademicProgramService academicProgramService;

	@PostMapping("/schools/{schoolId}/academic-programs")
	//@PreAuthorize("hasAuthority('ADMIN')")
	public ResponseEntity<ResponseStructure<AcademicProgramResponse>> saveAcademicProgram(
			@RequestBody @Valid AcademicProgramRequest academicProgramRequest,@PathVariable int schoolId)
	{
		return academicProgramService.saveAcademicProgram(academicProgramRequest,schoolId);
	}
	
	@GetMapping("/schools/{schoolId}/academic-programs")
	public ResponseEntity<ResponseStructure<List<AcademicProgramResponse>>> findAllAcademicProgram(@PathVariable int schoolId)
	{
		return academicProgramService.findAllAcademicProgram(schoolId);
	}
	
	@PutMapping("/academic-programs/{programId}/users/{userId}")
	@PreAuthorize("hasAuthority('ADMIN')")
	public ResponseEntity<ResponseStructure<AcademicProgramResponse>> assignAcademicProgramToUser(@PathVariable int programId, @PathVariable int userId)
	{
		return academicProgramService.assignAcademicProgramToUser(programId, userId);
	}
	
	@GetMapping("/academic-programs/{programId}/user-roles/{userRole}/users")
	public ResponseEntity<ResponseStructure<List<UserResponse>>> findAllUserInAcademicProgram(@PathVariable int programId, @PathVariable String userRole)
	{
		return academicProgramService.findAllUserInAcademicProgram(programId, userRole);
	}
	
	@DeleteMapping("/academic-programs/{programId}")
	public ResponseEntity<ResponseStructure<AcademicProgramResponse>> deleteAcademicProgram(@PathVariable int programId)
	{
		return academicProgramService.deleteAcademicProgram(programId);
	}
}
