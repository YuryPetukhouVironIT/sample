package com.company.def.enums;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.NUMBER)
public enum DicomType {
    DICOM_FILE,
    DICOM_FOLDER;
}
