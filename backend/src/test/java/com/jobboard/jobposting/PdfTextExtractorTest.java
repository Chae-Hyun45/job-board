package com.jobboard.jobposting;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PdfTextExtractorTest {

    @TempDir
    Path tempDir;

    @Test
    void PDF에서_텍스트를_추출한다() throws Exception {
        Path pdfPath = tempDir.resolve("sample.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("TestCompany JobPosting");
                stream.endText();
            }
            document.save(pdfPath.toFile());
        }

        PdfTextExtractor extractor = new PdfTextExtractor();
        String text = extractor.extractText(pdfPath);

        assertThat(text).contains("TestCompany");
    }
}
