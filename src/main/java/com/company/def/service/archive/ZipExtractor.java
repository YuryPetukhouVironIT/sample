package com.cephx.def.service.archive;

import com.cephx.def.util.file.FileUtility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ZipExtractor extends AbstractArchiveExtractor {

    private final File zip;
    private final File destDir;

    public ZipExtractor(final File zip, final File destDir) {
        this.zip = zip;
        this.destDir = destDir;
    }

    @Override
    public List<File> extractFiles() throws IOException {
        List<File> uncompressedFiles;
        try{
            uncompressedFiles = inputStreamDecompress();
        } catch (Exception e) {
            uncompressedFiles = zipFileDecompress();
        }
        return uncompressedFiles;
    }

    private List<File> inputStreamDecompress() throws IOException {
        final List<File> uncompressedFiles = new ArrayList<>();
        byte[] buffer = new byte[1024];
        final ZipInputStream zis = new ZipInputStream(new FileInputStream(zip), StandardCharsets.UTF_16);
        ZipEntry zipEntry = zis.getNextEntry();
        final List<File> directories = new ArrayList<>();
        long fileCount = 1;
        while (zipEntry != null) {
            if (zipEntry.isDirectory())
            {
                createSubdirectory(uncompressedFiles, zipEntry, directories);
            } else {
                final File newFile = newFile(destDir, zipEntry, fileCount);
                if (!newFile.getParentFile().exists()) {
                    newFile.getParentFile().mkdirs();
                }
                final FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                addRootFile(uncompressedFiles, directories, newFile);
            }
            zipEntry = zis.getNextEntry();
            fileCount++;
        }
        zis.closeEntry();
        zis.close();
        return uncompressedFiles;
    }

    private List<File> zipFileDecompress () throws IOException {
        final List<File> uncompressedFiles = new ArrayList<>();
        byte[] buffer = new byte[1024];
        final ZipFile zipFile = new ZipFile(zip);
        Enumeration<? extends ZipEntry> entries =zipFile.entries();
        final List<File> directories = new ArrayList<>();
        long fileCount = 1;
        while (entries.hasMoreElements()) {
            final ZipEntry zipEntry = entries.nextElement();
            if (zipEntry.isDirectory()) {
                createSubdirectory(uncompressedFiles, zipEntry, directories);
            } else {
                final File newFile = newFile(destDir, zipEntry, fileCount);
                if (!newFile.getParentFile().exists()) {
                    newFile.getParentFile().mkdirs();
                }
                try(InputStream inputStream = zipFile.getInputStream(zipEntry);
                    FileOutputStream outputStream = new FileOutputStream(newFile.getAbsolutePath());
                ){
                    int len;
                    while ((len = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, len);
                    }
//                    int data = inputStream.read();
//                    while(data != -1){
//                        outputStream.write(data);
//                        data = inputStream.read();
//                    }
                }
                addRootFile(uncompressedFiles, directories, newFile);
            }
            fileCount++;
        }

        return uncompressedFiles;
    }

    private void createSubdirectory(List<File> uncompressedFiles, ZipEntry zipEntry, List<File> directories) throws IOException {
        System.out.println("Creating Directory:" + destDir.getAbsolutePath() + zipEntry.getName());
        Files.createDirectories(FileSystems.getDefault().getPath(destDir.getAbsolutePath() + "/" + zipEntry.getName()));
        final File subdirectory = new File(destDir.getAbsolutePath() + "/" + zipEntry.getName());
        uncompressedFiles.add(subdirectory);
        directories.add(subdirectory);
    }

    private File newFile(final File destinationDir,final ZipEntry zipEntry, final long fileCount) throws IOException {
        final File destFile = new File(destinationDir, FileUtility.getFileItemName(zipEntry.getName(), fileCount));
        final String destDirPath = destinationDir.getCanonicalPath();
        final String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }
}
