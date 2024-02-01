package com.school.sba.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor

@Entity
public class School {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int schoolId;
	private String schoolName;
	private long contactNo;
	private String emailId;
	private String address;
	private boolean isDeleted;
	
	@OneToOne(mappedBy = "school",cascade = CascadeType.REMOVE, orphanRemoval = true)
	private Schedule schedule;
	
	@OneToMany(mappedBy = "school", fetch = FetchType.EAGER)
	private List<User> users;
	
	@OneToMany(mappedBy = "school", fetch = FetchType.EAGER)
	private List<AcademicProgram> academicPrograms;

}
