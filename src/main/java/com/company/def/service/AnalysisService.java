package com.cephx.def.service;

import com.cephx.def.BasicDoc;
import com.cephx.def.DBconnection;
import com.cephx.def.enums.PictureType;
import com.cephx.def.funcclass;
import com.cephx.def.model.PatientImage;
import com.cephx.def.pdf;
import com.cephx.def.service.db.DicomTaskService;
import com.cephx.def.service.db.PatientImageService;
import com.cephx.def.service.db.PatientService;
import com.cephx.def.servlets.accounts.UserAccounts;
import com.cephx.def.servlets.accounts.UserAccountsConsts;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class AnalysisService {
    private static final Logger logger = LogManager.getLogger(AnalysisService.class);
    private static final DBconnection dbConnection = DBconnection.GetDBconnection();

    @Autowired
    private S3Service s3Service;
    @Autowired
    private PatientImageService patientImageService;
    @Autowired
    private DicomTaskService dicomTaskService;

    public void getPDFAnalysis(HttpServletRequest vReq, HttpServletResponse vRes) {
        long patientNumber = Long.parseLong(vReq.getParameter("patnum"));
        long doctorNumber = Long.parseLong(vReq.getParameter("docnum"));
        String patientName = vReq.getParameter("patname");
        String doctorName = vReq.getParameter("DocName");
        String showLogo = "true";

        logger.info("Downloading analysis for patient {}", patientNumber);

        if (vReq.getParameter("showLogo") != null) {
            showLogo = vReq.getParameter("showLogo");
            logger.info("showLogo param string is " + showLogo + " ,after boolean parsing is " + new Boolean(showLogo));
        }

        vRes.setContentType("application/pdf;charset=UTF-8");
        String fileName = getDownFileName(patientName, patientNumber) + ".pdf";

        //vRes.setHeader("content-disposition", "inline;filename=" + fileName);
        vRes.setHeader("content-disposition", "attachment;filename=" + fileName);
        logger.info("Filename of analysis in PDF format is {}", fileName);

        StringTokenizer st = new StringTokenizer(vReq.getParameter("data"), UserAccountsConsts.DEF_DELIM);
        String[] ret = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            ret[i++] = st.nextToken();
        }
        DBconnection cn = UserAccounts.getDBConnection();

        try {
            CountingOutputStream cos = new CountingOutputStream(vRes.getOutputStream());
            new pdf(patientNumber, ret, cos, doctorName, cn.getDocPreferences(doctorNumber).getLogoPath(), new Boolean(showLogo));
                vRes.setContentLength((int) cos.getByteCount());
        } catch (Exception e) {
            logger.error("Error", e);
        }
    }

    public void getPDFAnalysisPrint(HttpServletRequest vReq, HttpServletResponse vRes) {
        long patientNumber = Long.parseLong(vReq.getParameter("patnum"));
        long doctorNumber = Long.parseLong(vReq.getParameter("docnum"));
        String patientName = vReq.getParameter("patname");
        String doctorName = vReq.getParameter("DocName");
        String showLogo = "true";

        logger.info("Downloading analysis for patient {}", patientNumber);

        if (vReq.getParameter("showLogo") != null) {
            showLogo = vReq.getParameter("showLogo");
            logger.info("showLogo param string is " + showLogo + " ,after boolean parsing is " + new Boolean(showLogo));
        }

        vRes.setContentType("application/pdf;charset=UTF-8");
        String fileName = getDownFileName(patientName, patientNumber) + ".pdf";
        logger.info("Filename of analysis in PDF format is {}", fileName);

        StringTokenizer st = new StringTokenizer(vReq.getParameter("data"), UserAccountsConsts.DEF_DELIM);
        String[] ret = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            ret[i++] = st.nextToken();
        }
        DBconnection cn = UserAccounts.getDBConnection();

        try {
            CountingOutputStream cos = new CountingOutputStream(vRes.getOutputStream());
            new pdf(patientNumber, ret, cos, doctorName, cn.getDocPreferences(doctorNumber).getLogoPath(), new Boolean(showLogo));
            vRes.setContentLength((int) cos.getByteCount());
        } catch (Exception e) {
            logger.error("Error", e);
        }
    }

    private String getDownFileName(String vName, long vNum) {

        String badChars = "~!@#$%^&*()-+=}{[]\"':;?><,`\\";
        for (int i = 0; i < vName.length(); ++i) {
            if (badChars.indexOf("" + vName.charAt(i)) != -1) {
                vName = vName.replace(vName.charAt(i), ' ');
            }
        }
        if (vName.trim().equals("")) {
            vName = "cephX patient " + vNum;
        }
        return vName.trim();
    }

    public void getStlFile(HttpServletRequest request, HttpServletResponse response, final String patientId) {

        File directory = null;
        File file = null;
        File fileRequest = null;
        String stlPath = PatientService.getStl1Path(Long.valueOf(patientId));
        ByteArrayResource resource = null;

        try {

            String tempDir = funcclass.tempPatientImagesPath;
            directory = new File(tempDir + "/dicomDir" + patientId);
            directory.mkdirs();
            fileRequest = s3Service.downloadStlFromS3(stlPath, directory.toPath());

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + stlPath.replace("STL/", "") + ".STL");

            byte[] fileContent = Files.readAllBytes(fileRequest.toPath());

            ServletOutputStream out = response.getOutputStream();
            out.write(fileContent);
            out.flush();
            out.close();
        } catch (IOException ex) {

        } finally {
            if (directory != null) {
                directory.delete();

                logger.info("Directory {} successfully deleted ", directory.getAbsolutePath());
            }
            if (fileRequest != null) {
                fileRequest.delete();
                logger.info("fileRequest {} successfully deleted ", directory.getAbsolutePath());
            }
        }
    }



    public void getJPGFile(HttpServletRequest request, HttpServletResponse response) {

        long patientNumber = Long.parseLong(request.getParameter("patnum"));
        long doctorNumber = Long.parseLong(request.getParameter("docnum"));
        String patientName = request.getParameter("patname");
        String doctorName = request.getParameter("DocName");
        String showLogo = "true";

        logger.info("Downloading analysis for patient {}", patientNumber);

        if (request.getParameter("showLogo") != null) {
            showLogo = request.getParameter("showLogo");
            logger.info("showLogo param string is " + showLogo + " ,after boolean parsing is " + new Boolean(showLogo));
        }

        response.setContentType("application/jpeg;charset=UTF-8");

        StringTokenizer st = new StringTokenizer(request.getParameter("data"), UserAccountsConsts.DEF_DELIM);
        String[] ret = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            ret[i++] = st.nextToken();
        }
        DBconnection cn = UserAccounts.getDBConnection();

        File directory = null;
        File zipDirectory = null;
        File jpgDirectory = null;
        File pdfFile = null;

        try {
            String tempDir = funcclass.tempPatientImagesPath;
            directory = new File(tempDir + "/pdfDir" + patientNumber);
            directory.mkdirs();
            zipDirectory = new File(tempDir + "/pdfDirZip" + patientNumber);
            zipDirectory.mkdirs();
            jpgDirectory = new File(tempDir + "/jpgDir" + patientNumber);
            jpgDirectory.mkdirs();
            pdfFile = new File(directory.getAbsolutePath() + "/pdfFile.pdf");
            FileOutputStream fos = new FileOutputStream(pdfFile);
            new pdf(patientNumber, ret, fos, doctorName, cn.getDocPreferences(doctorNumber).getLogoPath(), new Boolean(showLogo));

            try (final PDDocument document = PDDocument.load(pdfFile)) {
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                for (int page = 0; page < document.getNumberOfPages(); ++page) {
                    BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
                    String fileName = jpgDirectory.getAbsolutePath() + "/" + getDownFileName(patientName, patientNumber) + page + ".jpeg";
                    ImageIOUtil.writeImage(bim, fileName, 300);
                }
                document.close();
            } catch (IOException e) {
                System.err.println("Exception while trying to create pdf document - " + e);
            }

            String zipName = "/dirCompressed.zip";
            FileOutputStream zipfos = new FileOutputStream(zipDirectory.getAbsolutePath() + zipName);
            ZipOutputStream zipOut = new ZipOutputStream(zipfos);
            File fileToZip = new File(jpgDirectory.getAbsolutePath());

            zipFile(fileToZip, fileToZip.getName(), zipOut);
            zipOut.close();
            fos.close();

            response.setHeader("Content-Disposition", "attachment; filename=\"" + zipName + "\"");
            response.setHeader("Content-Type", "application/zip");
            File tmpFile = new File(zipDirectory.getAbsolutePath() + zipName);

            response.getOutputStream().write(Files.readAllBytes(tmpFile.toPath()));
            response.flushBuffer();
        } catch (Exception e) {
            logger.error("Error", e);
        } finally {
            if (zipDirectory != null) {
                for (File f : zipDirectory.listFiles()) {
                    f.delete();
                }
                zipDirectory.delete();
                logger.info("ZipDirectory {} successfully deleted ", zipDirectory.getAbsolutePath());
            }
            if (jpgDirectory != null) {
                for (File f : jpgDirectory.listFiles()) {
                    f.delete();
                }
                jpgDirectory.delete();
                logger.info("JPGDirectory {} successfully deleted ", zipDirectory.getAbsolutePath());
            }
            if (directory != null) {
                for (File f : directory.listFiles()) {
                    f.delete();
                }
                directory.delete();
                logger.info("Directory {} successfully deleted ", directory.getAbsolutePath());
            }
            if (pdfFile != null) {
                pdfFile.delete();
            }
        }
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

    public void getPPTFile(HttpServletRequest request, HttpServletResponse response) {

        long patientNumber = Long.parseLong(request.getParameter("patnum"));
        long doctorNumber = Long.parseLong(request.getParameter("docnum"));
        String patientName = request.getParameter("patname");
        String doctorName = request.getParameter("DocName");
        String showLogo = "true";

        logger.info("Downloading analysis for patient {}", patientNumber);

        if (request.getParameter("showLogo") != null) {
            showLogo = request.getParameter("showLogo");
            logger.info("showLogo param string is " + showLogo + " ,after boolean parsing is " + new Boolean(showLogo));
        }

        StringTokenizer st = new StringTokenizer(request.getParameter("data"), UserAccountsConsts.DEF_DELIM);
        String[] ret = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            ret[i++] = st.nextToken();
        }
        DBconnection cn = UserAccounts.getDBConnection();

        File directory = null;
        File pptFile = null;
        File pdfFile = null;
        File jpgDirectory = null;

        try {
            String tempDir = funcclass.tempPatientImagesPath;
            directory = new File(tempDir + "/pdfDir" + patientNumber);
            directory.mkdirs();
            jpgDirectory = new File(tempDir + "/slideDir" + patientNumber);
            jpgDirectory.mkdirs();
            pptFile = new File(directory.getAbsolutePath() + "/pptFile.ppt");
            pdfFile = new File(directory.getAbsolutePath() + "/pdfFile.pdf");
            FileOutputStream fos = new FileOutputStream(pdfFile);
            new pdf(patientNumber, ret, fos, doctorName, cn.getDocPreferences(doctorNumber).getLogoPath(), new Boolean(showLogo));

            XMLSlideShow ppt = new XMLSlideShow();
            ppt.setPageSize(new Dimension(595, 842));

            FileOutputStream pptout = new FileOutputStream(pptFile);

            try (final PDDocument document = PDDocument.load(pdfFile)) {
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                for (int page = 0; page < document.getNumberOfPages(); ++page) {
                    BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
//                    String fileName = "image-" + page + ".png";
                    String fileName = jpgDirectory.getAbsolutePath() + "/" + getDownFileName(patientName, patientNumber) + page + ".jpeg";
                    ImageIOUtil.writeImage(bim, fileName, 300);
                    XSLFSlide slide = ppt.createSlide();
                    byte[] pictureData = IOUtils.toByteArray(
                            new FileInputStream(fileName));

                    XSLFPictureData pd
                            = ppt.addPicture(pictureData, PictureData.PictureType.JPEG);
                    XSLFPictureShape picture = slide.createPicture(pd);
//                    picture.setAnchor(new Rectangle(0, 0, (int)PageSize.A4.getWidth(), (int)PageSize.A4.getHeight()));
                    picture.setAnchor(new Rectangle(0, 0, 595, 842));
                }
                ppt.write(pptout);
                pptout.close();
                document.close();
            } catch (IOException e) {
                System.err.println("Exception while trying to create pdf document - " + e);
            }
            response.setHeader("Content-Disposition", "attachment; filename=\"" + pptFile.getName() + "\"");
            response.setHeader("Content-Type", "application/ppt");
            response.getOutputStream().write(Files.readAllBytes(pptFile.toPath()));
            response.flushBuffer();
        } catch (Exception e) {
            logger.error("Error", e);
        } finally {
            if (jpgDirectory != null) {
                for (File f : jpgDirectory.listFiles()) {
                    f.delete();
                }
                jpgDirectory.delete();
            }
            if (directory != null) {
                for (File f : directory.listFiles()) {
                    f.delete();
                }
                directory.delete();

                logger.info("Directory {} successfully deleted ", directory.getAbsolutePath());
            }
            if (pdfFile != null) {
                pdfFile.delete();
            }
            if (pptFile != null) {
                pptFile.delete();
            }
        }
    }

    public void getStlZipFile(final HttpServletRequest request, final HttpServletResponse response, final String patientId) {

        final String patientZipName = "stl_" + lastKeyPart(PatientService.getStlPath(Long.valueOf(patientId))) + ".zip";

        getZipFile(response, patientId, patientZipName,"VideoZip");
    }

    public void getVideosZipFile(final HttpServletRequest request, final HttpServletResponse response, final String patientId) {
        final String patientZipName = "video_" + lastKeyPart(PatientService.getStlPath(Long.valueOf(patientId))) + ".zip";
        getZipFile(response, patientId, patientZipName, "StlZip");
    }

    private void getZipFile (final HttpServletResponse response, final String patientId, final String zipFileName, final String suffix) {
        final String tempDir = funcclass.tempPatientImagesPath;
        final File directory = new File(tempDir + "/dicomDir"+suffix + patientId);
        directory.mkdirs();
        final File destinationFile = new File(directory.getAbsolutePath()+"/"+zipFileName);
        final String stlPath = PatientService.getStlPath(Long.valueOf(patientId));
        final String zipFileKey = stlPath + "/" + zipFileName;
        if (!s3Service.s3FileExists(zipFileKey)) {
            zipStlFiles (Long.parseLong(patientId));
        }
        final File zipFile = s3Service.downloadSingleFileFromS3(zipFileKey, destinationFile.toPath());
        writeZipToResponse(response, zipFile);
        cleanFiles(directory, zipFile);
    }

    private void zipStlFiles(final Long patientId) {
        try {
            final long doctorId = dbConnection.getDocIdByPatient(patientId);
            final BasicDoc docInfo = dbConnection.getDocInfo(doctorId);
            final String stl1S3Path = PatientService.getStlPath(patientId);
            PatientService.addStl1S3Path(patientId, stl1S3Path);
            dicomTaskService.createZipFilesSynchronously(patientId);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private void cleanFiles(final File directory, final File zipFile) {
        zipFile.delete();
        if (directory != null) {
            directory.delete();
            logger.info("Directory {} successfully deleted ", directory.getAbsolutePath());
        }
    }

    private void writeZipToResponse(HttpServletResponse response, File zipFile) {
        try {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                "attachment;filename=" + zipFile.getName());
            final byte[] fileContent = Files.readAllBytes(zipFile.toPath());
            final ServletOutputStream out = response.getOutputStream();
            out.write(fileContent);
            out.flush();
            out.close();
        } catch (IOException ex) {
            logger.error(ex);
        }
    }

    public void removeCeph(final HttpServletRequest request, final HttpServletResponse response) {
        long patientId = Long.parseLong(request.getParameter("patnum"));
        java.util.List<PatientImage> images = patientImageService.getPatientImages(patientId);
        for (PatientImage image : images) {
            if (image.getType().equals(PictureType.CEPH_MAIN) || image.getType().equals(PictureType.CEPH_SECONDARY) || image.getType().equals(PictureType.CEPH_TRACED)) {
                patientImageService.changeImageType(image.getId(), PictureType.UNSPECIFIED);
            }
        }
        DBconnection.GetDBconnection().setPatientAnalysisOwner(patientId, -1);
    }

    private String lastKeyPart(final String s3Path) {
        final String[] pathSubstrings= s3Path.split("/");
        return pathSubstrings[pathSubstrings.length-1];
    }
}
