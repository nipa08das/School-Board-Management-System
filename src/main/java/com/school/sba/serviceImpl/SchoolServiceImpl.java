package com.school.sba.serviceImpl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.school.sba.entity.School;
import com.school.sba.entity.User;
import com.school.sba.enums.UserRole;
import com.school.sba.exception.SchoolExceededException;
import com.school.sba.exception.SchoolNotFoundByIdException;
import com.school.sba.exception.UnauthorizedException;
import com.school.sba.repository.AcademicProgramRepository;
import com.school.sba.repository.SchoolRepository;
import com.school.sba.repository.UserRepository;
import com.school.sba.request_dto.SchoolRequest;
import com.school.sba.response_dto.SchoolResponse;
import com.school.sba.service.SchoolService;
import com.school.sba.util.ResponseEntityProxy;
import com.school.sba.util.ResponseStructure;

import jakarta.validation.Valid;

@Service
public class SchoolServiceImpl implements SchoolService {
	@Autowired
	private SchoolRepository schoolRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private AcademicProgramRepository academicProgramRepository;

	//Mapper Methods
	private School mapToSchool(SchoolRequest schoolRequest) 
	{
		return School.builder()
				.schoolName(schoolRequest.getSchoolName())
				.contactNo(schoolRequest.getContactNo())
				.emailId(schoolRequest.getEmailId())
				.address(schoolRequest.getAddress())
				.build();
	}

	private SchoolResponse mapToSchoolResponse(School school) 
	{
		return SchoolResponse.builder()
				.schoolId(school.getSchoolId())
				.schoolName(school.getSchoolName())
				.contactNo(school.getContactNo())
				.emailId(school.getEmailId())
				.address(school.getAddress())
				.build();
	}

	public void deleteSchoolIfDeleted()
	{
		schoolRepository.findByIsDeleted(true)
		.forEach(school ->
		{
			if(!school.getAcademicPrograms().isEmpty())
				academicProgramRepository.deleteAll(school.getAcademicPrograms());

			List<User> usersToDelete = school.getUsers().stream()
					.peek(user -> {
						if(user.getUserRole().equals(UserRole.ADMIN))
						{
							user.setSchool(null);
							userRepository.save(user);
						}
					})
					.filter(user -> !user.getUserRole().equals(UserRole.ADMIN))
					.collect(Collectors.toList());

			userRepository.deleteAll(usersToDelete);

			schoolRepository.delete(school);
		});
	}


	@Override	
	public ResponseEntity<ResponseStructure<SchoolResponse>> saveSchool(@Valid SchoolRequest schoolRequest) {

		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		return userRepository.findByUsername(username)
				.map(user -> {
					if (user.getUserRole().equals(UserRole.ADMIN)) {
						if (user.getSchool() == null) {
							School school = schoolRepository.save(mapToSchool(schoolRequest));
							user.setSchool(school);
							userRepository.save(user);
							return ResponseEntityProxy.getResponseEntity(HttpStatus.CREATED, "School data saved successfully.", mapToSchoolResponse(school));
						} else {
							throw new SchoolExceededException("Cannot create more than one school.");
						}
					} else {
						throw new UnauthorizedException("Only admins are allowed to create schools.");
					}
				})
				.orElseThrow(() -> new UnauthorizedException("Only admins are allowed to create schools."));
	}

	@Override
	public ResponseEntity<ResponseStructure<SchoolResponse>> deleteSchool(int schoolId)
	{
		return schoolRepository.findById(schoolId).map(school -> {

			if(school.isDeleted())
				throw new SchoolNotFoundByIdException("Invalid School Id");

			school.setDeleted(true);
			schoolRepository.save(school);
			return ResponseEntityProxy.getResponseEntity(HttpStatus.OK, "School deleted successfully", mapToSchoolResponse(school));
		}).orElseThrow(() -> new SchoolNotFoundByIdException("Invalid School Id"));
	}

}
