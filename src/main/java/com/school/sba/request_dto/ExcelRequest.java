package com.school.sba.request_dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExcelRequest {

	private LocalDate fromDate;
	private LocalDate toDate;
	String folderPath;
}
