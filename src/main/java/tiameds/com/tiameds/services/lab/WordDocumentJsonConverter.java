package tiameds.com.tiameds.services.lab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Turns a Word document (.docx or legacy .doc) into JSON of the form
 * {"blocks":[{"type":"paragraph","text":"..."},{"type":"table","rows":[["a","b"]]}]},
 * keeping the order of paragraphs and tables. Formatting (fonts, images) is not kept.
 */
final class WordDocumentJsonConverter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WordDocumentJsonConverter() {
    }

    static String toJson(byte[] data) throws IOException {
        ArrayNode blocks = MAPPER.createArrayNode();
        if (isDocx(data)) {
            readDocx(data, blocks);
        } else {
            readDoc(data, blocks);
        }
        ObjectNode root = MAPPER.createObjectNode();
        root.set("blocks", blocks);
        return MAPPER.writeValueAsString(root);
    }

    // .docx is a zip archive ("PK"); .doc is an OLE2 file.
    private static boolean isDocx(byte[] data) {
        return data.length > 1 && data[0] == 'P' && data[1] == 'K';
    }

    private static void readDocx(byte[] data, ArrayNode blocks) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(data))) {
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    addParagraph(blocks, paragraph.getText(), paragraph.getStyle());
                } else if (element instanceof XWPFTable table) {
                    ArrayNode rows = MAPPER.createArrayNode();
                    for (XWPFTableRow row : table.getRows()) {
                        ArrayNode cells = rows.addArray();
                        for (XWPFTableCell cell : row.getTableCells()) {
                            cells.add(clean(cell.getText()));
                        }
                    }
                    addTable(blocks, rows);
                }
            }
        }
    }

    private static void readDoc(byte[] data, ArrayNode blocks) throws IOException {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(data))) {
            Range range = document.getRange();
            int count = range.numParagraphs();
            int i = 0;
            while (i < count) {
                Paragraph paragraph = range.getParagraph(i);
                if (!paragraph.isInTable()) {
                    addParagraph(blocks, paragraph.text(), null);
                    i++;
                    continue;
                }
                Table table = range.getTable(paragraph);
                ArrayNode rows = MAPPER.createArrayNode();
                for (int r = 0; r < table.numRows(); r++) {
                    TableRow row = table.getRow(r);
                    ArrayNode cells = rows.addArray();
                    for (int c = 0; c < row.numCells(); c++) {
                        TableCell cell = row.getCell(c);
                        cells.add(clean(cell.text()));
                    }
                }
                addTable(blocks, rows);
                // Skip every paragraph that belongs to this table.
                int end = table.getEndOffset();
                while (i < count && range.getParagraph(i).getStartOffset() < end) {
                    i++;
                }
            }
        }
    }

    private static void addParagraph(ArrayNode blocks, String text, String style) {
        String cleaned = clean(text);
        if (cleaned.isEmpty()) {
            return;
        }
        ObjectNode block = blocks.addObject();
        block.put("type", "paragraph");
        block.put("text", cleaned);
        if (style != null && !style.isBlank()) {
            block.put("style", style);
        }
    }

    private static void addTable(ArrayNode blocks, ArrayNode rows) {
        ObjectNode block = blocks.addObject();
        block.put("type", "table");
        block.set("rows", rows);
    }

    // Removes Word control characters (cell/row marks, carriage returns); vertical tab is a soft line break.
    private static String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u000b', '\n')
                .replaceAll("[\\r\\u0007\\u000c]", "")
                .trim();
    }
}
