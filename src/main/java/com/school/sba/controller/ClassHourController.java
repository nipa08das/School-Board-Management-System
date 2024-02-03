package com.school.sba.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.school.sba.request_dto.ClassHourRequest;
import com.school.sba.request_dto.ExcelRequest;
import com.school.sba.service.ClassHourService;
import com.school.sba.util.ResponseStructure;

@RestController
public class ClassHourController {
	
	@Autowired
	private ClassHourService classHourService;

	@PostMapping("/academic-program/{programId}/class-hours")
	public ResponseEntity<ResponseStructure<String>> generateClassHourForAcademicProgram(@PathVariable int programId)
	{
		return classHourService.generateClassHourForAcademicProgram(programId);
	}
	
	@PutMapping("/class-hours")
	public ResponseEntity<ResponseStructure<String>> updateClassHour(@RequestBody List<ClassHourRequest> classHourRequests)
	{
		return classHourService.updateClassHour(classHourRequests);
	}
	
	@PostMapping("/class-hours/auto-generate")
	public ResponseEntity<ResponseStructure<String>> generateClassHourForNextWeek()
	{
		return classHourService.generateClassHourForNextWeek();
	}
	
	@PostMapping("/academic-programs/{programId}/class-hours/write-excel")
	public ResponseEntity<String> generateClassHoursInExcel(@RequestBody ExcelRequest excelRequest, @PathVariable int programId)
	{
		return classHourService.generateClassHoursInExcel(excelRequest, programId);	
	}
	
	@PostMapping("/academic-programs/{programId}/class-hours/from/{fromDate}/to/{toDate}/write-excel")
	public ResponseEntity<?> writeIntoUserExcel(@RequestParam MultipartFile file, @PathVariable int programId, @PathVariable LocalDate fromDate, @PathVariable LocalDate toDate)
	{
		return classHourService.writeIntoUserExcel(file, programId, fromDate, toDate);
	}

}
