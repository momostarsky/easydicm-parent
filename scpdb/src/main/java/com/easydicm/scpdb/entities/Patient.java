package com.easydicm.scpdb.entities;


import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

/**
 * http://dicom.nema.org/medical/dicom/2016d/output/chtml/part03/sect_C.2.2.html
 *
 * @author dhz
 */
@Data
@Getter
@Setter
public class Patient {

    @Length(min = 2, max = 50, message = "PatientId   长度必须在 {min} - {max} 之间")
    private String patid;

    @Length(min = 2, max = 50, message = "patname   长度必须在 {min} - {max} 之间")
    private String patname;


    @Length(min = 2, max = 64, message = "accnum   长度必须在 {min} - {max} 之间")
    private String accnum;

    @Length(min = 2, max = 64, message = "PatientId   长度必须在 {min} - {max} 之间")
    private String birthdate;

    @Length(min = 2, max = 64, message = "birthdate   长度必须在 {min} - {max} 之间")
    private String birthtime;

    @Length(min = 1, max = 2, message = "patsex   长度必须在 {min} - {max} 之间")
    private String patsex;

    @Length(min = 2, max = 10, message = "patage   长度必须在 {min} - {max} 之间")
    private String patage;

}
