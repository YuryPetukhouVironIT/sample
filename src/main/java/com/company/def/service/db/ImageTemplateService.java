package com.company.def.service.db;

import com.company.def.CPatient;
import com.company.def.enums.PictureType;
import com.company.def.model.*;
import com.company.def.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class ImageTemplateService {
    @Autowired
    private  TemplateRowRepository templateRowRepository;
    @Autowired
    private  TemplateCellRepository templateCellRepository;
    @Autowired
    private CommonTemplateRepository commonTemplateRepository;
    @Autowired
    private TemplateImageRepository templateImageRepository;
    @Autowired
    private ImageTemplateRepository imageTemplateRepository;
    @Autowired
    private PatientImageRepository patientImageRepository;
    @Autowired
    private PatientImageService patientImageService;

    public void updateLayout(ImageTemplate template) {
        templateRowRepository.removeForTemplate(template.getId());
        templateCellRepository.remove(template.getId());
        for (TemplateRow row: template.getRows()) {
            templateRowRepository.insert(row);
            for (TemplateCell cell: row.getCells()) {
                templateCellRepository.insert(cell);
            }
        }
    }

    public void createCommonTemplateForAllDoctorCases(long doctorId, long commonTemplateId, long commonTemplateOwner) {
        List<CPatient> doctorPatients = PatientService.getDoctorPatients(doctorId);
        for (CPatient patient: doctorPatients) {
            long patientNumber = patient.getNumber();
            if (patientNumber != commonTemplateOwner) {
                commonTemplateRepository.insert(commonTemplateId, patient.GetNumber());
            }
        }
    }

    public long createImageTemplate(ImageTemplate imageTemplate) {
        long id = imageTemplateRepository.insert(imageTemplate);
        for (TemplateImage image: imageTemplate.getImages()) {
            image.setTemplateId(id);
            templateImageRepository.insert(image);
        }
        for (TemplateRow row : imageTemplate.getRows()) {
            row.setTemplateId(id);
            for (TemplateCell cell: row.getCells()) {
                cell.setTemplateId(id);
                templateCellRepository.insert(cell);
            }
            templateRowRepository.insert(row);
        }
        return id;
    }

    public void removeImageTemplate(long templateId) {
        templateImageRepository.removeAllImagesFromTemplate(templateId);
        templateCellRepository.remove(templateId);
        templateRowRepository.removeForTemplate(templateId);
        imageTemplateRepository.remove(templateId);
        commonTemplateRepository.removeCopiedTemplate(templateId);
    }

    public void renameTemplate(long templateId, String newName) {
        imageTemplateRepository.updateName(templateId, newName);
    }

    public List<ImageTemplate> createDefaultTemplates(long patientNumber) {
        List<ImageTemplate> defaultTemplates = new ArrayList<>();

        ImageTemplate layout11 = new ImageTemplate("11 Layout", patientNumber);
        layout11.setRows(getLayout533());
        defaultTemplates.add(layout11);

        ImageTemplate layout9 = new ImageTemplate("9 Layout", patientNumber);
        layout9.setRows(getLayout333());
        defaultTemplates.add(layout9);

        ImageTemplate intraoralTemplate = new ImageTemplate("Intraoral", patientNumber);
        intraoralTemplate.setRows(getIntraoralLayout());
        defaultTemplates.add(intraoralTemplate);

        for (ImageTemplate template: defaultTemplates) {
            long templateId = createImageTemplate(template);
            template.setId(templateId);

            updateEmptyCells(template);
        }
        return defaultTemplates;
    }

    public List<TemplateRow> getLayout533() {
        List<TemplateRow> rows = new ArrayList<>();

        TemplateRow row1 = new TemplateRow();
        row1.addCell(new TemplateCell(1,1, PictureType.FRONT_FACE_PHOTO_RELAXED, 0.1904, 1, 0.5));
        row1.addCell(new TemplateCell(1,2, PictureType.FRONTAL_CLOSED_LIPS,0.1904, 1, 0.5));
        row1.addCell(new TemplateCell(1,3, PictureType.FRONT_FACE_PHOTO_SMILE,0.1904, 1, 0.5));
        row1.addCell(new TemplateCell(1,4, PictureType.PROFILE,0.1904, 1, 0.5));
        row1.addCell(new TemplateCell(1,5, PictureType.CEPH_MAIN,0.1904, 1, 0.5));
        row1.setNumber(1);
        row1.setHeight(0.31733);
        row1.setFirstHorizontalLineY(0.333);
        row1.setSecondHorizontalLineY(0.666);
        rows.add(row1);

        TemplateRow row2 = new TemplateRow();
        row2.addCell(new TemplateCell(2,1, PictureType.INTRAORAL_LEFT, 0.32533, 1, 0.5));
        row2.addCell(new TemplateCell(2,2, PictureType.INTRAORAL_FRONT, 0.32533, 1, 0.5));
        row2.addCell(new TemplateCell(2,3, PictureType.INTRAORAL_RIGHT, 0.32533, 1, 0.5));
        row2.setNumber(2);
        row2.setHeight(0.31733);
        row2.setFirstHorizontalLineY(0.333);
        row2.setSecondHorizontalLineY(0.666);
        rows.add(row2);

        TemplateRow row3 = new TemplateRow();
        row3.addCell(new TemplateCell(3,1, PictureType.INTRAORAL_UPPER, 0.32533, 1, 0.5));
        row3.addCell(new TemplateCell(3,2, PictureType.PANORAMIC, 0.32533, 1, 0.5));
        row3.addCell(new TemplateCell(3,3, PictureType.INTRAORAL_LOWER, 0.32533, 1, 0.5));
        row3.setNumber(3);
        row3.setHeight(0.31733);
        row3.setFirstHorizontalLineY(0.333);
        row3.setSecondHorizontalLineY(0.666);
        rows.add(row3);

        return rows;
    }


    public List<TemplateRow> getLayout333() {
        List<TemplateRow> rows = new ArrayList<>();

        TemplateRow row1 = new TemplateRow();
        row1.addCell(new TemplateCell(1,1, PictureType.PROFILE_SMILE, 0.32533, 1, 0.5));
        row1.addCell(new TemplateCell(1,2, PictureType.PROFILE_RELAX, 0.32533, 1, 0.5));
        row1.addCell(new TemplateCell(1,3, PictureType.FRONTAL, 0.32533, 1, 0.5));
        row1.setNumber(1);
        row1.setHeight(0.31733);
        row1.setFirstHorizontalLineY(0.333);
        row1.setSecondHorizontalLineY(0.666);
        rows.add(row1);

        TemplateRow row2 = new TemplateRow();
        row2.addCell(new TemplateCell(2,1, PictureType.INTRAORAL_UPPER, 0.32533, 1, 0.5));
        row2.addCell(new TemplateCell(2,2, PictureType.INTRAORAL_LOWER, 0.32533, 1, 0.5));
        row2.addCell(new TemplateCell(2,3, PictureType.CEPH_MAIN, 0.32533, 1, 0.5));
        row2.setNumber(2);
        row2.setHeight(0.31733);
        row2.setFirstHorizontalLineY(0.333);
        row2.setSecondHorizontalLineY(0.666);
        rows.add(row2);

        TemplateRow row3 = new TemplateRow();
        row3.addCell(new TemplateCell(3,1, PictureType.INTRAORAL_LEFT, 0.32533, 1, 0.5));
        row3.addCell(new TemplateCell(3,2, PictureType.INTRAORAL_RIGHT, 0.32533, 1, 0.5));
        row3.addCell(new TemplateCell(3,3, PictureType.INTRAORAL_FRONT, 0.32533, 1, 0.5));
        row3.setNumber(3);
        row3.setHeight(0.31733);
        row3.setFirstHorizontalLineY(0.333);
        row3.setSecondHorizontalLineY(0.666);
        rows.add(row3);

        return rows;
    }

    public List<TemplateRow> getIntraoralLayout() {
        List<TemplateRow> rows = new ArrayList<>();

        TemplateRow row1 = new TemplateRow();
        row1.addCell(new TemplateCell(1,1, PictureType.EMPTY, 0.32533, 1, 0.5));
        row1.addCell(new TemplateCell(1,2, PictureType.INTRAORAL_UPPER, 0.32533, 1, 0.5));
        row1.addCell(new TemplateCell(1,3, PictureType.EMPTY, 0.32533, 1, 0.5));
        row1.setNumber(1);
        row1.setHeight(0.31733);
        row1.setFirstHorizontalLineY(0.333);
        row1.setSecondHorizontalLineY(0.666);
        rows.add(row1);

        TemplateRow row2 = new TemplateRow();
        row2.addCell(new TemplateCell(2,1, PictureType.INTRAORAL_LEFT, 0.32533, 1, 0.5));
        row2.addCell(new TemplateCell(2,2, PictureType.INTRAORAL_FRONT, 0.32533, 1, 0.5));
        row2.addCell(new TemplateCell(2,3, PictureType.INTRAORAL_RIGHT, 0.32533, 1, 0.5));
        row2.setNumber(2);
        row2.setHeight(0.31733);
        row2.setFirstHorizontalLineY(0.333);
        row2.setSecondHorizontalLineY(0.666);
        rows.add(row2);

        TemplateRow row3 = new TemplateRow();
        row3.addCell(new TemplateCell(3,1, PictureType.EMPTY, 0.32533, 1, 0.5));
        row3.addCell(new TemplateCell(3,2, PictureType.INTRAORAL_LOWER, 0.32533, 1, 0.5));
        row3.addCell(new TemplateCell(3,3, PictureType.EMPTY, 0.32533, 1, 0.5));
        row3.setNumber(3);
        row3.setHeight(0.31733);
        row3.setFirstHorizontalLineY(0.333);
        row3.setSecondHorizontalLineY(0.666);
        rows.add(row3);

        return rows;
    }

    public void updateEmptyCells(ImageTemplate template) {
        List<TemplateCell> emptyCells = template.getEmptyCells();
        if (emptyCells.size() > 0) {
            Set<PictureType> emptyTypes = new HashSet<>();
            for (TemplateCell cell: emptyCells) {
                emptyTypes.add(cell.getType());
            }
            List<PatientImage> patientImages = patientImageService.getPatientImagesForTypes(template.getPatientId(), emptyTypes);

            for (PatientImage patientImage: patientImages) {
                for (TemplateCell cell: emptyCells) {
                    if (cell.getType() == patientImage.getType()) {
                        TemplateImage templateImage = new TemplateImage(patientImage);
                        templateImage.setRowIndex(cell.getRow());
                        templateImage.setColumnIndex(cell.getColumn());
                        templateImage.setTemplateId(template.getId());
                        template.addImage(templateImage);
                        addImageToTemplate(patientImage.getId(), template.getId(), templateImage.getRowIndex(), templateImage.getColumnIndex());
                    }

                }

            }
        }
    }

    public void addImageToTemplate(long imageId, long templateId, int row, int column) {
        TemplateImage templateImage = new TemplateImage(new PatientImage(imageId));
        templateImage.setRowIndex(row);
        templateImage.setColumnIndex(column);
        templateImage.setTemplateId(templateId);
        templateImageRepository.insert(templateImage);
    }

    public List<ImageTemplate> getTemplatesForPatient(long patientId) {
        List<ImageTemplate> templates = imageTemplateRepository.getTemplatesForPatient(patientId);
        for (ImageTemplate template: templates) {
            List<TemplateImage> images = templateImageRepository.getImagesForTemplate(template.getId());
            for (TemplateImage image: images) {
                image.setImage(patientImageRepository.findById(image.getImage().getId()));
            }
            template.setImages(images);
            List<TemplateRow> templateRows = templateRowRepository.findByTemplate(template.getId());
            for (TemplateRow row: templateRows) {
                List<TemplateCell> rowCells = templateCellRepository.findByRow(template.getId(), row.getNumber());
                row.setCells(rowCells);
            }
            template.setRows(templateRows);
        }
        return templates;
    }

    public List<CommonTemplate> getCommonTemplates(long patientId) {
        return commonTemplateRepository.getCommonTemplates(patientId);
    }

    public void copyCommonTemplate(long commonTemplateId, long patientId) {
        ImageTemplate template = getTemplate(commonTemplateId);
        template.setPatientId(patientId);
        String newName = template.getName();
        newName += "_copy";
        template.setName(newName);
        template.setImages(Collections.<TemplateImage>emptyList());
        long copiedTemplateId = createImageTemplate(template);
        commonTemplateRepository.updateCopiedTemplateId(patientId, commonTemplateId, copiedTemplateId);
    }

    public ImageTemplate getTemplate(long templateId) {
        ImageTemplate template = imageTemplateRepository.findById(templateId);
        if (template != null) {
            template.setImages(templateImageRepository.getImagesForTemplate(templateId));
            for (TemplateImage image: template.getImages()) {
                image.setImage(patientImageRepository.findById(image.getImage().getId()));
            }
            template.setRows(templateRowRepository.findByTemplate(templateId));
            for (TemplateRow row: template.getRows()) {
                List<TemplateCell> cells = templateCellRepository.findByRow(templateId, row.getNumber());
                row.setCells(cells);
            }
        }
        return template;
    }

    public void correctTemplate(ImageTemplate template) {
        Iterator<TemplateImage> iterator = template.getImages().iterator();
        while (iterator.hasNext()) {
            TemplateImage templateImage = iterator.next();
            if (!templateImage.getImage().isPrimary()) {
                removeImagesFromTemplate(templateImage.getImage().getId(), template.getId());
                iterator.remove();
            } else {
                TemplateCell cell = template.getCell(templateImage.getRowIndex(), templateImage.getColumnIndex());
                if (cell == null || cell.getType() != templateImage.getImage().getType()) {
                    removeImagesFromTemplate(templateImage.getImage().getId(), template.getId());
                    iterator.remove();
                }
            }
        }
    }

    public void removeImagesFromTemplate(long imageId, long templateId) {
        templateImageRepository.remove(imageId, templateId);
    }

    public void removeImageFromTemplate(long templateId, int row, int column) {
        templateImageRepository.remove(templateId, row, column);
    }

    public void updateTemplateImage(TemplateImage templateImage) {
        templateImageRepository.update(templateImage);
    }

    public void updateTemplateOrder(ImageTemplate template) {
        imageTemplateRepository.updateOrder(template.getId(), template.getOrder());
    }

    public void updateVerticalOrientationLine(TemplateCell cell) {
        templateCellRepository.updateVerticalLine(cell.getTemplateId(), cell.getRow(), cell.getColumn(), cell.getVerticalLineX());
    }

    public void updateHorizontalOrientationLines(TemplateRow row) {
        templateRowRepository.updateHorizontalLines(row.getTemplateId(), row.getNumber(), row.getFirstHorizontalLineY(),
                row.getSecondHorizontalLineY());
    }


}
