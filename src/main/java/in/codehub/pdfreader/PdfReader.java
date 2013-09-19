package in.codehub.pdfreader;

import in.codehub.document.Document;
import in.codehub.document.Line;
import in.codehub.document.Page;
import in.codehub.document.Paragraph;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.TextNormalize;
import org.apache.pdfbox.util.TextPosition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class PdfReader extends PDFParserListener
{
    private static PdfReader instance = null;

    private final PDFParser pdfParser;
    private final TextNormalize normalize;
    private Document document;

    private Map<Integer, Integer> fontSizeMap = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> leftMarginMap = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> rightMarginMap = new HashMap<Integer, Integer>();
    private Page currPage;
    private Paragraph currParagraph;
    private static final String SPACE = " ";
    private static final int FONT_OFFSET = 1000000;
    private static final int CAP_OFFSET = 100;

    public static PdfReader getInstance() throws IOException
    {
        if (instance == null)
        {
            TextNormalize normalize = new TextNormalize("UTF-8");
            PDFParser pdfParser = new PDFParser(normalize);
            instance = new PdfReader(pdfParser, normalize);
        }
        return instance;
    }

    private PdfReader(PDFParser pdfParser, TextNormalize normalize)
    {
        this.pdfParser = pdfParser;
        this.normalize = normalize;
        pdfParser.setDropThreshold(2.8f);
        pdfParser.setListener(this);
    }

    public Document read(String filePath) throws IOException
    {
        PDDocument doc = PDDocument.load(filePath);
        return read(doc);
    }

    public Document read(File file) throws IOException
    {
        PDDocument doc = PDDocument.load(file);
        return read(doc);
    }

    public Document read(InputStream inputStream) throws IOException
    {
        PDDocument doc = PDDocument.load(inputStream);
        return read(doc);
    }

    public Document read(PDDocument doc) throws IOException
    {
        pdfParser.parse(doc);
        doc.close();
        reset();
        return document;
    }

    @Override
    void startPage(PDPage page, int pageNo)
    {
        PDRectangle box = page.getTrimBox();
        currPage = new Page(pageNo, Math.round(box.getWidth()), Math.round(box.getHeight()));
    }

    @Override
    void startParagraph()
    {
        currParagraph = new Paragraph();
    }

    @Override
    void endParagraph()
    {
        if (currParagraph.getLines().size() > 0)
            currPage.addParagraph(currParagraph);
    }

    @Override
    void newLine(List<TextPosition> line)
    {
        extractParams(line, currPage);
        if (line.size() > 0)
        {
            Line l = createLine(line);
            if (l.length() > 0) currParagraph.addLine(l);
        }
    }

    @Override
    void endLine()
    {
    }

    @Override
    void endPage(PDPage page)
    {
        if (currPage.getParagraphs().size() > 0)
            document.addPage(currPage);
    }

    @Override
    void startDocument(PDDocument pdf)
    {
        document = new Document();
    }

    @Override
    void endDocument(PDDocument pdf)
    {
        document.setContentFontSize(getMax(fontSizeMap, 1));
        int l = getMax(leftMarginMap, 1);
        int r = currPage.getWidth() - getMax(rightMarginMap, -1);
        int t = 40;
        document.setMargins(l, r, t, t);
    }

    @Override
    void startArticle()
    {
    }

    @Override
    void endArticle()
    {
    }


    private void reset()
    {
        pdfParser.resetEngine();
        fontSizeMap.clear();
        leftMarginMap.clear();
        rightMarginMap.clear();
    }

    private void extractParams(List<TextPosition> line, Page page)
    {
        for (TextPosition text : line)
            addToMap(fontSizeMap, Math.round(text.getFontSizeInPt()));

        if (line.size() > 0 && !(line.get(0) instanceof WordSeparator))
        {
            int margin = normalize(line.get(0).getX(), false);
            if (margin < page.getWidth() * .25)
                addToMap(leftMarginMap, margin);
        }

        if (line.size() > 0 && !(line.get(line.size() - 1) instanceof WordSeparator))
        {
            int margin = normalize(line.get(line.size() - 1).getX(), true);
            if (margin > page.getWidth() * .7)
                addToMap(rightMarginMap, margin);
        }
    }

    private Line createLine(List<TextPosition> line)
    {
        int maxFontSize = 0;
        int count = 0;
        StringBuilder lineBuilder = new StringBuilder();
        TextPosition firstText = null;
        TextPosition lastText = null;
        for (TextPosition text : line)
        {
            if (text instanceof WordSeparator)
            {
                lineBuilder.append(SPACE);
            }
            else
            {
                lineBuilder.append(text.getCharacter());
                int fontSize = getFontSize(text);
                count += (fontSize == maxFontSize ? 1 : -1);
                if (count < 0) maxFontSize = fontSize;
                if (firstText == null) firstText = text;
                lastText = text;
            }
        }
        int firstX = firstText != null ? Math.round(firstText.getX()) : -1;
        int lastX = lastText != null ? Math.round(lastText.getX()) : -1;
        int y = firstText != null ? Math.round(firstText.getY()) : -1;

        return new Line(getString(lineBuilder), maxFontSize, firstX, lastX, y);
    }

    private void addToMap(Map<Integer, Integer> map, Integer key)
    {
        Integer count = map.get(key);
        if (count == null)
            count = 0;
        count += 1;
        map.put(key, count);

    }

    private int getFontSize(TextPosition text)
    {
        float fontSize = text.getFontSizeInPt();
        float capHeight = 0;
        float italicAngle = 0;
        PDFont font = text.getFont();
        if (font != null && font.getFontDescriptor() != null)
        {
            capHeight = font.getFontDescriptor().getCapHeight();
            italicAngle = font.getFontDescriptor().getItalicAngle();
        }
        return (int) (fontSize * FONT_OFFSET + capHeight * CAP_OFFSET + italicAngle);
    }

    private int getMax(Map<Integer, Integer> map, final int order)
    {
        List<Map.Entry<Integer, Integer>> list = new ArrayList<Map.Entry<Integer, Integer>>();
        int count = 0;
        for (Map.Entry<Integer, Integer> entry : map.entrySet())
        {
            list.add(entry);
            count += entry.getValue();
        }
        Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>()
        {
            @Override
            public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2)
            {
                int diff = o2.getValue() - o1.getValue();
                if (diff == 0)
                {
                    diff = (o1.getKey() - o2.getKey()) * order;
                }
                return diff;
            }
        });
        int result = list.size() > 0 ? list.get(0).getKey() : 0;
        if (list.size() > 1)
        {
            Map.Entry<Integer, Integer> entry = list.get(1);
            int diff = Math.abs(percentage(list.get(0).getValue(), count) - percentage(entry.getValue(), count));
            if (order == 1 && entry.getKey() < result && diff < 10)
                result = entry.getKey();
        }
        return result;
    }

    private int percentage(int value, int count)
    {
        return (int) Math.round(value * 100.0 / count);
    }

    private String getString(StringBuilder sb)
    {
        return normalize.normalizePres(sb.toString()).replaceAll("\\p{C}", "").trim();
    }

    private int normalize(float number, boolean toUpper)
    {
        int n = Math.round(number);
        int delta = 4;
        return toUpper ? n + delta - n % delta : n - n % delta;
    }
}
