package in.codehub.paperparser;

import in.codehub.document.*;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

public class PaperParser
{
    private static PaperParser instance = null;
    private String ABSTRACT = "abstract";
    private String[] PREFIXES = {"keywords", "index terms", "general terms"};
    private String[] NO_NAMES = {"Computer", "Department", "Science", "University", "School", "Academy", "College",
            "Abstract", "Email", " of ", ","};
    private Pattern EMAIL_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    private static final String SPACE = " ";
    private float absConf = 1;

    private PaperParser()
    {

    }

    public static PaperParser getInstance()
    {
        if (instance == null)
        {
            instance = new PaperParser();
        }
        return instance;
    }

    public Paper parse(Document document)
    {
        absConf = 1;
        Paper paper = new Paper(document.getId());
        markUseless(document);
        DocumentIterator iterator = new DocumentIterator(document);
        paper.setTitle(extractTitle(iterator));
        paper.getAuthors().addAll(extractAuthors(iterator));
        paper.setAbstract(extractAbstract(iterator));
        paper.getKeywords().addAll(extractKeywords(iterator));
        return paper;
    }

    private String extractAbstract(DocumentIterator iterator)
    {
        String abs = iterator.currParagraph().text();
        int abstractFontSize = iterator.currParagraph().fontSize();
        Paragraph paragraph;
        while (iterator.hasNextParagraph() && isAbstractEx(paragraph = iterator.nextParagraph(), abstractFontSize, abs.length()))
        {
            abs += " " + paragraph.text();
        }
        return StringUtils.removeStartIgnoreCase(abs, ABSTRACT).replaceAll("^[^A-Za-z]*", "");
    }

    private String extractTitle(DocumentIterator iterator)
    {
        Line line = null;

        while (iterator.hasNextLine())
        {
            line = iterator.nextLine();
            if (line.tag() == null) break;
        }

        StringBuilder title = new StringBuilder();
        if (line != null)
        {
            int titleFontSize = 0;
            while (line.getFontSize() > titleFontSize)
            {
                title = new StringBuilder();
                title.append(line.getText());
                line.setTag(PaperTags.TITLE);
                titleFontSize = line.getFontSize();
                while (iterator.hasNextLine())
                {
                    Line currLine = iterator.nextLine();
                    if (line.getFontSize() == currLine.getFontSize())
                    {
                        title.append(SPACE).append(currLine.getText());
                        currLine.setTag(PaperTags.TITLE);
                        line = currLine;
                    }
                    else
                    {
                        line = currLine;
                        break;
                    }
                }
            }
        }
        return title.toString().trim();
    }

    private List<String> extractKeywords(DocumentIterator iterator)
    {
        Set<String> keywords = new HashSet<String>();
        Paragraph paragraph = iterator.currParagraph();
        while (isKeyWordSection(paragraph))
        {
            String getText = paragraph.text().trim();
            for (String prefix : PREFIXES)
                getText = StringUtils.removeStartIgnoreCase(getText, prefix);
            for (String keyword : Arrays.asList(getText.split(",")))
            {
                String str = StringUtils.normalizeSpace(keyword).replaceAll("[^A-Za-z0-9 ]", " ");
                keywords.add(StringUtils.removeStart(str, "and "));
            }
            paragraph = iterator.nextParagraph();
        }
        return new ArrayList<String>(keywords);
    }

    private List<String> extractAuthors(DocumentIterator iterator)
    {
        List<String> authors = new ArrayList<String>();
        Line authorLine = iterator.currLine();
        if (authorLine != null)
        {
            int authorFontSize = authorLine.getFontSize();
            addToAuthors(authors, authorLine, true);
            boolean allFound = authors.size() > 1;

            if (!allFound)
            {
                for (Line line : iterator.currParagraph().getLines())
                {
                    if (line.tag() == null && !allFound && line.getFontSize() == authorFontSize)
                        addToAuthors(authors, line, false);
                    allFound = allFound || authors.size() >= 4;
                }
            }

            Paragraph prevPara = iterator.currParagraph();
            while (iterator.hasNextParagraph())
            {
                Paragraph currPara = iterator.nextParagraph();
                if (!isAbstract(currPara, prevPara))
                {
                    Line line = iterator.nextLine();
                    if (!allFound && line.getFontSize() == authorFontSize)
                        addToAuthors(authors, line, false);
                    allFound = allFound || authors.size() >= 4;

                }
                else break;
                prevPara = currPara;
            }
        }
        return authors;
    }

    private void addToAuthors(List<String> authors, Line line, boolean isFirstLine)
    {
        line.setTag(PaperTags.AUTHOR);
        String text = line.getText();
        if (isFirstLine)
        {
            text = text.replaceAll("[^A-Za-z. ,]", "").trim();
            for (String keyword : Arrays.asList(text.split(",")))
            {
                String str = StringUtils.normalizeSpace(keyword);
                str = StringUtils.removeStartIgnoreCase(str, "and ");
                for (String name : str.split(" and "))
                {
                    String boiled = boilAuthor(name);
                    if (boiled.length() > 0) authors.add(boiled);
                }
            }
        }
        else if (text.length() < 25)
        {
            text = text.replaceAll("[^A-Za-z. ,]", "").trim();

            boolean isName = text.length() > 0;
            for (String str : NO_NAMES)
            {
                if (StringUtils.containsIgnoreCase(text, str))
                {
                    isName = false;
                    break;
                }
            }
            if (isName && (text = boilAuthor(text)).length() > 0)
                authors.add(text);
        }
    }

    private String boilAuthor(String author)
    {
        if (author.length() < 5) return "";
        if (author.lastIndexOf(' ') == author.length() - 2)
            author = author.substring(0, author.length() - 2);
        return author;
    }

    private boolean isAbstractEx(Paragraph paragraph, int abstractFontSize, int length)
    {
        return abstractFontSize == paragraph.fontSize()
                && abstractFontSize == paragraph.getLines().get(0).getFontSize()
                && length + paragraph.textLength() < 1200 && !isKeyWordSection(paragraph);
    }

    private boolean isKeyWordSection(Paragraph paragraph)
    {
        String getText = paragraph.text().trim().toLowerCase();
        return getText.length() < 150 && StringUtils.startsWithAny(getText, PREFIXES);
    }

    private boolean isAbstract(Paragraph currPara, Paragraph prevPara)
    {
        String lastText = prevPara.text();
        if (StringUtils.containsIgnoreCase(lastText, ABSTRACT) && lastText.length() < 12) return true;

        String getText = currPara.text();
        if (getText.length() > 20 && StringUtils.startsWithIgnoreCase(getText, ABSTRACT)) return true;

        if (getText.length() > 350)
        {
            absConf *= .9;
            return true;
        }
        return false;
    }

    private void markUseless(Document document)
    {
        boolean isTitleFound = false;
        for (DocumentIterator iterator = document.iterator(); iterator.hasNextLine(); )
        {
            Line line = iterator.nextLine();
            Page page = iterator.currPage();
            if (!isInsideContentArea(line, page, document))
            {
                line.setTag(PaperTags.USELESS);
            }

            if (!isTitleFound)
            {
                isTitleFound = isPotentialTitle(line, page, document);
                if (!isTitleFound)
                {
                    line.setTag(PaperTags.USELESS);
                }
            }
        }
    }

    private boolean isPotentialTitle(Line line, Page page, Document document)
    {
        return line.getY() < page.getHeight() * .75
                && getAlignment(line, page, document) != Alignment.RIGHT;

    }

    private Alignment getAlignment(Line line, Page page, Document document)
    {
        int left = line.getFirstX() - document.getLeftMargin();
        int right = page.getWidth() - document.getRightMargin() - line.getLastX();
        int offset = 20;
        if (line.getLastX() - line.getFirstX() > 300) offset += 20;
        if (left - right > offset) return Alignment.RIGHT;
        else if (right - left > offset) return Alignment.LEFT;
        return Alignment.CENTER;
    }

    private boolean isInsideContentArea(Line line, Page page, Document document)
    {
        return line.getFirstX() > document.getLeftMargin() - 20
                && line.getLastX() < page.getWidth() - document.getRightMargin() + 30
                && line.getY() > document.getTopMargin()
                && line.getY() < page.getHeight() - document.getBottomMargin();
    }
}

