package com.cephx.def.service.archive;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipExtractorApacheCommons implements ArchiveExtractor {

    private final File zip;
    private final File destDir;

    public ZipExtractorApacheCommons(final File zip, final File destDir) {
        this.zip = zip;
        this.destDir = destDir;
    }

    @Override
    public List<File> extractFiles() throws IOException, InterruptedException {
        final ZipFile zipFile = new ZipFile(zip);
        final List<File> uncompressedFiles = new ArrayList<>();
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while(entries.hasMoreElements()){
            ZipEntry entry = entries.nextElement();
            if(entry.isDirectory()){
                System.out.print("dir  : " + entry.getName());
                String destPath = destDir.getAbsolutePath() + File.separator + entry.getName();
                System.out.println(" => " + destPath);
                File file = new File(destPath);
                file.mkdirs();
            } else {
                String destPath = destDir.getAbsolutePath() + File.separator + entry.getName();

                try(InputStream inputStream = zipFile.getInputStream(entry);
                    FileOutputStream outputStream = new FileOutputStream(destPath);
                ){
                    int data = inputStream.read();
                    while(data != -1){
                        outputStream.write(data);
                        data = inputStream.read();
                    }
                }
                System.out.println("file : " + entry.getName() + " => " + destPath);
            }
        }
        return uncompressedFiles;
    }
}
