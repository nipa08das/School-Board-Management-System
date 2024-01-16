package com.school.sba.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//Lombok annotations
//@Data - generates getters,setters,toString,equals,hashCode
//@NoArgsConstructor - generates zero args constructor
//@AllArgsConstructor - generates parameterized constructor based on the variables present in the class

@Getter  //generates getters
@Setter  //generates setters

@Entity
public class School {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int schoolId;
	private String schoolName;
	private long contactNo;
	private String emailId;
	private String address;
	
	@OneToOne
	private Schedule schedule;

}
