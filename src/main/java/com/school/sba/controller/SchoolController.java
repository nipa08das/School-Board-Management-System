package com.school.sba.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.school.sba.entity.School;
import com.school.sba.service.SchoolService;

@RestController
public class SchoolController {
	@Autowired
	private SchoolService schoolService;
	
	@PostMapping(path = "/save-school")
	public ResponseEntity<School> saveSchool(@RequestBody School school)
	{
		return schoolService.saveSchool(school);
	}
	
	@GetMapping(path = "/get-school/{schoolId}")
	public ResponseEntity<School> getSchoolById(@PathVariable int schoolId)
	{
		return schoolService.getSchoolById(schoolId);
	}
	
	@DeleteMapping(path = "/delete-school/{schoolId}")
	public ResponseEntity<String> deleteSchool(@PathVariable int schoolId)
	{
		return schoolService.deleteSchool(schoolId);
	}
	
	@PutMapping(path = "/update-school/{schoolId}")
	public ResponseEntity<School> updateSchool(@PathVariable int schoolId,@RequestBody School updatedSchool)
	{
		return schoolService.updateSchool(schoolId,updatedSchool);
	}
	
}
