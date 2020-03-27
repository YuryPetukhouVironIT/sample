package com.company.def.service.archive;

import com.company.def.funcclass;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class RarExtractor extends AbstractArchiveExtractor {

    private final File rar;
    private final File destDir;

    public RarExtractor(final File rar, final File destDir) {
        this.rar = rar;
        this.destDir = destDir;
    }

    @Override
    public List<File> extractFiles() throws IOException, InterruptedException {
        extractRarFiles(rar, destDir);
        return filterFilesAndDirectories(destDir);

    }

    private void extractRarFiles(File rar, File destDir) throws InterruptedException, IOException {
        final String rarCommand = funcclass.RAR_LOCATION + " e -scu " + "\"" + rar.getAbsolutePath() + "\"" + " " + "\"" + destDir.getAbsolutePath() + "\"";
        final Process proc = Runtime.getRuntime().exec(new String[]{"bash", "-c", rarCommand, "exit"});
        try (final InputStream stdin = proc.getInputStream();
             final InputStreamReader isr = new InputStreamReader(stdin);
             final BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            proc.waitFor();
        }
    }

    private List<File> filterFilesAndDirectories(final File destDir) throws IOException {
        final List<File> uncompressedFiles = new ArrayList<>();
        final List<File> directories = new ArrayList<>();
        Files.walkFileTree(destDir.toPath(), new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                final File file = path.toFile();
                if (file.isDirectory()) {
                    directories.add(file);
                    uncompressedFiles.add(file);
                } else {
                    addRootFile(uncompressedFiles, directories, file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return uncompressedFiles;
    }
}
