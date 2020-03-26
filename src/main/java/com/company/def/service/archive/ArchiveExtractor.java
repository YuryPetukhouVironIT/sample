package com.cephx.def.service.archive;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface ArchiveExtractor {
    List<File> extractFiles() throws IOException, InterruptedException;
}
