package com.school.sba.serviceImpl;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.school.sba.entity.AcademicProgram;
import com.school.sba.entity.ClassHour;
import com.school.sba.entity.Schedule;
import com.school.sba.entity.School;
import com.school.sba.entity.User;
import com.school.sba.enums.ClassStatus;
import com.school.sba.enums.UserRole;
import com.school.sba.exception.AcademicProgramNotFoundByIdException;
import com.school.sba.exception.ClassHourAlreadyExistException;
import com.school.sba.exception.ClassHourNotFoundByIdException;
import com.school.sba.exception.ClassHourNotFoundException;
import com.school.sba.exception.DuplicateClassHourException;
import com.school.sba.exception.InvalidAacdemicProgramException;
import com.school.sba.exception.InvalidUserRoleException;
import com.school.sba.exception.ScheduleNotFoundBySchoolIdException;
import com.school.sba.exception.UserNotFoundByIdException;
import com.school.sba.repository.AcademicProgramRepository;
import com.school.sba.repository.ClassHourRepository;
import com.school.sba.repository.UserRepository;
import com.school.sba.request_dto.ClassHourRequest;
import com.school.sba.request_dto.ExcelRequest;
import com.school.sba.service.ClassHourService;
import com.school.sba.util.ResponseEntityProxy;
import com.school.sba.util.ResponseStructure;

@Service
public class ClassHourServiceImpl implements ClassHourService {

	@Autowired
	private AcademicProgramRepository academicProgramRepository;
	@Autowired
	private ClassHourRepository classHourRepository;
	@Autowired
	private UserRepository userRepository;

	private boolean isBreakTime(LocalDateTime beginsAt, LocalDateTime endsAt, Schedule schedule)
	{
		LocalTime breakTimeStart = schedule.getBreakTime();

		return ((breakTimeStart.isAfter(beginsAt.toLocalTime()) && breakTimeStart.isBefore(endsAt.toLocalTime())) || breakTimeStart.equals(beginsAt.toLocalTime()));
	}

	private boolean isLunchTime(LocalDateTime beginsAt, LocalDateTime endsAt , Schedule schedule)
	{
		LocalTime lunchTimeStart = schedule.getLunchTime();

		return ((lunchTimeStart.isAfter(beginsAt.toLocalTime()) && lunchTimeStart.isBefore(endsAt.toLocalTime())) || lunchTimeStart.equals(beginsAt.toLocalTime()));
	}

	public void changeClassStatus()
	{

	}

	public void generateClassHourForTheWeek(Schedule schedule, AcademicProgram academicProgarm)
	{
		LocalDateTime currentTime = LocalDateTime.now().with(schedule.getOpensAt());
		int classHourPerDay = schedule.getClassHoursPerDay();
		int classHourLength = (int) schedule.getClassHourLengthInMinutes().toMinutes();

		int lastDay = 7;
		int currentDay = LocalDate.now().getDayOfWeek().getValue();
		int sunday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY)).getDayOfWeek().getValue();
		if(!LocalDate.now().getDayOfWeek().equals(DayOfWeek.MONDAY))
			lastDay = sunday - currentDay + lastDay;

		boolean existsForThisWeek = classHourRepository.existsByAcademicProgramAndBeginsAtBetween(academicProgarm, currentTime, currentTime.plusDays(lastDay));
		if(existsForThisWeek)
			throw new ClassHourAlreadyExistException("Class Hour Already generated for the Academic Program between "+currentTime+" to "+currentTime.plusDays(lastDay-1).with(schedule.getClosesAt()));


		LocalTime lunchTimeStart = schedule.getLunchTime();
		LocalTime lunchTimeEnd = lunchTimeStart.plusMinutes(schedule.getLunchLengthInMinutes().toMinutes());
		LocalTime breakTimeStart = schedule.getBreakTime();
		LocalTime breakTimeEnd = breakTimeStart.plusMinutes(schedule.getBreakLengthInMinutes().toMinutes());

		List<ClassHour> classHourToSave = new ArrayList<ClassHour>();

		for(int day = 1 ; day <= lastDay ; day++)
		{
			if(!currentTime.getDayOfWeek().equals(DayOfWeek.SUNDAY))
			{
				for(int hour = 1 ; hour <= classHourPerDay+2 ; hour++)
				{
					ClassHour classHour = new ClassHour();
					LocalDateTime beginsAt = currentTime;
					LocalDateTime endsAt = beginsAt.plusMinutes(classHourLength);

					if(!isLunchTime(beginsAt, endsAt, schedule))
					{
						if(!isBreakTime(beginsAt, endsAt, schedule))
						{
							classHour.setBeginsAt(beginsAt);
							classHour.setEndsAt(endsAt);
							classHour.setClassStatus(ClassStatus.NOT_SCHEDULED);

							currentTime = endsAt;
						}
						else
						{
							classHour.setBeginsAt(currentTime.with(breakTimeStart));
							classHour.setEndsAt(currentTime.with(breakTimeEnd));
							classHour.setClassStatus(ClassStatus.BREAK_TIME);
							currentTime = currentTime.with(breakTimeEnd);
						}
					}
					else
					{
						classHour.setBeginsAt(currentTime.with(lunchTimeStart));
						classHour.setEndsAt(currentTime.with(lunchTimeEnd));
						classHour.setClassStatus(ClassStatus.LUNCH_TIME);
						currentTime = currentTime.with(lunchTimeEnd);
					}
					classHour.setAcademicProgram(academicProgarm);
					classHourToSave.add(classHour);
				}
				currentTime = currentTime.plusDays(1).with(schedule.getOpensAt());
			}
			else
			{
				currentTime = currentTime.plusDays(1).with(schedule.getOpensAt());
			}
		}
		classHourRepository.saveAll(classHourToSave);
	}

	@Override
	public ResponseEntity<ResponseStructure<String>> generateClassHourForAcademicProgram(int programId) 
	{
		return academicProgramRepository.findById(programId)
				.map(academicProgarm -> {
					if(academicProgarm.isDeleted())
						throw new AcademicProgramNotFoundByIdException("Invalid Program Id");

					School school = academicProgarm.getSchool();
					Schedule schedule = school.getSchedule();
					if(schedule!=null)
						generateClassHourForTheWeek(schedule, academicProgarm);
					else
						throw new ScheduleNotFoundBySchoolIdException("The school does not contain any schedule, please provide a schedule to the school");

					return ResponseEntityProxy.getResponseEntity(HttpStatus.CREATED, "Class Hour generated successfully for the academic progarm","Class Hour generated for the current week successfully");
				})
				.orElseThrow(() -> new AcademicProgramNotFoundByIdException("Invalid Program Id"));
	}

	@Override
	public ResponseEntity<ResponseStructure<String>> updateClassHour(List<ClassHourRequest> classHourRequests) 
	{
		for(ClassHourRequest classHourRequest : classHourRequests)
		{
			ClassHour classHour = classHourRepository.findById(classHourRequest.getClassHourId())
					.orElseThrow(() -> new ClassHourNotFoundByIdException("Invalid Class Hour"));

			User user = userRepository.findById(classHourRequest.getUserId()).orElseThrow(() -> new UserNotFoundByIdException("Invalid User Id"));

			if(user.getUserRole().equals(UserRole.TEACHER))
			{
				if(user.getAcademicPrograms().contains(classHour.getAcademicProgram()))
				{
					LocalDateTime beginsAt = classHour.getBeginsAt();
					LocalDateTime endsAt = classHour.getEndsAt();
					int roomNo = classHourRequest.getRoomNo();

					boolean isExist = classHourRepository.existsByRoomNoAndBeginsAtBetween( roomNo, beginsAt, endsAt);

					if(!isExist)
					{
						classHour.setSubject(user.getSubject());
						classHour.setUser(user);
						classHour.setRoomNo(roomNo);
						classHourRepository.save(classHour);
					}
					else 
						throw new DuplicateClassHourException("Another Class Hour already allotted for the same date and time in the given room.");
				}else
					throw new InvalidAacdemicProgramException("The user's Academic Program is not same as the Class Hour's Academic Program.");
			}
			else 
				throw new InvalidUserRoleException("Only Teachers can be alloted to a Class Hour");
		}
		return ResponseEntityProxy.getResponseEntity(HttpStatus.OK, "Class Hour updated successfully.", "Subject, Teacher and Room No assinged to a Class Hour");
	}

	@Override
	public ResponseEntity<ResponseStructure<String>> generateClassHourForNextWeek() 
	{
		List<AcademicProgram> academicPrograms = academicProgramRepository.findAll();
		if(academicPrograms.isEmpty())
			throw new AcademicProgramNotFoundByIdException("No Academic Programs found To auto-generate the Class Hour for the next week");

		List<ClassHour> classHourToGenerateForNextWeek = new ArrayList<ClassHour>();

		for(AcademicProgram academicProgram : academicPrograms)
		{
			Schedule schedule = academicProgram.getSchool().getSchedule();
			if(schedule == null)
				throw new ScheduleNotFoundBySchoolIdException("The school does not contain any schedule, please provide a schedule to the school");

			LocalTime opensAt = schedule.getOpensAt();
			int classHourLength = (int) schedule.getClassHourLengthInMinutes().toMinutes();
			int classHourPerDay = schedule.getClassHoursPerDay();	

			LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
			LocalDateTime nextMondayDateTime = LocalDateTime.of(nextMonday, opensAt);

			boolean classHourforNextWeekExist = classHourRepository.existsByAcademicProgramAndBeginsAtBetween(academicProgram, nextMondayDateTime, nextMondayDateTime.plusDays(5));
			if(classHourforNextWeekExist)
			{
				throw new ClassHourAlreadyExistException("Class Hour Already generated for the next week ("+nextMondayDateTime+" to "+nextMondayDateTime.plusDays(5).with(schedule.getClosesAt())+")");
			}

			if(LocalDate.now().getDayOfWeek().equals(DayOfWeek.MONDAY))
			{
				LocalDateTime todaysDateTime = LocalDateTime.of(LocalDate.now(), opensAt);
				boolean classHourforThisWeekExist = classHourRepository.existsByAcademicProgramAndBeginsAtBetween(academicProgram, todaysDateTime, todaysDateTime.plusDays(5));
				if(!classHourforThisWeekExist)
				{
					nextMondayDateTime = todaysDateTime;
				}
			}

			LocalDateTime lastClassHourTime = classHourRepository.findMaximumEndsAt(academicProgram);
			if(lastClassHourTime == null)
				throw new ClassHourNotFoundException("No Class Hour found to auto-generate the Class Hour for the next week");

			LocalTime lunchTimeStart = schedule.getLunchTime();
			LocalTime lunchTimeEnd = lunchTimeStart.plusMinutes(schedule.getLunchLengthInMinutes().toMinutes());
			LocalTime breakTimeStart = schedule.getBreakTime();
			LocalTime breakTimeEnd = breakTimeStart.plusMinutes(schedule.getBreakLengthInMinutes().toMinutes());

			List<ClassHour> classHours = classHourRepository.findByBeginsAtBetween(lastClassHourTime.minusDays(6).with(opensAt), lastClassHourTime);

			for(ClassHour classHour : classHours)
			{
				for(int day = 1; day <= 6; day++)
				{
					for(int hours = 1; hours < classHourPerDay+2; hours++)
					{
						LocalDateTime beginsAt = nextMondayDateTime;
						LocalDateTime endsAt = beginsAt.plusMinutes(classHourLength);
						ClassHour newClassHour = new ClassHour();

						if(!isLunchTime(beginsAt, endsAt, schedule))
						{
							if(!isBreakTime(beginsAt, endsAt, schedule))
							{
								newClassHour.setBeginsAt(beginsAt);
								newClassHour.setEndsAt(endsAt);
								newClassHour.setClassStatus(ClassStatus.NOT_SCHEDULED);
								newClassHour.setRoomNo(classHour.getRoomNo());
								newClassHour.setSubject(classHour.getSubject());
								newClassHour.setUser(classHour.getUser());

								nextMondayDateTime = endsAt;
							}
							else
							{
								newClassHour.setBeginsAt(beginsAt.with(breakTimeStart));
								newClassHour.setEndsAt(beginsAt.with(breakTimeEnd));
								newClassHour.setClassStatus(ClassStatus.BREAK_TIME);
								nextMondayDateTime = beginsAt.with(breakTimeEnd);
							}
						}
						else
						{
							newClassHour.setBeginsAt(beginsAt.with(lunchTimeStart));
							newClassHour.setEndsAt(beginsAt.with(lunchTimeEnd));
							newClassHour.setClassStatus(ClassStatus.BREAK_TIME);
							nextMondayDateTime = beginsAt.with(lunchTimeEnd);
						}
						newClassHour.setAcademicProgram(classHour.getAcademicProgram());
						classHourToGenerateForNextWeek.add(newClassHour);
					}
					nextMondayDateTime = nextMondayDateTime.plusDays(1).with(opensAt);						
				}
			}
		}

		classHourRepository.saveAll(classHourToGenerateForNextWeek);
		return ResponseEntityProxy.getResponseEntity(HttpStatus.CREATED, "Class Hour generated successfully","Class Hour generated for the next week (Monday - Saturday) successfully");
	}

	@Override
	public ResponseEntity<String> generateClassHoursInExcel(ExcelRequest excelRequest, int programId)
	{
		AcademicProgram academicProgram = academicProgramRepository.findById(programId)
				.orElseThrow(() -> new AcademicProgramNotFoundByIdException("Invalid Program Id"));

		LocalDateTime beginTimeDate = excelRequest.getFromDate().atTime(LocalTime.MIDNIGHT);
		LocalDateTime endTimeDate = excelRequest.getToDate().atTime(6, 0);

		List<ClassHour> classHours = classHourRepository.findByAcademicProgramAndBeginsAtBetween(academicProgram, beginTimeDate, endTimeDate);

		XSSFWorkbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet();

		int rowNumber = 0;
		Row header = sheet.createRow(rowNumber);
		header.createCell(0).setCellValue("Date");
		header.createCell(1).setCellValue("Begin Time");
		header.createCell(2).setCellValue("End Time");
		header.createCell(3).setCellValue("Room No");
		header.createCell(4).setCellValue("Subject");
		header.createCell(5).setCellValue("Teacher");
		header.createCell(6).setCellValue("Standard");
		header.createCell(7).setCellValue("Class Status");

		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm");
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-mm-dd");

		for(ClassHour classHour : classHours) 
		{			
			Row row = sheet.createRow(++rowNumber);

			row.createCell(0).setCellValue(dateFormatter.format(classHour.getBeginsAt()));
			row.createCell(1).setCellValue(timeFormatter.format(classHour.getBeginsAt()));
			row.createCell(2).setCellValue(timeFormatter.format(classHour.getEndsAt()));
			row.createCell(3).setCellValue(classHour.getRoomNo());
			if(classHour.getSubject()!=null)
				row.createCell(4).setCellValue(classHour.getSubject().getSubjectName());
			if(classHour.getUser()!=null)
				row.createCell(5).setCellValue(classHour.getUser().getUsername());
			if(classHour.getAcademicProgram()!=null)
				row.createCell(6).setCellValue(classHour.getAcademicProgram().getProgramName());
			row.createCell(7).setCellValue(classHour.getClassStatus().toString());
		}

		String filePath = excelRequest.getFolderPath()+"//test.xlsx";

		try {
			workbook.write(new FileOutputStream(filePath));
			workbook.close();
		} catch (FileNotFoundException e) {

		} catch (IOException e) {

		}

		return new ResponseEntity<String>("Excel sheet created successfully", HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<?> writeIntoUserExcel(MultipartFile file, int programId, LocalDate fromDate, LocalDate toDate)
	{
		AcademicProgram academicProgram = academicProgramRepository.findById(programId)
				.orElseThrow(() -> new AcademicProgramNotFoundByIdException("Invalid Program Id"));

		LocalDateTime beginTimeDate = fromDate.atTime(LocalTime.MIDNIGHT);
		LocalDateTime endTimeDate = toDate.atTime(6, 0);

		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm");
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-mm-dd");

		List<ClassHour> classHours = classHourRepository.findByAcademicProgramAndBeginsAtBetween(academicProgram, beginTimeDate, endTimeDate);

		XSSFWorkbook workbook = null;
		try {
			workbook = new XSSFWorkbook(file.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}

		workbook.forEach(sheet -> {
			int rowNumber = 0;
			Row header = sheet.createRow(rowNumber);
			header.createCell(0).setCellValue("Date");
			header.createCell(1).setCellValue("Begin Time");
			header.createCell(2).setCellValue("End Time");
			header.createCell(3).setCellValue("Room No");
			header.createCell(4).setCellValue("Subject");
			header.createCell(5).setCellValue("Teacher");
			header.createCell(6).setCellValue("Standard");
			header.createCell(7).setCellValue("Class Status");

			for(ClassHour classHour : classHours) 
			{			
				Row row = sheet.createRow(++rowNumber);

				row.createCell(0).setCellValue(dateFormatter.format(classHour.getBeginsAt()));
				row.createCell(1).setCellValue(timeFormatter.format(classHour.getBeginsAt()));
				row.createCell(2).setCellValue(timeFormatter.format(classHour.getEndsAt()));
				row.createCell(3).setCellValue(classHour.getRoomNo());
				if(classHour.getSubject()!=null)
					row.createCell(4).setCellValue(classHour.getSubject().getSubjectName());
				if(classHour.getUser()!=null)
					row.createCell(5).setCellValue(classHour.getUser().getUsername());
				if(classHour.getAcademicProgram()!=null)
					row.createCell(6).setCellValue(classHour.getAcademicProgram().getProgramName());
				row.createCell(7).setCellValue(classHour.getClassStatus().toString());
			}
		});
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			workbook.write(outputStream);
			workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		byte[] byteData = outputStream.toByteArray();

		return ResponseEntity.ok()
				.header("Content Disposition", "attachment filename "+file.getOriginalFilename())
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(byteData);
	}
}
