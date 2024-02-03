package com.school.sba.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.school.sba.serviceImpl.AcademicProgramServiceImpl;
import com.school.sba.serviceImpl.ClassHourServiceImpl;
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
	@Autowired
	private ClassHourServiceImpl classHourServiceImpl;

	//@Scheduled(fixedDelay = 1000l)
	public void delete()
	{
		userServiceImpl.deleteUserIfDeleted();
		academicProgramServiceImpl.deleteAcademicProgramIfDeleted();
		schoolServiceImpl.deleteSchoolIfDeleted();
	}
	
	@Scheduled(cron = "0 0 7 * * MON")
	public void autoGenerateClassHour()
	{
		if(academicProgramServiceImpl.autoGenerateClassHour)
			classHourServiceImpl.generateClassHourForNextWeek();
	}
}
