package com.school.sba.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.school.sba.entity.School;
import com.school.sba.repository.SchoolRepository;

@Service
public class SchoolService {
	@Autowired
	private SchoolRepository schoolRepository;
	
	public ResponseEntity<School> saveSchool(School school)
	{
		School school2 = schoolRepository.save(school);
		return new ResponseEntity<School>(school2,HttpStatus.CREATED);		
	}

	public ResponseEntity<School> getSchoolById(int schoolId)
	{
		Optional<School> optional = schoolRepository.findById(schoolId);
		if(optional.isPresent())
		{
			School school = optional.get();
			return new ResponseEntity<School>(school,HttpStatus.FOUND);
		}
		else
		{
			return null;
		}
	}

	public ResponseEntity<String> deleteSchool(int schoolId)
	{
		Optional<School> optional = schoolRepository.findById(schoolId);
		if(optional.isPresent())
		{
			schoolRepository.delete(optional.get());
			return  new ResponseEntity<String>("deleted sucessfully",HttpStatus.OK);
		}
		else
		{
			return null;
		}
	}

	public ResponseEntity<School> updateSchool(int schoolId, School updatedSchool) 
	{
		Optional<School> optional = schoolRepository.findById(schoolId);
		if(optional.isPresent())
		{
			School exSchool = optional.get();
			School updatedSchool2 = mapToUpdatedSchool(exSchool,updatedSchool);
			return new ResponseEntity<School>(updatedSchool2,HttpStatus.OK);
		}
		else
		{
			return null;
		}
	}

	private School mapToUpdatedSchool(School schoolToUpdate,School updatedSchool)
	{
		schoolToUpdate.setSchoolName(updatedSchool.getSchoolName());
		schoolToUpdate.setEmailId(updatedSchool.getEmailId());
		schoolToUpdate.setContactNo(updatedSchool.getContactNo());
		schoolToUpdate.setAddress(updatedSchool.getAddress());
		
		return schoolToUpdate;
	}
	
	
}
