package com.school.sba.serviceImpl;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.school.sba.entity.AcademicProgram;
import com.school.sba.entity.School;
import com.school.sba.enums.ProgramType;
import com.school.sba.exception.InvalidProgramTypeException;
import com.school.sba.exception.SchoolNotFoundByIdException;
import com.school.sba.repository.AcademicProgramRepository;
import com.school.sba.repository.SchoolRepository;
import com.school.sba.request_dto.AcademicProgramRequest;
import com.school.sba.response_dto.AcademicProgramResponse;
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

	//Mapper Methods
	private AcademicProgram mapToAcademicProgram(@Valid AcademicProgramRequest academicProgramRequest)
	{
		return AcademicProgram.builder()
				.programType(ProgramType.valueOf(academicProgramRequest.getProgramType().toUpperCase()))
				.programName(academicProgramRequest.getProgramName())
				.beginsAt(academicProgramRequest.getBeginsAt())
				.endsAt(academicProgramRequest.getEndsAt())
				.build();
	}
	
	private AcademicProgramResponse mapToAcademicProgramResponse(@Valid AcademicProgram academicProgram)
	{
		return AcademicProgramResponse.builder()
				.programId(academicProgram.getProgramId())
				.programType(academicProgram.getProgramType())
				.programName(academicProgram.getProgramName())
				.beginsAt(academicProgram.getBeginsAt())
				.endsAt(academicProgram.getEndsAt())
				.build();
	}

	@Override
	public ResponseEntity<ResponseStructure<AcademicProgramResponse>> saveAcademicProgram(
			@Valid AcademicProgramRequest academicProgramRequest,int schoolId) 
	{
		try {
		ProgramType.valueOf(academicProgramRequest.getProgramType().toUpperCase());
		return schoolRepository.findById(schoolId)
				.map(school -> {
					AcademicProgram academicProgram = mapToAcademicProgram(academicProgramRequest);
					academicProgram.setSchool(school);
					academicProgram = academicProgramRepository.save(academicProgram);
					return ResponseEntityProxy.getResponseEntity(HttpStatus.CREATED, "Academic Program created successfully", mapToAcademicProgramResponse(academicProgram));
					
				}).orElseThrow(() -> new SchoolNotFoundByIdException("Invalid School Id"));
		}
		catch (IllegalArgumentException | NullPointerException ex) {
			throw new InvalidProgramTypeException("Invalid Program Type");
		}
	}

	@Override
	public ResponseEntity<ResponseStructure<List<AcademicProgramResponse>>> findAllAcademicProgram(int schoolId) {
		School school = schoolRepository.findById(schoolId).orElseThrow(() -> new SchoolNotFoundByIdException("Invalid School Id"));
		List<AcademicProgram> academicPrograms = academicProgramRepository.findBySchoolSchoolId(school.getSchoolId());
		if (academicPrograms.isEmpty()) {
            return ResponseEntityProxy.getResponseEntity(HttpStatus.NOT_FOUND, "No Academic Program details found", null);
        } else {
            List<AcademicProgramResponse> academicProgramsResponse = academicPrograms.stream().map(this::mapToAcademicProgramResponse).collect(Collectors.toList());
            return ResponseEntityProxy.getResponseEntity(HttpStatus.FOUND, "All Academic Program details found successfully", academicProgramsResponse);
        }
	}
	
}
