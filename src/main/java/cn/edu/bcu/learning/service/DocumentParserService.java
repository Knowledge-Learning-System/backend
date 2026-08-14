package cn.edu.bcu.learning.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.extractor.QuickButCruddyTextExtractor;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xslf.extractor.XSLFExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档正文解析服务：PDF / Word / PPT 均支持。
 */
@Service
public class DocumentParserService {

    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 100;

    /** 按扩展名解析正文，返回纯文本；无法解析时返回 null。 */
    public String parse(File file, String ext) {
        if (file == null || !file.exists() || ext == null) {
            return null;
        }
        String e = ext.toLowerCase();
        try {
            switch (e) {
                case "pdf":
                    return parsePdf(file);
                case "docx":
                    return parseDocx(file);
                case "doc":
                    return parseDoc(file);
                case "pptx":
                    return parsePptx(file);
                case "ppt":
                    return parsePpt(file);
                default:
                    return null;
            }
        } catch (Exception ex) {
            return null;
        }
    }

    /** 将正文按固定大小切块（带重叠）。 */
    public List<String> splitChunks(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        String cleaned = text.replaceAll("\\s+", " ").trim();
        int len = cleaned.length();
        int start = 0;
        while (start < len) {
            int end = Math.min(start + CHUNK_SIZE, len);
            String chunk = cleaned.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (end >= len) {
                break;
            }
            start = end - CHUNK_OVERLAP;
        }
        return chunks;
    }

    private String parsePdf(File file) throws Exception {
        try (PDDocument doc = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String parseDocx(File file) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(file));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private String parseDoc(File file) throws Exception {
        try (HWPFDocument doc = new HWPFDocument(new FileInputStream(file));
             WordExtractor extractor = new WordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private String parsePptx(File file) throws Exception {
        try (XMLSlideShow ppt = new XMLSlideShow(new FileInputStream(file));
             XSLFExtractor extractor = new XSLFExtractor(ppt)) {
            return extractor.getText();
        }
    }

    private String parsePpt(File file) throws Exception {
        try (FileInputStream in = new FileInputStream(file)) {
            return new QuickButCruddyTextExtractor(in).getTextAsString();
        }
    }
}
