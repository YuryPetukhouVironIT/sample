package com.company.def.service.archive;

import java.io.File;
import java.util.List;

public abstract class AbstractArchiveExtractor implements ArchiveExtractor {

    protected void addRootFile(final List<File> uncompressedFiles, final List<File> directories, final File newFile) {
        boolean inSubdirectory = false;
        for (File subdirectory : directories) {
            if (newFile.getParentFile().equals(subdirectory)) {
                inSubdirectory = true;
            }
        }
        if (!inSubdirectory) {
            uncompressedFiles.add(newFile);
        }
    }
}
