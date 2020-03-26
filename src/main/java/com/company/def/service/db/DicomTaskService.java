package com.cephx.def.service.db;

import com.cephx.def.BasicDoc;
import com.cephx.def.BasicPatientInfoData;
import com.cephx.def.CAdvancedPatInfo;
import com.cephx.def.CBasicPatientInfo;
import com.cephx.def.CPatient;
import com.cephx.def.Cdoctor;
import com.cephx.def.DBconnection;
import com.cephx.def.PatientPoints;
import com.cephx.def.RequestRTS_Mail;
import com.cephx.def.SlackClient;
import com.cephx.def.ZohoCrmClient;
import com.cephx.def.dto.NextDicomTaskDTO;
import com.cephx.def.enums.BillingMethod;
import com.cephx.def.enums.DicomType;
import com.cephx.def.exceptions.NoDoctorInSessionException;
import com.cephx.def.funcclass;
import com.cephx.def.intercom.IntercomClient;
import com.cephx.def.model.DicomRequest;
import com.cephx.def.model.DicomTask;
import com.cephx.def.model.NewPatientData;
import com.cephx.def.model.PatientImageToDisplay;
import com.cephx.def.repository.DicomRequestRepository;
import com.cephx.def.repository.DicomTaskRepository;
import com.cephx.def.repository.PatientImageToDisplayRepository;
import com.cephx.def.service.AnalysisService;
import com.cephx.def.service.S3Service;
import com.cephx.def.service.ZipCreationTask;
import com.cephx.def.service.archive.ArchiveExtractor;
import com.cephx.def.service.archive.RarExtractor;
import com.cephx.def.service.archive.ZipExtractor;
import com.cephx.def.servlets.patient.uploadNewPatientServlet;
import com.cephx.def.system;
import com.cephx.def.util.file.FileUtility;
import com.cephx.def.util.string.StringUtility;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.cephx.def.funcclass.COMPANY;

@Service
@Transactional
public class DicomTaskService {
    private static final Logger logger = LogManager.getLogger(DicomTaskService.class);
    public static final String DCM_EXTENSION = "DCM";

    @Autowired
    private DicomTaskRepository dicomTaskRepository;
    @Autowired
    private S3Service s3Service;
    @Autowired
    private PatientImageService patientImageService;
    @Autowired
    private IntercomClient intercomClient;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private PatientImageToDisplayRepository patientImageToDisplayRepository;
    @Autowired
    private DicomRequestRepository dicomRequestRepository;

    private DBconnection db = DBconnection.GetDBconnection();

    private FilenameFilter ignoreHidenFile = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return !name.startsWith(".");
        }
    };

    public boolean isNotCompletedTaskExists() throws Exception {
        updateStuckTaskStatuses();
        return dicomTaskRepository.existsByCompletedAndTaskStuck(false, false);
    }

    public NextDicomTaskDTO getNotCompletedTask() throws Exception {
        updateStuckTaskStatuses();
        final DicomTask nextTask = dicomTaskRepository.getFirstByCompletedAndTaskStuck(false, false);
        try {
            if (nextTask!=null) {
                final long expirationTimestamp = new Date(System.currentTimeMillis() + funcclass.DICOM_EXPIRATION_HOURS * 3600 * 1000).getTime();
                dicomTaskRepository.updateTaskExpirationTimestamp(expirationTimestamp, nextTask.getTransactionId());
                return new NextDicomTaskDTO(nextTask, db.getDocInfo(db.getDocIdByPatient(nextTask.getPatientId())), db.getPatientBasicInfo(nextTask.getPatientId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void updateStuckTaskStatuses() throws Exception {
        //final Long outdatedTime = new Date(System.currentTimeMillis() - funcclass.DICOM_EXPIRATION_HOURS * 3600 * 1000).getTime();
        final Long outdatedTime = new Date().getTime();
        final List<DicomTask> outdatedTasks = dicomTaskRepository.getOutdatedTasks(outdatedTime);
        final int numberOfOutdatedTasks = dicomTaskRepository.updateTaskStuckStatuses(outdatedTime);
        for (DicomTask task : outdatedTasks) {
            if (!task.isCompleted()) {
                slackNotifyTaskStuck(task);
            }
        }
    }

    private void slackNotifyTaskStuck(final DicomTask task) throws Exception {
        if (!funcclass.isLocalHost()) {
            long docId = db.getDocIdByPatient(task.getPatientId());
            BasicDoc docInfo = db.getDocInfo(docId);
            StringBuilder slackMessage = new StringBuilder();
            slackMessage.append("Dicom task is marked as stuck:\n");
            slackMessage.append("No:").append(docInfo.docnum).append("\n");
            slackMessage.append("Name:").append(docInfo.getDocFullName()).append("\n");
            slackMessage.append("Email:").append(docInfo.email).append("\n");
            slackMessage.append("TaskID:").append(task.getTransactionId()).append("\n");
            slackMessage.append("PatientID:").append(task.getPatientId()).append("\n");
            slackMessage.append("Env:").append(funcclass.getEnvironment()).append("\n");
            SlackClient.sendMessageToUrl(slackMessage.toString(), SlackClient.urlForDicom);

        }
    }

    public boolean moveFromS3ToGlacier(long transactionId) {
        try {
            logger.info("String s3 ");
            String s3 = dicomTaskRepository.getOne(transactionId).getS3Key();
            logger.info("String s3 = " + s3);
            s3Service.moveDicomToS3Glacier(s3);
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
        return true;
    }

    public boolean setTaskAsCompleted(long transactionId) {
        boolean isUpdated = dicomTaskRepository.updateTaskCompleted(transactionId, true) != 0;
        if (isUpdated) {
            return true;
        } else {
            logger.info("Status isUpdated for transaction " + transactionId + " is false . Returning false");
            return false;
        }
    }

    public long getPatientId(long id) {
        return dicomTaskRepository.findOne(id).getPatientId();
    }

    public void getTasks() {
    }

    public void updateCompleted(long dicomId) {
        dicomTaskRepository.updateTask(dicomId);
    }


    @Transactional
    public synchronized long createTask(int patientId, long filesCount, MultipartFile multipartFile, HttpSession session) throws IOException {

        File directory = null;
        File file = null;
        if (filesCount > 1) {

            int filesChecked = 0;
            if (session.getAttribute("dicomUploaded") == null) {
                session.setAttribute("dicomUploaded", 0);
            } else {
                filesChecked = (int) session.getAttribute("dicomUploaded");
            }
            final String tempDir = funcclass.tempUploadDir;
            directory = new File(tempDir + "/dicomDirTask" + patientId);
            String fileItemName = ((CommonsMultipartFile)multipartFile).getFileItem().getName();
            fileItemName = FileUtility.getFileItemName(fileItemName, filesCount);
            String folderName = "";
            if (fileItemName.contains("/")) {
                final String[] subfoldersNames = fileItemName.split("/");
                for (int i =0; i< subfoldersNames.length-1; i++) {
                    folderName+=subfoldersNames[i]+"/";
                }
            }
            file = new File(/*tempDir + "/dicomDir" + patientId*/ directory.getAbsolutePath() + "/" + ((CommonsMultipartFile)multipartFile).getFileItem().getName());
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();

            try {
                multipartFile.transferTo(file);
                s3Service.uploadRawFileToS3(patientId, file, folderName);
            } catch (Exception ex) {
                PatientService.deletePatient(patientId);
                logger.error(ex.getMessage());
            }
            synchronized (session) {
                filesChecked++;
                session.setAttribute("dicomUploaded", filesChecked);
            }
            if ( filesChecked == filesCount) {
                try {
                    final Collection<File> subdirectories = new ArrayList<>();
                    getSubdirectoriesRecursively (directory, subdirectories);
                    subdirectories.add(directory);
                    return processDicomDirectories(patientId, subdirectories);

                } catch (Exception ex) {
                    PatientService.deletePatient(patientId);
                    logger.error(ex.getMessage());
                } finally {
                    session.removeAttribute("dicomUploaded");
                    if (directory != null) {
                        for (File f : directory.listFiles()) {
                            f.delete();
                            logger.info("file {} successfully deleted ", f.getAbsolutePath());
                        }
                        directory.delete();
                        logger.info("Directory {} successfully deleted ", directory.getAbsolutePath());
                    }
                }
            }

        } else {
            try {

                String tempDir = funcclass.tempUploadDir;
                directory = new File(tempDir);
                directory.mkdirs();
                String fileItemName = multipartFile.getOriginalFilename();
                fileItemName = FileUtility.getFileItemName(fileItemName, filesCount);
                file = new File(directory.getAbsolutePath() + "/" + fileItemName);
                file.createNewFile();
                try {
                    multipartFile.transferTo(file);
                    s3Service.uploadRawFileToS3(patientId, file);
                } catch (Exception ex) {
                    PatientService.deletePatient(patientId);
                    logger.error(ex.getMessage());
                }
                long dicomId = getDicomIdFile(patientId, file);
                //---------Send to slack-----------------
                slackNotifyDicomUpload(patientId, dicomId);
                //----------------------------------------------------
                return dicomId;
            } catch (Exception ex) {
                PatientService.deletePatient(patientId);
                logger.error(ex.getMessage());
            } finally {
                file.delete();
                logger.info("file {} successfully deleted ", file.getAbsolutePath());
            }
        }
        return -1;
    }


    private Collection<File> getSubdirectoriesRecursively(final File directory, final Collection<File> subdirectories) {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                subdirectories.add(file);
                getSubdirectoriesRecursively(file, subdirectories);
            }
        }
        return subdirectories;
    }

    private long getDicomIdFile(int patientId, File file) {
        DicomTask task = new DicomTask();
        task.setS3Key("dicom-" + patientId + new Date().getTime());
        task.setDicomType(DicomType.DICOM_FILE);
        task.setPatientId(patientId);
        task.setCompleted(false);
        task.setProgress(false);
        task.setTaskStuck(false);
        task.setTimestamp(new Date().getTime());
        s3Service.uploadDicomFileToS3(task.getS3Key(), file);
        long dicomId = dicomTaskRepository.save(task).getTransactionId();

        return dicomId;
    }

    private void slackNotifyDicomUpload(int patientId, long dicomId) throws Exception {
        if (!funcclass.isLocalHost()) {
            long docId = db.getDocIdByPatient(patientId);
            BasicDoc docInfo = db.getDocInfo(docId);
            StringBuilder slackMessage = new StringBuilder();
            slackMessage.append("Dicom Upload:\n");
            slackMessage.append("No:").append(docInfo.docnum).append("\n");
            slackMessage.append("Name:").append(docInfo.getDocFullName()).append("\n");
            slackMessage.append("Email:").append(docInfo.email).append("\n");
            slackMessage.append("TaskID:").append(dicomId).append("\n");
            slackMessage.append("PatientID:").append(patientId).append("\n");
            slackMessage.append("Env:").append(funcclass.getEnvironment()).append("\n");
            SlackClient.sendMessageToUrl(slackMessage.toString(), SlackClient.urlForDicom);

        }
    }

    private long getDicomIdDirectory(final int patientId, final File directory) throws Exception {
        DicomTask task = new DicomTask();
        task.setS3Key("dicom-" + patientId + new Date().getTime());
        task.setDicomType(DicomType.DICOM_FOLDER);
        task.setPatientId(patientId);
        task.setCompleted(false);
        task.setProgress(false);
        task.setTaskStuck(false);
        task.setTimestamp(new Date().getTime());
        removeDicomDirFile(directory);
        removeSubdirectories(directory);
        s3Service.uploadDicomDirectoryToS3(task.getS3Key(), directory);
        long dicomId = dicomTaskRepository.save(task).getTransactionId();
        //---------Send to slack-----------------
        slackNotifyDicomUpload(patientId, dicomId);

        //----------------------------------------------------
        return dicomId;
    }

    private void removeSubdirectories(File directory) throws IOException {
        List<File> subdirectories = new ArrayList<>();
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                subdirectories.add(file);
            }
        }
        for (File subdirectory : subdirectories) {
            FileUtils.deleteDirectory(subdirectory);
        }
    }

    private void removeDicomDirFile(final File directory) {
        final List<File> dicomDirFiles = new ArrayList<>();
        for (File file : directory.listFiles()) {
            if (file.getName().toUpperCase().startsWith("DICOMDIR")) {
             dicomDirFiles.add(file);
            }
        }
        for (File fileToDelete : dicomDirFiles) {
            fileToDelete.delete();
        }
    }

    private long getDicomIdDirectoryAndFile(int patientId, File file, File subdirectory) throws Exception {

        DicomTask task = new DicomTask();
        task.setS3Key("dicom-" + patientId + new Date().getTime());
        task.setDicomType(DicomType.DICOM_FOLDER);
        task.setPatientId(patientId);
        task.setCompleted(false);
        task.setProgress(false);
        task.setTaskStuck(false);
        task.setTimestamp(new Date().getTime());
        s3Service.uploadDicomFileToS3(task.getS3Key(), file);
        s3Service.uploadDicomDirectoryToS3(task.getS3Key(), subdirectory);
        long dicomId = dicomTaskRepository.save(task).getTransactionId();

        //---------Send to slack-----------------
        slackNotifyDicomUpload(patientId, dicomId);

        //----------------------------------------------------
        return dicomId;
    }

    public NewPatientData createPatient(HttpServletRequest req, HttpServletResponse res) throws IOException {


        long patientNumber = 0;
        long doctorNumber = -1;
        Cdoctor doctor = null;
        HttpSession session = req.getSession(true);
        CBasicPatientInfo basicPatientInfo = null;
        CPatient p = null;
        CAdvancedPatInfo advancedPatInfo = null;
        boolean freeCredsWasRemoved = false;
        boolean prepaidCredsWasRemoved = false;
        boolean docHasCredits = true;
        int freeAnalysisCount = 0;
        int prepaidAnalysisCount = 0;
        boolean isnew = Boolean.parseBoolean(req.getParameter("IsNew"));
        boolean isAutoTrace = false;

        try {

            res.setContentType("text/html;charset=UTF-8");


            doctor = (Cdoctor) session.getAttribute("DocObj");
            if (doctor == null) {
                throw new NoDoctorInSessionException();
            } else {
                logger.info("Try to upload patient");
            }


            if (!doctor.isOperator() && doctor.getDocType() != COMPANY) {
                doctorNumber = doctor.DocNum();
            } else {
                doctorNumber = Long.parseLong(req.getParameter("selDoctor"));
            }

            String firstName = req.getParameter("firstName");
            if (firstName != null) {
                firstName = StringUtility.remove(firstName.trim(), "'");
            }

            logger.info("FirstName is {}", firstName);

            String lastName = req.getParameter("lastName");
            if (lastName != null) {
                lastName = StringUtility.remove(lastName.trim(), "'");
            }

            logger.info("lastName is {}", lastName);

            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            int age = 0;
            Date dateOfBirth = null;
            String ageString = req.getParameter("age");
            if (ageString != null) {
                age = Integer.valueOf(ageString);
            } else {
                dateOfBirth = dateFormat.parse(req.getParameter("dateOfBirth"));
            }

            Date cephDate = dateFormat.parse(req.getParameter("cephDate"));
            int gender = Integer.parseInt(req.getParameter("gender"));

            int n = req.getParameter("txN").equals("null") ? 0 : Integer.parseInt(req.getParameter("txN"));
            int gn = req.getParameter("txGn").equals("null") ? 0 : Integer.parseInt(req.getParameter("txGn"));
            int crowd = req.getParameter("MODEL_DISK").equals("null") ? 0 : Integer.parseInt(req.getParameter("MODEL_DISK"));
            int curv = req.getParameter("C_OF_SPEE").equals("null") ? 0 : Integer.parseInt(req.getParameter("C_OF_SPEE"));
            //ver 1.8 has no supervisior checkbox on gui
            long supervisorId = 0;

            Calendar rightNow = Calendar.getInstance();
            String strdate = funcclass.now();
            String strtime = rightNow.get(Calendar.HOUR) + ":" + rightNow.get(Calendar.MINUTE) + ":" + rightNow.get(Calendar.SECOND);
            int prod = db.getEcephProductNumber();

            isAutoTrace = db.isDoctorAllowedAutoTrace(doctorNumber);
            logger.info("Doctor {} is allowed to autotrace : {}", doctor.DocNum(), isAutoTrace);

            basicPatientInfo = new CBasicPatientInfo(firstName, lastName, strdate);
            advancedPatInfo = new CAdvancedPatInfo();
            advancedPatInfo.time = strtime;
            advancedPatInfo.product = prod;
            advancedPatInfo.pat_company = 0;
            advancedPatInfo.cupat = false;
            advancedPatInfo.pat_n = n;
            advancedPatInfo.pat_Gn = gn;
            advancedPatInfo.pat_Curv = curv;
            advancedPatInfo.pat_Discrepancy = crowd;
            if (dateOfBirth == null) {
                advancedPatInfo.pat_age = age;
            } else {
                advancedPatInfo.dateOfBirth = dateOfBirth;
                advancedPatInfo.setAgeFromBirthday();
            }

            advancedPatInfo.cephDate = cephDate;
            advancedPatInfo.pat_sex = gender;
            advancedPatInfo.addType = "Manual";
            advancedPatInfo.commentPhase = req.getParameter("analysisComment");
            try {
                advancedPatInfo.phase = CAdvancedPatInfo.Phases.valueOf(req.getParameter("analysisPhase"));
            } catch (Exception e) {
                advancedPatInfo.phase = CAdvancedPatInfo.Phases.PRE_TREATMENT;
            }
            advancedPatInfo.owner = Integer.parseInt(req.getParameter("owner"));
            advancedPatInfo.SupervisorId = supervisorId;
            if (req.getParameter("ext_patient_id") != null) {
                advancedPatInfo.ext_pat_id = req.getParameter("ext_patient_id");
            }
            if (req.getParameter("mainId") != null) {
                long mainAnalysisId = Long.parseLong(req.getParameter("mainId"));
                long mainIdForPatient = db.getMainAnalysisId(mainAnalysisId);
                if (mainIdForPatient != 0) {
                    advancedPatInfo.mainAnalysisId = mainIdForPatient;
                } else {
                    advancedPatInfo.mainAnalysisId = mainAnalysisId;
                }
            } else {
                advancedPatInfo.mainAnalysisId = 0L;
            }

            advancedPatInfo.SupervisionStat = funcclass.No_supervision;


            if (isnew) {
                p = new CPatient(advancedPatInfo, basicPatientInfo, new PatientPoints());
                if (!doctor.isOperator()) {
                    advancedPatInfo.employeeNum = funcclass.DOC_OP_NUM;
                    if (doctor.getDocType() == COMPANY) {
                        if (doctorNumber == 0) {
                            advancedPatInfo.DocNum = doctor.DocNum();
                        } else {
                            advancedPatInfo.DocNum = doctorNumber;
                        }
                        advancedPatInfo.pat_company = doctor.DocNum();
                    } else {
                        advancedPatInfo.DocNum = doctor.DocNum();
                        advancedPatInfo.pat_company = funcclass.NO_COMPANY;
                    }

                    //advancedPatInfo.uploaded=CAdvancedPatInfo.DOC_UPLOADED;
                    advancedPatInfo.uploaded = funcclass.DOCTOR;
                    BasicDoc docInfo = db.getDocInfo(doctorNumber);


                    if (docInfo.freeAnalyses > 0) {
                        p.AdvancedInfo.isFree = true;
                    }
                    final boolean sync = docInfo.hasBillingPlan;
                    patientNumber = db.saveNewPat(p, 0, 0, sync);
                    db.increaseCaseCount((int)doctorNumber,"3D");
                    if (!StringUtils.isEmpty(advancedPatInfo.commentPhase)) {
                        db.setDocRemark(patientNumber, advancedPatInfo.commentPhase);
                    }

                    if (!docInfo.hasBillingMethod() && (docInfo.prepaidAnalyses + docInfo.freeAnalyses) == 0) {
                        logger.info("Doctor {} hasn't free analysis. His billing method is {}.", doctor.DocNum(), docInfo.billingMethod);
                        PatientService.updatePatientOwner(patientNumber, funcclass.OPERATOR);
                        docHasCredits = false;
                    }
                    prepaidAnalysisCount = docInfo.prepaidAnalyses;
                    boolean tryPrepaid = true;
                    if (docInfo.freeAnalyses > 0) {
                        tryPrepaid = false;
                        db.setDocFreeAnalysis(doctorNumber, docInfo.freeAnalyses - 1);
                        freeCredsWasRemoved = true;
                        docInfo.freeAnalyses--;
                        freeAnalysisCount = docInfo.freeAnalyses;
                        if (BillingMethod.SHARE_DOC.equals(docInfo.billingMethod)) {
                            docInfo.billingMethod = null;
                            DoctorService.setDoctorBillingMethod(doctor.DocNum(), null);
                        }
                        doctor.docInfo = docInfo;
                        session.setAttribute("DocObj", doctor);
                        if (funcclass.isProdEnvironment() && docInfo.freeAnalyses == 0) {
                            HashMap<String, String> parameters = new HashMap<>();
                            parameters.put("CreditsConsumed", "TRUE");
                            ZohoCrmClient.updateLeadRecord(parameters, doctorNumber);
                        }
                    }
                    if (tryPrepaid && docInfo.prepaidAnalyses > 0) {
                        db.setDocPrepaidAnalysis(doctorNumber, docInfo.prepaidAnalyses - 1);
                        docInfo.prepaidAnalyses--;
                        prepaidCredsWasRemoved = true;
                        prepaidAnalysisCount = docInfo.prepaidAnalyses;
                        doctor.docInfo = docInfo;
                        session.setAttribute("DocObj", doctor);
                    }
                    if (docInfo.prepaidAnalyses == 0 && docInfo.hasBillingMethod() && docInfo.billingMethod.equals(BillingMethod.PREPAYMENT)) {
                        db.setDoctorBillingMethod(docInfo.docnum, null);
                        doctor.docInfo = docInfo;
                        session.setAttribute("DocObj", doctor);
                    }
                    p.SetNumber(patientNumber);
                    new RequestRTS_Mail(doctor, p, "");
                } else {
                    advancedPatInfo.employeeNum = (int) doctor.DocNum();
                    advancedPatInfo.DocNum = doctorNumber;
                    advancedPatInfo.uploaded = funcclass.OPERATOR;
                    advancedPatInfo.pat_company = Integer.parseInt((String) req.getParameter("Company"));
                    if (advancedPatInfo.pat_company == 0) {
                        advancedPatInfo.pat_company = funcclass.CEPHX_COMPANY;
                    }
                    final boolean sync = db.getDocInfo(doctorNumber).hasBillingPlan;
                    patientNumber = db.saveNewPat(p, 0, 0, sync);
                    db.increaseCaseCount((int)doctorNumber,"3D");
                    p.SetNumber(patientNumber);
                    new RequestRTS_Mail(doctor, p, "");
                }
            } else //for editing existing pat
            {
                patientNumber = Long.parseLong(req.getParameter("serialnum"));
                basicPatientInfo.setNumber(patientNumber);

                db.updateCephPictureS3Expiration(patientNumber, new Date());

                advancedPatInfo.isDigitized = PatientService.isDigitizedPatient(patientNumber);
                //get the rest of the patient data from the db
                CPatient oldpat = db.getPlottedPat(patientNumber);
                advancedPatInfo.pat_company = oldpat.AdvancedInfo.pat_company;
                advancedPatInfo.scale = oldpat.AdvancedInfo.scale;
                p = new CPatient(advancedPatInfo, basicPatientInfo, new PatientPoints());

                if (oldpat.get_pat_supervision_status() == funcclass.Done_supervision) {
                    p.AdvancedInfo.SupervisionStat = oldpat.AdvancedInfo.SupervisionStat;
                } else {
                    p.AdvancedInfo.SupervisionStat = funcclass.No_supervision;
                }

                if (!doctor.isOperator()) {

                    advancedPatInfo.employeeNum = oldpat.AdvancedInfo.employeeNum;

                    if (doctor.getDocType() == COMPANY) {
                        if (doctorNumber == 0) {
                            advancedPatInfo.DocNum = doctor.DocNum();
                        } else {
                            advancedPatInfo.DocNum = doctorNumber;
                        }

                        if (oldpat.AdvancedInfo.DocNum != doctorNumber) {
                            db.changePatpicksDoctor(doctorNumber, patientNumber);
                            funcclass.copyPatient(patientNumber, oldpat.AdvancedInfo.DocNum, doctorNumber);
                        }

                    } else {
                        advancedPatInfo.DocNum = oldpat.getDocNum();
                    }

                    advancedPatInfo.uploaded = funcclass.DOCTOR;
                    PatientService.updatePatientInfo(p);

                } else {
                    advancedPatInfo.employeeNum = (int) doctor.DocNum();
                    advancedPatInfo.DocNum = doctorNumber;
                    //advancedPatInfo.uploaded=CAdvancedPatInfo.OP_UPLOADED;
                    advancedPatInfo.uploaded = funcclass.OPERATOR;
                    advancedPatInfo.pat_company = Long.parseLong((String) req.getParameter("Company"));
                    PatientService.updatePatientInfo(p);
                    db.movePatPicks(patientNumber, oldpat.AdvancedInfo.DocNum, doctorNumber);

                }
            }
        } catch (NoDoctorInSessionException e) {
            PrintWriter out = res.getWriter();
            out.println("Unable to fetch Patient's doctor.");
            out.close();
            logger.warn(e);
        } catch (Exception e) {
            logger.error("Error during uploading patient", e);
        }
        PatientService.updateDicomStatus(patientNumber, true);

        NewPatientData newPatientData = new NewPatientData();
        newPatientData.setDoctorNumber(doctorNumber);
        newPatientData.setPatientNumber(patientNumber);
        newPatientData.setRequest(req);
        newPatientData.setResponse(res);
        newPatientData.setHttpSession(session);
        newPatientData.setDoctor(doctor);
        newPatientData.setPatient(p);
        newPatientData.setBasicPatientInfo(basicPatientInfo);
        newPatientData.setNew(isnew);
        newPatientData.setAdvancedPatInfo(advancedPatInfo);
        newPatientData.setFreeCredsWasRemoved(freeCredsWasRemoved);
        newPatientData.setPrepaidCredsWasRemoved(prepaidCredsWasRemoved);
        newPatientData.setDocHasCredits(docHasCredits);
        newPatientData.setFreeAnalysisCount(freeAnalysisCount);
        newPatientData.setPrepaidAnalysisCount(prepaidAnalysisCount);
        newPatientData.setAutoTrace(isAutoTrace);

        return newPatientData;
    }

    @Transactional
    public boolean setTaskDone(final HttpServletRequest request) {

        final long transactionId = Long.valueOf(request.getParameter("TransactionId"));
        final String cephS3Path = request.getParameter("Ceph_S3_path");
        final String stl1S3Path = request.getParameter("STL1_S3_path");
        final String stl2S3Path = request.getParameter("STL2_S3_path");
        final String imagesS3Path = request.getParameter("Images_S3_path");

        logger.info("request parameters: ");
        logger.info("transactionId: " + transactionId);
        logger.info("cephS3Path: " + cephS3Path);
        logger.info("stl1S3Path: " + stl1S3Path);
        logger.info("stl2S3Path: " + stl2S3Path);
        logger.info("imagesS3Path: " + imagesS3Path);

        long patId = 0;
        long docId = 0;
        BasicDoc docInfo = null;
        try {
            if (StringUtils.isBlank(request.getParameter("TransactionId"))) {
                logger.info("Transaction " + transactionId + " is blank value. Returning false");
                return false;
            }
            db.setProgress(transactionId);
            DicomTask task = dicomTaskRepository.findOne(new Long(transactionId));
            if (task == null) {
                return true;
            }
            patId = task.getPatientId();
            docId = db.getDocIdByPatient(patId);
            docInfo = db.getDocInfo(docId);

            if (!StringUtils.isBlank(cephS3Path)) {
                PatientService.addCephS3Path(patId, cephS3Path);
                if (!PatientService.isAlgoCeph(patId)) {
                    patientImageService.uploadImageFromDicom(patId, "CEPH_MAIN", true, cephS3Path, docInfo, docId);
                    handleCephFile(transactionId, cephS3Path);
                }
            } else {
                PatientService.addCephS3Path(patId, "n");
            }
            List<String> imagePathes = new ArrayList<>();
            if (!StringUtils.isBlank(stl1S3Path)) {
                PatientService.addStl1S3Path(patId, stl1S3Path);
                createZipFiles(patId, stl1S3Path, docInfo.email, docInfo.docnum);
                final String imagesDirectoryPath = stl1S3Path + "/images";
                imagePathes = s3Service.getPathesFromDirectory(imagesDirectoryPath);
                if (imagePathes.size() > 0) {
                    final List<String> imagePathesToDisplay = pathesToDisplay(imagePathes);
                    for (String path : imagePathesToDisplay) {
                        if (lastKeyPart(path).toLowerCase().equals("panaroma_mandibular.png") || lastKeyPart(path).toLowerCase().equals("panaroma_maxillary.png")) {
                            patientImageService.uploadImageFromDicom(patId, "PANORAMIC", false, path, docInfo, docId);
                        } else {
                            patientImageService.uploadImageFromDicom(patId, "UNSPECIFIED", false, path, docInfo, docId);
                        }
                    }
                }
            }
            if (!StringUtils.isBlank(imagesS3Path)) {
                final List<String> pathes = s3Service.getPathesFromDirectory(imagesS3Path);
                for (String path : pathes) {
                    if (!imagePathes.contains(path)) {
                        if (lastKeyPart(path).toLowerCase().equals("panaroma_mandibular.png") || lastKeyPart(path).toLowerCase().equals("panaroma_maxillary.png")) {
                            patientImageService.uploadImageFromDicom(patId, "PANORAMIC", false, path, docInfo, docId);
                        } else {
                            patientImageService.uploadImageFromDicom(patId, "UNSPECIFIED", false, path, docInfo, docId);
                        }
                        if (lastKeyPart(path).toLowerCase().equals(funcclass.LEFT_PROJECTION_FILE_NAME)) {
                            PatientService.processLeftProjection(patId, docId, path);
                        }
                    }
                }
            }
            PatientService.checkAirwaysData(patId);
        } catch (Exception ex) {
            final DicomRequest failedRequest = new DicomRequest();
            final String requestJson = IOUtils.toString(request.getReader());
            failedRequest.setProcessed(false);
            failedRequest.setSkipped(false);
            failedRequest.setJson(requestJson);
            failedRequest.setDateTime(new Date());
            dicomRequestRepository.save(failedRequest);
            logger.error(ex.getMessage(), ex);
            if (!funcclass.isLocalHost()) {
                final String message = "Cloud failed to process request to endpoint /set3DTaskDone. Request json is: " + requestJson;
                SlackClient.sendMessageToChannel(message,SlackClient.urlForDicom);
            }
        } finally {
            logger.info("===== set task done finally=====");
            moveFromS3ToGlacier(transactionId);
            PatientService.updateDicomStatus(patId, true);
            return setTaskAsCompleted(transactionId);
        }
    }

    private List<String> pathesToDisplay(List<String> pathes) {
        final List<String> pathesToDisplay = new ArrayList<>();
        final List<PatientImageToDisplay> imagesToDisplay = patientImageToDisplayRepository.findAll();
        final List<String> fileNamesToDisplay = new ArrayList<>();
        for (PatientImageToDisplay imageToDisplay : imagesToDisplay) {
            fileNamesToDisplay.add(imageToDisplay.getName().toLowerCase());
        }
        for (String path : pathes) {
            final String fileName = lastKeyPart(path).toUpperCase();
            if (fileNamesToDisplay.contains(fileName.toLowerCase())) {
                pathesToDisplay.add(path);
            }
        }
        return pathesToDisplay;
    }

    public void handleCephFile(long taskId, String s3Key) throws Exception {

        File file = null;
        File file2d = null;
        try {
            long patId = dicomTaskRepository.findOne(taskId).getPatientId();
            long docId = db.getDocIdByPatient(patId);

            String doctorNum = String.valueOf(docId);
            String patientNum = String.valueOf(patId);
            //-------------------
            system.log("*******in upload new patient num: " + doctorNum + " serialnum: " + patientNum);

            //get file from s3
            //        String rndFieName = patientData.getRequest().getParameter("cephFile");//Math.random()+"";

            file = new File("tmpDir/" + patientNum);
            file.mkdirs();

            file2d = new File("tmpDir/" + patientNum);//s3Service.getFFromS3(s3Key, file.toPath());


            db.insertNewPictureCeph("JPEG", patId, docId, "Ceph", 3);
            uploadNewPatientServlet.handlePatientCephFileD(doctorNum, patientNum, true, file2d, null, true, s3Key);


            if (file2d != null && file2d.exists()) {
                file2d.delete();
            }

            BasicDoc docInfo = db.getDocInfo(docId);


            if (PatientService.getPatientOwner(patId) == funcclass.OPERATOR) {      //if Removed

                db.setDocFreeAnalysis(patId, docInfo.freeAnalyses);

                db.setDocPrepaidAnalysis(patId, docInfo.prepaidAnalyses);
            }

            if (docInfo.alertOnSlack) {
                SlackClient.sendMessageToUrl(String.format("New case by [%s].", docInfo.getDocFullName()), SlackClient.urlSupportChannel);
            }

            if (funcclass.isProdEnvironment()) {
                Map<String, String> parameters = new HashMap<>();
                SimpleDateFormat dateFormatZoho = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                parameters.put(ZohoCrmClient.LAST_UPLOADED_CEPH, dateFormatZoho.format(new Date()));
                parameters.put(ZohoCrmClient.TOTAL_CEPHS_UPLOADED, String.valueOf(PatientService.getNotDeletedPatientCount(docId)));
                ZohoCrmClient.updateRecord(parameters, String.valueOf(docId), ZohoCrmClient.LEADS_MODULE);
                ZohoCrmClient.updateRecord(parameters, String.valueOf(docId), ZohoCrmClient.ACCOUNTS_MODULE);
                DBconnection.GetDBconnection().insertNotUpdatedDoctor(docId);
            }
        } finally {
            if (file2d != null && file2d.exists()) {
                file2d.delete();
            }
        }


    }

    public void createZipFiles(final long patId, final String stl1S3Path, final String doctorAddress, final long doctorId) throws Exception {
        final Runnable zipCreationTask = zipTask(patId, stl1S3Path, doctorAddress, doctorId, intercomClient);
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(zipCreationTask);
    }

    public void createZipFilesSynchronously(final long patientId) throws Exception {
        final long doctorId = db.getDocIdByPatient(patientId);
        final BasicDoc docInfo = db.getDocInfo(doctorId);
        final String stl1S3Path = PatientService.getStlPath(patientId);
        PatientService.addStl1S3Path(patientId, stl1S3Path);
        final Runnable zipCreationTask = zipTask(patientId, stl1S3Path, docInfo.email, doctorId, null);
        zipCreationTask.run();
    }

    public void createZipJsonSynchronously(final long patientId) throws Exception {
        final long doctorId = db.getDocIdByPatient(patientId);
        final BasicDoc docInfo = db.getDocInfo(doctorId);
        final String stl1S3Path = PatientService.getStlPath(patientId);
        PatientService.addStl1S3Path(patientId, stl1S3Path);
        final ZipCreationTask zipCreationTask = zipTask(patientId, stl1S3Path, docInfo.email, doctorId, null);
        zipCreationTask.createStlZipJson(patientId, stl1S3Path, docInfo.isCompany);
    }

    private ZipCreationTask zipTask(final long patId, final String stl1S3Path, final String doctorAddress, final long doctorId, final IntercomClient client) throws Exception {
        final BasicPatientInfoData patientInfo = db.getPatientBasicInfo(patId);
        final BasicDoc doctorInfo = db.getDocInfo(doctorId);
        final String patientFirstName = patientInfo.getPatientFirstName();
        final String patientLastName = patientInfo.getPatientLastName();
        final String username = doctorInfo.user;
        final boolean isCompany = doctorInfo.isCompany;
        return new ZipCreationTask(s3Service, client, stl1S3Path, patId, doctorAddress, doctorId, patientFirstName, patientLastName, username, isCompany);
    }

    public long createTaskZip(final Integer patientId, final MultipartFile uploadingFile) {
        long taskId = -1;
        try {
            logger.info("CreateDicomZip dicomPatientId : " + patientId);
            String tempDir = funcclass.tempUploadDir;
            File directory = new File(tempDir + "/dicomDirTaskZip" + patientId);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            final String fileName = archiveFileName(uploadingFile.getOriginalFilename());
            File fileZip = new File(directory.getAbsolutePath() + "/" + fileName);
            fileZip.createNewFile();
            uploadingFile.transferTo(fileZip);
            s3Service.uploadRawFileToS3(patientId, fileZip);
            File destDir = new File(tempDir + "/dicomDirTaskUnzip" + patientId);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            ArchiveExtractor extractor = null;
            if (fileZip.getName().toUpperCase().endsWith(".ZIP")) {
                extractor = new ZipExtractor(fileZip, destDir);
            }
            if (fileZip.getName().toUpperCase().endsWith(".RAR")) {
                extractor = new RarExtractor(fileZip, destDir);
            }
            final List<File> uncompressedFiles = extractor.extractFiles();
                taskId = processDicomDirectories(patientId, uncompressedFiles);
            fileZip.delete();
            FileUtils.deleteDirectory(directory);
            FileUtils.deleteDirectory(destDir);

        } catch (Exception ex) {
            PatientService.deletePatient(patientId);
            logger.error("Failed to create zip", ex);
        }
        return taskId;
    }

    private String archiveFileName(final String originalFilename) {
        if (StringUtility.isLatinString(originalFilename)) {
            return originalFilename;
        }
        final String fileName ="DicomArchive";
        final String extenstion = getArchiveExtension(originalFilename);
        return fileName + extenstion;
    }

    private String getArchiveExtension(final String originalFilename) {
        if (originalFilename.toUpperCase().endsWith(".ZIP")) {
            return ".ZIP";
        }
        if (originalFilename.toUpperCase().endsWith(".RAR")) {
            return ".RAR";
        }
        if (originalFilename.toUpperCase().startsWith("ZIP.")) {
            return ".ZIP";
        }
        if (originalFilename.toUpperCase().startsWith("RAR.")) {
            return ".RAR";
        }
        return ".ZIP";
    }


    private long processDicomDirectories(final int patientId, final Collection<File> uncompressedFiles) throws Exception {

        long taskId = -1;
        File dicomSingleFile = null;
        File dicomDirectory = null;
        for (File file : uncompressedFiles) {
            if (!file.isDirectory()) {
                if (directoryContainsSingleFile(file.getParentFile()) && file.getAbsolutePath().toUpperCase().endsWith(DCM_EXTENSION)) {
                    dicomSingleFile = file;
                }
                if (directoryContainsMultipleFiles(file.getParentFile())) {
                    if (directoryContainsDcmFiles(file.getParentFile())) {
                        dicomDirectory = file.getParentFile();
                    } else {
                        if (dicomDirectory == null || (!directoryContainsDcmFiles(dicomDirectory))) {
                            dicomDirectory = checkMaxFilesDirectory(dicomDirectory, file.getParentFile());
                        }
                    }
                }
            } else {
                if (directoryContainsDcmFiles(file)) {
                    if (directoryContainsSingleFile(file)) {
                        dicomSingleFile = getSingleFileFromDirectory(file);
                    }
                    if (directoryContainsMultipleFiles(file)) {
                        if (directoryContainsDcmFiles(file)) {
                            dicomDirectory = file;
                        }
                    }
                } else {
                    if (dicomDirectory == null || (!directoryContainsDcmFiles(dicomDirectory))) {
                        dicomDirectory = checkMaxFilesDirectory(dicomDirectory, file);
                    }
                }
            }
        }
        if (dicomSingleFile != null && dicomDirectory != null) {
            if (directoryContainsDcmFiles(dicomDirectory)) {
                taskId = getDicomIdDirectoryAndFile(patientId, dicomSingleFile, dicomDirectory);
            } else {
                taskId = getDicomIdFile(patientId, dicomSingleFile);
            }
        } else {
            if (dicomDirectory != null) {
                if (directoryContainsDcmFiles(dicomDirectory)) {
                    removeNonDcmFiles(dicomDirectory);
                }
                taskId = getDicomIdDirectory(patientId, dicomDirectory);
            }
            if (dicomSingleFile != null) {
                taskId = getDicomIdFile(patientId, dicomSingleFile);
            }


        }
        return taskId;
    }

    private File checkMaxFilesDirectory(final File maxFilesDirectory, final File checkedDirectory) {
        if (maxFilesDirectory == null) {
            return checkedDirectory;
        }
        return numberOfFiles(maxFilesDirectory) < numberOfFiles(checkedDirectory) ? checkedDirectory : maxFilesDirectory;
    }

    private void removeNonDcmFiles(final File dicomDirectory) throws IOException {
        final List<File> files = Arrays.asList(dicomDirectory.listFiles());
        for (File file : files) {
            if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
            }
            if (!file.getAbsolutePath().toUpperCase().endsWith(DCM_EXTENSION)) {
                file.delete();
            }
        }
    }

    private boolean directoryContainsDcmFiles(final File directory) {
        boolean containsDcm = false;
        if (directory.isDirectory()) {
            final Iterator<File> it = Arrays.asList(directory.listFiles()).iterator();
            while (it.hasNext() && !containsDcm) {
                containsDcm = it.next().getAbsolutePath().toUpperCase().endsWith(DCM_EXTENSION);
            }
        }
        return containsDcm;
    }

    private boolean directoryContainsSingleFile(final File directory) {
        return numberOfFiles(directory) == 1;
    }

    private boolean directoryContainsMultipleFiles(final File directory) {
        return numberOfFiles(directory) > 1;
    }

    private int numberOfFiles(File directory) {
        int numberOfFiles = 0;
        for (File file : directory.listFiles()) {
            if (!file.isDirectory()) {
                numberOfFiles++;
            }
        }
        return numberOfFiles;
    }


    private File getSingleFileFromDirectory(final File directory) {
        File singleFile = null;
        int i = 0;
        while (i < directory.listFiles().length && singleFile == null) {
            singleFile = directory.listFiles()[i].isDirectory() ? null : directory.listFiles()[i];
            i++;
        }
        return singleFile;
    }

    private String lastKeyPart(final String s3Path) {
        String[] pathSubstrings = s3Path.split("/");
        return pathSubstrings[pathSubstrings.length - 1];
    }

}
