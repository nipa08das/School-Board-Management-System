package com.school.sba.serviceImpl;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.school.sba.entity.AcademicProgram;
import com.school.sba.entity.ClassHour;
import com.school.sba.entity.Schedule;
import com.school.sba.entity.School;
import com.school.sba.entity.User;
import com.school.sba.enums.ClassStatus;
import com.school.sba.enums.ProgramType;
import com.school.sba.enums.UserRole;
import com.school.sba.exception.AcademicProgramNotFoundByIdException;
import com.school.sba.exception.InvalidProgramTypeException;
import com.school.sba.exception.InvalidAcademicProgramAssignmentToTeacherException;
import com.school.sba.exception.InvalidUserRoleException;
import com.school.sba.exception.ScheduleNotFoundBySchoolIdException;
import com.school.sba.exception.SchoolNotFoundByIdException;
import com.school.sba.exception.SubjectNotAssignedToTeacherException;
import com.school.sba.exception.SubjectNotFoundInAcademicProgramException;
import com.school.sba.exception.UserNotFoundByIdException;
import com.school.sba.repository.AcademicProgramRepository;
import com.school.sba.repository.ClassHourRepository;
import com.school.sba.repository.SchoolRepository;
import com.school.sba.repository.UserRepository;
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
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ClassHourRepository classHourRepository;

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
	
	private boolean isBreakTime(LocalDateTime currentTime , Schedule schedule)
	{
		LocalTime breakTimeStart = schedule.getBreakTime();
		LocalTime breakTimeEnd = breakTimeStart.plusMinutes(schedule.getBreakLengthInMinutes().toMinutes());
		
		return (currentTime.toLocalTime().isAfter(breakTimeStart) && currentTime.toLocalTime().isBefore(breakTimeEnd));

	}
	
	private boolean isLunchTime(LocalDateTime currentTime , Schedule schedule)
	{
		LocalTime lunchTimeStart = schedule.getLunchTime();
		LocalTime lunchTimeEnd = lunchTimeStart.plusMinutes(schedule.getBreakLengthInMinutes().toMinutes());
		
		return (currentTime.toLocalTime().isAfter(lunchTimeStart) && currentTime.toLocalTime().isBefore(lunchTimeEnd));

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
				
				Schedule schedule = school.getSchedule();
				if(schedule!=null)
				{
					int classHourPerDay = schedule.getClassHoursPerDay();
					int classHourLength = (int) schedule.getClassHourLengthInMinutes().toMinutes();
					
					LocalDateTime currentTime = LocalDateTime.now().with(schedule.getOpensAt());
					
					for(int day = 0 ; day<7 ; day++)
					{
						for(int hour = 0;hour<classHourPerDay;hour++)
						{
							if(!isBreakTime(currentTime, schedule))
							{
								if(!isLunchTime(currentTime, schedule))
								{
									LocalDateTime beginsAt = currentTime;
									LocalDateTime endsAt = beginsAt.plusMinutes(classHourLength);
									
									ClassHour classHour = new ClassHour();
									classHour.setBeginsAt(beginsAt);
									classHour.setEndsAt(endsAt);
									classHour.setClassStatus(ClassStatus.NOT_SCHEDULED);
									classHourRepository.save(classHour);
									
									currentTime = endsAt;
								}
								else
									currentTime = currentTime.plusMinutes(schedule.getLunchLengthInMinutes().toMinutes());
								
							}
							else
								currentTime = currentTime.plusMinutes(schedule.getBreakLengthInMinutes().toMinutes());
					
						}
						currentTime = currentTime.plusDays(1).with(schedule.getOpensAt());
					}
					
					
					
				}
				else
					throw new ScheduleNotFoundBySchoolIdException("The school does not contain any schedule, please provide a schedule to the school");
				
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
		School school = schoolRepository.findById(schoolId).orElseThrow(() -> new SchoolNotFoundByIdException("Invalid School Id"));
		List<AcademicProgram> academicPrograms = academicProgramRepository.findBySchoolSchoolId(school.getSchoolId());
		if (academicPrograms.isEmpty()) {
			return ResponseEntityProxy.getResponseEntity(HttpStatus.NOT_FOUND, "No Academic Program details found", null);
		} else {
			List<AcademicProgramResponse> academicProgramsResponse = academicPrograms.stream().map(this::mapToAcademicProgramResponse).collect(Collectors.toList());
			return ResponseEntityProxy.getResponseEntity(HttpStatus.FOUND, "All Academic Program details found successfully", academicProgramsResponse);
		}
	}

	@Override
	public ResponseEntity<ResponseStructure<AcademicProgramResponse>> assignAcademicProgramToUser(int programId,int userId) 
	{
		User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundByIdException("Invalid User Id"));
		if(user.getUserRole().equals(UserRole.TEACHER) || user.getUserRole().equals(UserRole.STUDENT))
		{
			return academicProgramRepository.findById(programId).map(academicProgram -> {

				if(academicProgram.getSubjects().isEmpty())

					throw new SubjectNotFoundInAcademicProgramException("First add subjects to the academic program");

				if(user.getUserRole().equals(UserRole.TEACHER))
				{
					if(user.getSubject()!= null)
					{

						if(academicProgram.getSubjects().contains(user.getSubject()))
						{
							user.getAcademicPrograms().add(academicProgram);
							userRepository.save(user);
						}
						else

							throw new InvalidAcademicProgramAssignmentToTeacherException("Invalid Academic Program assigned to teacher, please assign a proper academic program which contains subject related to the teacher");

					}
					else

						throw new SubjectNotAssignedToTeacherException("Subject has not been assigned to the teacher, first assign a subject to the Teacher");

				}
				else
				{
					user.getAcademicPrograms().add(academicProgram);
					userRepository.save(user);
				}
				return ResponseEntityProxy.getResponseEntity(HttpStatus.OK, "Academic Program assigned to the user successfully", mapToAcademicProgramResponse(academicProgram));

			}).orElseThrow(() -> new AcademicProgramNotFoundByIdException("Invalid Program Id"));
		}
		else {
			throw new InvalidUserRoleException("Only User with User Role Teacher or Student can be assigned to an Academic Program");
		}
	}

}
