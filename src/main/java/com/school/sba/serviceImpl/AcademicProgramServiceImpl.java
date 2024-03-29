package com.school.sba.serviceImpl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.school.sba.entity.AcademicProgram;
import com.school.sba.entity.School;
import com.school.sba.entity.User;
import com.school.sba.enums.ProgramType;
import com.school.sba.enums.UserRole;
import com.school.sba.exception.AcademicProgramNotFoundByIdException;
import com.school.sba.exception.AcademicProgramNotFoundBySchoolIdException;
import com.school.sba.exception.InvalidProgramTypeException;
import com.school.sba.exception.InvalidAcademicProgramAssignmentToTeacherException;
import com.school.sba.exception.InvalidUserRoleException;
import com.school.sba.exception.SchoolNotFoundByIdException;
import com.school.sba.exception.SubjectNotAssignedToTeacherException;
import com.school.sba.exception.SubjectNotFoundInAcademicProgramException;
import com.school.sba.exception.UserNotFoundByIdException;
import com.school.sba.exception.UserNotFoundInAcademicProgramException;
import com.school.sba.repository.AcademicProgramRepository;
import com.school.sba.repository.SchoolRepository;
import com.school.sba.repository.UserRepository;
import com.school.sba.request_dto.AcademicProgramRequest;
import com.school.sba.response_dto.AcademicProgramResponse;
import com.school.sba.response_dto.UserResponse;
import com.school.sba.service.AcademicProgramService;
import com.school.sba.util.ResponseEntityProxy;
import com.school.sba.util.ResponseStructure;

import jakarta.validation.Valid;

@Service
public class AcademicProgramServiceImpl implements AcademicProgramService{
	@Autowired
	private SchoolRepository schoolRepository;
	@Autowired
	private AcademicProgramRepository academicProgramRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private UserServiceImpl userServiceImpl;
	
	public boolean autoGenerateClassHour = false;

	//Mapper Methods
	private AcademicProgram mapToAcademicProgram(AcademicProgramRequest academicProgramRequest)
	{
		return AcademicProgram.builder()
				.programType(ProgramType.valueOf(academicProgramRequest.getProgramType().toUpperCase()))
				.programName(academicProgramRequest.getProgramName())
				.beginsAt(academicProgramRequest.getBeginsAt())
				.endsAt(academicProgramRequest.getEndsAt())
				.build();
	}

	public AcademicProgramResponse mapToAcademicProgramResponse(AcademicProgram academicProgram)
	{
		List<String> subjectNames = new ArrayList<String>();
		if(academicProgram.getSubjects()!=null) {
			academicProgram.getSubjects().forEach(subject -> {
				subjectNames.add(subject.getSubjectName().toUpperCase());
			});
		}

		return AcademicProgramResponse.builder()
				.programId(academicProgram.getProgramId())
				.programType(academicProgram.getProgramType())
				.programName(academicProgram.getProgramName())
				.beginsAt(academicProgram.getBeginsAt())
				.endsAt(academicProgram.getEndsAt())
				.subjectNames(subjectNames)
				.build();
	}

	public void deleteAcademicProgramIfDeleted()
	{		
		List<AcademicProgram> academicProgramsToDelete = academicProgramRepository.findByIsDeleted(true);
		if(!academicProgramsToDelete.isEmpty())
			academicProgramRepository.deleteAll(academicProgramsToDelete);
	}

	@Override
	public ResponseEntity<ResponseStructure<AcademicProgramResponse>> saveAcademicProgram(
			@Valid AcademicProgramRequest academicProgramRequest,int schoolId) 
	{
		try {
			ProgramType programType = ProgramType.valueOf(academicProgramRequest.getProgramType().toUpperCase());
			if(EnumSet.allOf(ProgramType.class).contains(programType))
			{

				School school = schoolRepository.findById(schoolId).orElseThrow(() -> new SchoolNotFoundByIdException("Invalid School Id"));

				AcademicProgram academicProgram = mapToAcademicProgram(academicProgramRequest);
				academicProgram.setSchool(school);
				academicProgram = academicProgramRepository.save(academicProgram);
				return ResponseEntityProxy.getResponseEntity(HttpStatus.CREATED, "Academic Program created successfully", mapToAcademicProgramResponse(academicProgram));
			}
			else {
				throw new InvalidProgramTypeException("Invalid Program Type");
			}
		}
		catch (IllegalArgumentException | NullPointerException ex) {
			throw new InvalidProgramTypeException("Invalid Program Type");
		}
	}

	@Override
	public ResponseEntity<ResponseStructure<List<AcademicProgramResponse>>> findAllAcademicProgram(int schoolId) {
		return schoolRepository.findById(schoolId).map(school -> {
			if(school.isDeleted())
				throw new SchoolNotFoundByIdException("Invalid School Id");

			List<AcademicProgram> academicPrograms = academicProgramRepository.findBySchoolSchoolId(school.getSchoolId());
			if (academicPrograms.isEmpty()) {
				throw new AcademicProgramNotFoundBySchoolIdException("No Academic Program details found");
			} else {
				List<AcademicProgramResponse> academicProgramsResponse = academicPrograms.stream()
						.filter(AcademicProgram -> !AcademicProgram.isDeleted())
						.map(this::mapToAcademicProgramResponse)
						.collect(Collectors.toList());
				return ResponseEntityProxy.getResponseEntity(HttpStatus.FOUND, "All Academic Program details found successfully", academicProgramsResponse);
			}
		}).orElseThrow(() -> new SchoolNotFoundByIdException("Invalid School Id"));
	}

	@Override
	public ResponseEntity<ResponseStructure<AcademicProgramResponse>> assignAcademicProgramToUser(int programId,int userId) 
	{
		return userRepository.findById(userId).map(user -> {

			if(user.isDeleted())
				throw new UserNotFoundByIdException("Invalid User Id");

			if(user.getUserRole().equals(UserRole.TEACHER) || user.getUserRole().equals(UserRole.STUDENT))
			{
				return academicProgramRepository.findById(programId).map(academicProgram -> {

					if(academicProgram.isDeleted())
						throw new AcademicProgramNotFoundByIdException("Invalid Program Id");

					if(academicProgram.getSubjects().isEmpty())
						throw new SubjectNotFoundInAcademicProgramException("First add subjects to the academic program");

					if(user.getUserRole().equals(UserRole.TEACHER))
					{
						if(user.getSubject()!= null)
						{

							if(academicProgram.getSubjects().contains(user.getSubject()))
							{
								academicProgram.getUsers().add(user);
								academicProgramRepository.save(academicProgram);
							}
							else
								throw new InvalidAcademicProgramAssignmentToTeacherException("Invalid Academic Program assigned to teacher, please assign a proper academic program which contains subject related to the teacher");

						}
						else
							throw new SubjectNotAssignedToTeacherException("Subject has not been assigned to the teacher, first assign a subject to the Teacher");

					}
					else
					{
						academicProgram.getUsers().add(user);
						academicProgramRepository.save(academicProgram);
					}
					return ResponseEntityProxy.getResponseEntity(HttpStatus.OK, "Academic Program assigned to the user successfully", mapToAcademicProgramResponse(academicProgram));

				}).orElseThrow(() -> new AcademicProgramNotFoundByIdException("Invalid Program Id"));
			}
			else {
				throw new InvalidUserRoleException("Only User with User Role Teacher or Student can be assigned to an Academic Program");
			}
		})
				.orElseThrow(() -> new UserNotFoundByIdException("Invalid User Id"));

	}

	@Override
	public ResponseEntity<ResponseStructure<List<UserResponse>>> findAllUserInAcademicProgram(int programId,String userRole)
	{
		try {
			UserRole role = UserRole.valueOf(userRole.toUpperCase());

			if(role.equals(UserRole.ADMIN))

				throw new InvalidUserRoleException("Academic programs can be associated to Teachers and Students only, Admins are not associated with any Academic Programs.");		

			AcademicProgram academicProgram = academicProgramRepository.findById(programId).orElseThrow(() -> new AcademicProgramNotFoundByIdException("Invalid Academic Program Id"));

			if(academicProgram.isDeleted())
				throw new AcademicProgramNotFoundByIdException("Invalid Program Id");

			List<User> users = userRepository.findByAcademicProgramsAndUserRole(academicProgram,role);	

			if(users.isEmpty())
				throw new UserNotFoundInAcademicProgramException("There are no users associated with the academic program "+academicProgram.getProgramId()+" with role "+role);

			else {
				List<UserResponse> userResponses = users.stream().map(userServiceImpl::mapToUserResponse).collect(Collectors.toList());
				return ResponseEntityProxy.getResponseEntity(HttpStatus.FOUND, "All the users fetched successfully",userResponses );
			}
		}catch (IllegalArgumentException | NullPointerException ex) {
			throw new InvalidUserRoleException("Invalid user Role, please provide User Role as Teacher or Student");
		}	
	}

	@Override
	public ResponseEntity<ResponseStructure<AcademicProgramResponse>> deleteAcademicProgram(int programId)
	{
		return academicProgramRepository.findById(programId).map(academicProgram -> {

			if(academicProgram.isDeleted())
				throw new AcademicProgramNotFoundByIdException("Invalid Academic Program");

			academicProgram.setDeleted(true);
			academicProgramRepository.save(academicProgram);
			return ResponseEntityProxy.getResponseEntity(HttpStatus.OK, "Academic Program deleted successfully", mapToAcademicProgramResponse(academicProgram));
		}).orElseThrow(() -> new AcademicProgramNotFoundByIdException("Invalid Academic Program"));
	}

}
