package com.school.sba.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.school.sba.entity.AcademicProgram;
import com.school.sba.entity.School;
import com.school.sba.enums.UserRole;
import com.school.sba.repository.AcademicProgramRepository;
import com.school.sba.repository.ClassHourRepository;
import com.school.sba.repository.SchoolRepository;
import com.school.sba.repository.UserRepository;

@Component
public class ScheduledJobs {
	
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private SchoolRepository schoolRepository;
	@Autowired
	private AcademicProgramRepository academicProgramRepository;
	@Autowired
	private ClassHourRepository classHourRepository;

	@Scheduled(fixedDelay = 1000l*60)
	public void test()
	{
		deleteUserIfDeleted();
		deleteSchoolIfDeleted();
		deleteAcademicProgramIfDeleted();
	}
	
	private void deleteUserIfDeleted()
	{
		userRepository.findAll().stream()
		.filter(user -> !user.getUserRole().equals(UserRole.ADMIN) && user.isDeleted())
		.forEach(user ->
		{
			user.getClassHours().forEach(classHour -> classHour.setUser(null));
			classHourRepository.saveAll(user.getClassHours());
			
			userRepository.delete(user);
		});
	}
	
	private void deleteSchoolIfDeleted()
	{
		schoolRepository.findAll().stream()
		.filter(School::isDeleted)
		.forEach(school ->
		{
			school.getUsers().forEach(user -> user.setSchool(null));
			userRepository.saveAll(school.getUsers());
			
			school.getAcademicPrograms().forEach(academicProgram -> academicProgram.setSchool(null));
			academicProgramRepository.saveAll(school.getAcademicPrograms());
			
			schoolRepository.delete(school);
		});
	}
	
	private void deleteAcademicProgramIfDeleted()
	{
		academicProgramRepository.findAll().stream()
		.filter(AcademicProgram::isDeleted)
		.forEach(academicProgram -> 
		{
			//Deleting All the Class Hours related to the Academic Program
			if(!academicProgram.getClassHours().isEmpty())
			classHourRepository.deleteAll(academicProgram.getClassHours());
			
			academicProgram.getUsers().forEach(user -> user.setAcademicPrograms(null));
			userRepository.saveAll(academicProgram.getUsers());
			
			academicProgramRepository.delete(academicProgram);
		});
	}
}
