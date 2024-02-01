package com.school.sba.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.school.sba.serviceImpl.AcademicProgramServiceImpl;
import com.school.sba.serviceImpl.SchoolServiceImpl;
import com.school.sba.serviceImpl.UserServiceImpl;

@Component
public class ScheduledJobs {
	
	@Autowired
	private UserServiceImpl userServiceImpl;
	@Autowired
	private AcademicProgramServiceImpl academicProgramServiceImpl;
	@Autowired
	private SchoolServiceImpl schoolServiceImpl;

	@Scheduled(fixedDelay = 1000l*60)
	public void test()
	{
		userServiceImpl.deleteUserIfDeleted();
		academicProgramServiceImpl.deleteAcademicProgramIfDeleted();
		schoolServiceImpl.deleteSchoolIfDeleted();
	}
	
	
	
}
