package com.cephx.def.service;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;

import java.io.File;
import java.io.IOException;

public class FontService {
    private static BaseFont headerFont;

    static {
        try {
            headerFont = BaseFont.createFont(new File(FontService.class.getClassLoader()
                    .getResource("fonts/ARIALUNI.TTF").getFile()).getAbsolutePath(),
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BaseFont getHeaderFont() {
        return headerFont;
    }
}
