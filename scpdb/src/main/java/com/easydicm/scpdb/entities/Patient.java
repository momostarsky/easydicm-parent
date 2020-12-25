package com.easydicm.scpdb.entities;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * http://dicom.nema.org/medical/dicom/2016d/output/chtml/part03/sect_C.2.2.html
 *
 * @author dhz
 */
@Data
@Getter
@Setter
public class Patient {

    private String PatientId;

    private String PatientName;

}
