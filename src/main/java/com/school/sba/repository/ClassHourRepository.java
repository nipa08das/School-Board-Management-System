package com.school.sba.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.school.sba.entity.AcademicProgram;
import com.school.sba.entity.ClassHour;

public interface ClassHourRepository extends JpaRepository<ClassHour, Integer>{

	
	boolean existsByRoomNoAndBeginsAtBetween(int roomNo, LocalDateTime beginsAt, LocalDateTime endsAt);

	boolean existsByAcademicProgramAndBeginsAtBetween(AcademicProgram academicProgram, LocalDateTime beginsAt, LocalDateTime plusDays);
	
	List<ClassHour> findByBeginsAtBetween(LocalDateTime beginsAt, LocalDateTime plusDays);

	@Query("select max(ch.endsAt) from ClassHour ch where ch.academicProgram = :academicProgram")
	LocalDateTime findMaximumEndsAt(AcademicProgram academicProgram);

	List<ClassHour> findByAcademicProgramAndBeginsAtBetween(AcademicProgram academicProgram, LocalDateTime beginTime,
			LocalDateTime endTime);

}
