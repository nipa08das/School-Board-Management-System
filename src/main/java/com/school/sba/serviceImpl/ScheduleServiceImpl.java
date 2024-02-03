package com.school.sba.serviceImpl;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.school.sba.entity.Schedule;
import com.school.sba.exception.InvalidScheduleTimingsException;
import com.school.sba.exception.ScheduleExceededException;
import com.school.sba.exception.ScheduleNotFoundByIdException;
import com.school.sba.exception.ScheduleNotFoundBySchoolIdException;
import com.school.sba.exception.SchoolNotFoundByIdException;
import com.school.sba.repository.ScheduleRepository;
import com.school.sba.repository.SchoolRepository;
import com.school.sba.request_dto.ScheduleRequest;
import com.school.sba.response_dto.ScheduleResponse;
import com.school.sba.service.ScheduleService;
import com.school.sba.util.ResponseEntityProxy;
import com.school.sba.util.ResponseStructure;

import jakarta.validation.Valid;

@Service
public class ScheduleServiceImpl implements ScheduleService{
	@Autowired
	private ScheduleRepository scheduleRepository;
	@Autowired
	private SchoolRepository schoolRepository;

	//Mapper Methods
	private Schedule mapToSchedule(@Valid ScheduleRequest scheduleRequest)
	{
		return Schedule.builder()
				.opensAt(scheduleRequest.getOpensAt())
				.closesAt(scheduleRequest.getClosesAt())
				.classHoursPerDay(scheduleRequest.getClassHoursPerDay())
				.classHourLengthInMinutes(Duration.ofMinutes(scheduleRequest.getClassHourLengthInMinutes()))
				.breakTime(scheduleRequest.getBreakTime())
				.breakLengthInMinutes(Duration.ofMinutes(scheduleRequest.getBreakLengthInMinutes()))
				.lunchTime(scheduleRequest.getLunchTime())
				.lunchLengthInMinutes(Duration.ofMinutes(scheduleRequest.getLunchLengthInMinutes()))
				.build();
	}

	private ScheduleResponse mapToScheduleResponse(Schedule schedule)
	{
		return ScheduleResponse.builder()
				.scheduleId(schedule.getScheduleId())
				.opensAt(schedule.getOpensAt())
				.closesAt(schedule.getClosesAt())
				.classHoursPerDay(schedule.getClassHoursPerDay())
				.classHourLengthInMinutes((int)schedule.getClassHourLengthInMinutes().toMinutes())
				.breakTime(schedule.getBreakTime())
				.breakLengthInMinutes((int)schedule.getBreakLengthInMinutes().toMinutes())
				.lunchTime(schedule.getLunchTime())
				.lunchLengthInMinutes((int) schedule.getLunchLengthInMinutes().toMinutes())
				.build();
	}

	@Override
	public ResponseEntity<ResponseStructure<ScheduleResponse>> saveSchedule(@Valid ScheduleRequest scheduleRequest, int schoolId) {
		return schoolRepository.findById(schoolId)
				.map(school -> {
					if(school.isDeleted())
						throw new SchoolNotFoundByIdException("Invalid School Id");

					if(school.getSchedule() == null)
					{
						validateScheduleTimings(scheduleRequest);

						Schedule schedule = mapToSchedule(scheduleRequest);
						schedule.setSchool(school);
						schedule = scheduleRepository.save(schedule);
						return ResponseEntityProxy.getResponseEntity(HttpStatus.CREATED, "Scheduled created for School", mapToScheduleResponse(schedule));
					}
					else
					{
						throw new ScheduleExceededException("You can't have more than one schedule for a school");
					}      
				})
				.orElseThrow(() -> new SchoolNotFoundByIdException("Invalid School Id"));
	}

	private void validateScheduleTimings(ScheduleRequest scheduleRequest) 
	{
		LocalTime opensAt = scheduleRequest.getOpensAt();
		LocalTime closesAt = scheduleRequest.getClosesAt();
		int classHoursPerDay = scheduleRequest.getClassHoursPerDay();
		int classHourLengthInMinutes = scheduleRequest.getClassHourLengthInMinutes();
		int breakLengthInMinutes = scheduleRequest.getBreakLengthInMinutes();
		int lunchLengthInMinutes = scheduleRequest.getLunchLengthInMinutes();
		LocalTime breakTime = scheduleRequest.getBreakTime();
		LocalTime lunchTime = scheduleRequest.getLunchTime();

		if(ChronoUnit.MINUTES.between(opensAt, closesAt)!=(classHoursPerDay*classHourLengthInMinutes+breakLengthInMinutes+lunchLengthInMinutes))
		{
			throw new InvalidScheduleTimingsException("The opensAt time and the closesAt time does not matches with your given duration");
		}

		LocalTime beginsAt = opensAt;
		LocalTime endsAt = beginsAt.plusMinutes(classHourLengthInMinutes);

		for(int hour = 1 ; hour <= classHoursPerDay+2 ; hour++)
		{	
			if(breakTime.isAfter(beginsAt) && breakTime.isBefore(endsAt) || breakTime.equals(beginsAt))
			{
				if(breakTime.isAfter(beginsAt) && breakTime.isBefore(endsAt))
					throw new InvalidScheduleTimingsException("Break Time should be exactly at the end time of a class");
				beginsAt = breakTime;
				endsAt = breakTime.plusMinutes(breakLengthInMinutes);
			}
			else if(lunchTime.isAfter(beginsAt) && lunchTime.isBefore(endsAt) || lunchTime.equals(beginsAt))
			{
				if(lunchTime.isAfter(beginsAt) && lunchTime.isBefore(endsAt))
					throw new InvalidScheduleTimingsException("Lunch Time should be exactly at the end time of a class");
				beginsAt = lunchTime;
				endsAt = lunchTime.plusMinutes(lunchLengthInMinutes);
			}

			if(!breakTime.equals(beginsAt) || !lunchTime.equals(beginsAt))
			{
				beginsAt = endsAt;
				endsAt = beginsAt.plusMinutes(classHourLengthInMinutes);
			}	
		}
	}

	@Override
	public ResponseEntity<ResponseStructure<ScheduleResponse>> findScheduleBySchoolId(int schoolId) 
	{
		return schoolRepository.findById(schoolId)
				.map(school -> {
					if(school.isDeleted())
						throw new SchoolNotFoundByIdException("Invalid School Id");

					Schedule schedule = school.getSchedule();
					if(schedule != null)
					{
						return ResponseEntityProxy.getResponseEntity(HttpStatus.FOUND, "Schedule found successfully", mapToScheduleResponse(schedule));
					}
					else
					{
						throw new ScheduleNotFoundBySchoolIdException("Schedule is not present for the given school Id");
					}

				}).orElseThrow(() -> new SchoolNotFoundByIdException("Invalid School Id"));
	}

	@Override
	public ResponseEntity<ResponseStructure<ScheduleResponse>> updateSchedule(@Valid ScheduleRequest scheduleRequest,int scheduleId) 
	{
		return scheduleRepository.findById(scheduleId)
				.map(s ->{
					Schedule updatedSchedule = mapToSchedule(scheduleRequest);
					updatedSchedule.setScheduleId(s.getScheduleId());
					Schedule schedule = scheduleRepository.save(updatedSchedule);
					return ResponseEntityProxy.getResponseEntity(HttpStatus.FOUND, "Schedule updated successfully", mapToScheduleResponse(schedule));

				}).orElseThrow(() -> new ScheduleNotFoundByIdException("Invalid Schedule Id"));
	}	
}
