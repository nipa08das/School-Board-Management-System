package com.school.sba.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.school.sba.request_dto.ClassHourRequest;
import com.school.sba.request_dto.ExcelRequest;
import com.school.sba.util.ResponseStructure;

public interface ClassHourService {

	ResponseEntity<ResponseStructure<String>> generateClassHourForAcademicProgram(int programId);

	ResponseEntity<ResponseStructure<String>> updateClassHour(List<ClassHourRequest> classHourRequests);

	ResponseEntity<ResponseStructure<String>> generateClassHourForNextWeek();

	ResponseEntity<String> generateClassHoursInExcel(ExcelRequest excelRequest, int programId);

	ResponseEntity<?> writeIntoUserExcel(MultipartFile file, int programId, LocalDate fromDate, LocalDate toDate);

}
