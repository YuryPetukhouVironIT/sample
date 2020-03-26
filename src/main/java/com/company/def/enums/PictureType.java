package com.cephx.def.enums;

import java.util.*;

public enum PictureType {
    UNSPECIFIED("Unspecified"),
    CEPH_MAIN("Ceph - main for analysis"),
    CEPH_SECONDARY("Ceph secondary"),
    PROFILE("Profile"),
    PROFILE_SECONDARY("Profile secondary"),
    PROFILE_SMILE("Profile smile"),
    PROFILE_RELAX("Profile relax"),
    PANORAMIC("Panoramic"),
    PA("PA"),
    PA_TRACE("PA trace"),
    FRONTAL("Frontal"),
    FRONT_FACE_PHOTO_SMILE("Front face photo smile"),
    FRONT_FACE_PHOTO_RELAXED("Front face photo relaxed"),
    FRONTAL_OPEN_LIPS("Frontal open lips"),
    FRONTAL_CLOSED_LIPS("Frontal closed lips"),
    DEG_45_LEFT_FACE_PHOTO("45 deg. left face photo"),
    DEG_45_RIGHT_FACE_PHOTO("45 deg.right face photo"),
    MODEL_UPPER_PHOTO("Model Upper photo"),
    MODEL_LOWER_PHOTO("Model Lower photo"),
    MODEL_INTRAORAL_FRONT("Model Intraoral front"),
    MODEL_INTRAORAL_LEFT("Model Intraoral left"),
    MODEL_INTRAORLA_RIGHT("Model Intraoral right"),
    CEPH_TRACED("Ceph tracing"),
    INTRAORAL("Intraoral"),
    INTRAORAL_UPPER("Intraoral Upper"),
    INTRAORAL_LOWER("Intraoral lower"),
    INTRAORAL_FRONT("Intraoral front"),
    INTRAORAL_LEFT("Intraoral left"),
    INTRAORAL_RIGHT("Intraoral right"),
    EMPTY("Empty cell"),
    INFORMATION("Information cell");

    private String description;

    PictureType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static PictureType[] getUploadableTypes() {
        PictureType[] allValues = PictureType.values();
        List<PictureType> pictureTypeList = new ArrayList<>();
        for (PictureType type: allValues) {
            if (type != CEPH_MAIN && type != INFORMATION && type != EMPTY) {
                pictureTypeList.add(type);
            }
        }
        return pictureTypeList.toArray(new PictureType[allValues.length - 1]);
    }

    public static PictureType[] getTypesForImageGallery() {
        PictureType[] allValues = PictureType.values();
        List<PictureType> pictureTypeList = new ArrayList<>();
        for (PictureType type: allValues) {
            if (type != INFORMATION && type != EMPTY) {
                pictureTypeList.add(type);
            }
        }
        return pictureTypeList.toArray(new PictureType[allValues.length - 1]);
    }

    public static List<String> getPictureTypesAsList(Set<PictureType> types) {
        List<String> stringPictureTypes = new ArrayList<>();
        for (PictureType type: types) {
            stringPictureTypes.add(type.name());
        }
        return stringPictureTypes;
    }

    public static Map<String, String> getCellTypesMap() {
        Map<String, String> map = new HashMap<>();
        for (PictureType type: PictureType.values()) {
            map.put(type.name(), type.description);
        }
        return map;
    }

}
