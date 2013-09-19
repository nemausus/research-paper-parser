package in.codehub.pdfreader;

import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.exceptions.InvalidPasswordException;
import org.apache.pdfbox.exceptions.WrappedIOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.pagenavigation.PDThreadBead;
import org.apache.pdfbox.util.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

class PDFParser extends PDFStreamEngine
{
    private static final String thisClassName = "pdftextstripper";
    private static float DEFAULT_INDENT_THRESHOLD = 2.0f;
    private static float DEFAULT_DROP_THRESHOLD = 2.5f;

    //enable the ability to set the default indent/drop thresholds
    //with -D system properties:
    //    pdftextstripper.indent
    //    pdftextstripper.drop
    static
    {
        String prop = thisClassName + ".indent";
        String s = System.getProperty(prop);
        if (s != null && s.length() > 0)
        {
            try
            {
                DEFAULT_INDENT_THRESHOLD = Float.parseFloat(s);
            } catch (NumberFormatException nfe)
            {
                //ignore and use default
            }
        }
        prop = thisClassName + ".drop";
        s = System.getProperty(prop);
        if (s != null && s.length() > 0)
        {
            try
            {
                DEFAULT_DROP_THRESHOLD = Float.parseFloat(s);
            } catch (NumberFormatException nfe)
            {
                //ignore and use default
            }
        }
    }

    private int currentPageNo = 0;
    private int startPage = 1;
    private int endPage = Integer.MAX_VALUE;
    private PDOutlineItem startBookmark = null;
    private int startBookmarkPageNumber = -1;
    private PDOutlineItem endBookmark = null;
    private int endBookmarkPageNumber = -1;
    private boolean suppressDuplicateOverlappingText = true;
    private boolean shouldSeparateByBeads = true;
    private boolean sortByPosition = false;

    private float indentThreshold = DEFAULT_INDENT_THRESHOLD;
    private float dropThreshold = DEFAULT_DROP_THRESHOLD;

    // We will need to estimate where to add spaces.
    // These are used to help guess.
    private float spacingTolerance = .5f;
    private float averageCharTolerance = .3f;

    private List<PDThreadBead> pageArticles = null;
    protected Vector<List<TextPosition>> charactersByArticle = new Vector<List<TextPosition>>();

    private Map<String, TreeMap<Float, TreeSet<Float>>> characterListMapping =
            new HashMap<String, TreeMap<Float, TreeSet<Float>>>();

    protected PDDocument document;
    private TextNormalize normalize;
    private boolean inParagraph;
    private PDFParserListener listener;

    PDFParser(TextNormalize normalize) throws IOException
    {
        super(ResourceLoader.loadProperties(
                "org/apache/pdfbox/resources/PDFTextStripper.properties", true));
        this.normalize = normalize;
        this.listener = new PDFParserListener()
        {
        };
    }

    public void setListener(PdfReader listener)
    {
        this.listener = listener;
    }

    void parse(COSDocument doc) throws IOException
    {
        parse(new PDDocument(doc));
    }

    public void resetEngine()
    {
        super.resetEngine();
        currentPageNo = 0;
    }

    void parse(PDDocument doc) throws IOException
    {
        resetEngine();
        document = doc;
        startDocument(document);

        if (document.isEncrypted())
        {
            // We are expecting non-encrypted documents here, but it is common
            // for users to pass in a document that is encrypted with an empty
            // password (such a document appears to not be encrypted by
            // someone viewing the document, thus the confusion).  We will
            // attempt to decrypt with the empty password to handle this case.
            //
            try
            {
                document.decrypt("");
            } catch (CryptographyException e)
            {
                throw new WrappedIOException("Error decrypting document, details: ", e);
            } catch (InvalidPasswordException e)
            {
                throw new WrappedIOException("Error: document is encrypted", e);
            }
        }

        processPages(document.getDocumentCatalog().getAllPages());
        endDocument(document);
    }

    protected void processPages(List<COSObjectable> pages) throws IOException
    {
        if (startBookmark != null)
        {
            startBookmarkPageNumber = getPageNumber(startBookmark, pages);
        }

        if (endBookmark != null)
        {
            endBookmarkPageNumber = getPageNumber(endBookmark, pages);
        }

        if (startBookmarkPageNumber == -1 && startBookmark != null &&
                endBookmarkPageNumber == -1 && endBookmark != null &&
                startBookmark.getCOSObject() == endBookmark.getCOSObject())
        {
            //this is a special case where both the start and end bookmark
            //are the same but point to nothing.  In this case
            //we will not extract any getText.
            startBookmarkPageNumber = 0;
            endBookmarkPageNumber = 0;
        }

        for (COSObjectable page : pages)
        {
            PDPage nextPage = (PDPage) page;
            PDStream contentStream = nextPage.getContents();
            currentPageNo++;
            if (contentStream != null)
            {
                COSStream contents = contentStream.getStream();
                processPage(nextPage, contents);
            }
        }
    }

    private int getPageNumber(PDOutlineItem bookmark, List<COSObjectable> allPages) throws IOException
    {
        int pageNumber = -1;
        PDPage page = bookmark.findDestinationPage(document);
        if (page != null)
        {
            pageNumber = allPages.indexOf(page) + 1;//use one based indexing
        }
        return pageNumber;
    }

    protected void processPage(PDPage page, COSStream content) throws IOException
    {
        if (currentPageNo >= startPage && currentPageNo <= endPage &&
                (startBookmarkPageNumber == -1 || currentPageNo >= startBookmarkPageNumber) &&
                (endBookmarkPageNumber == -1 || currentPageNo <= endBookmarkPageNumber))
        {
            startPage(page, currentPageNo);
            pageArticles = page.getThreadBeads();
            int numberOfArticleSections = 1 + pageArticles.size() * 2;
            if (!shouldSeparateByBeads)
            {
                numberOfArticleSections = 1;
            }
            int originalSize = charactersByArticle.size();
            charactersByArticle.setSize(numberOfArticleSections);
            for (int i = 0; i < numberOfArticleSections; i++)
            {
                if (numberOfArticleSections < originalSize)
                {
                    charactersByArticle.get(i).clear();
                }
                else
                {
                    charactersByArticle.set(i, new ArrayList<TextPosition>());
                }
            }

            characterListMapping.clear();
            processStream(page, page.findResources(), content);
            parsePage();
            endPage(page);
        }

    }


    private static final float ENDOFLASTTEXTX_RESET_VALUE = -1;
    private static final float MAXYFORLINE_RESET_VALUE = -Float.MAX_VALUE;
    private static final float EXPECTEDSTARTOFNEXTWORDX_RESET_VALUE = -Float.MAX_VALUE;
    private static final float MAXHEIGHTFORLINE_RESET_VALUE = -1;
    private static final float MINYTOPFORLINE_RESET_VALUE = Float.MAX_VALUE;
    private static final float LASTWORDSPACING_RESET_VALUE = -1;

    protected void parsePage() throws IOException
    {
        float maxYForLine = MAXYFORLINE_RESET_VALUE;
        float minYTopForLine = MINYTOPFORLINE_RESET_VALUE;
        float endOfLastTextX = ENDOFLASTTEXTX_RESET_VALUE;
        float lastWordSpacing = LASTWORDSPACING_RESET_VALUE;
        float maxHeightForLine = MAXHEIGHTFORLINE_RESET_VALUE;
        PositionWrapper lastPosition = null;
        PositionWrapper lastLineStartPosition = null;

        boolean startOfPage = true;//flag to indicate start of page
        boolean startOfArticle;

        for (List<TextPosition> textList : charactersByArticle)
        {
            if (getSortByPosition())
            {
                TextPositionComparator comparator = new TextPositionComparator();
                Collections.sort(textList, comparator);
            }

            startArticle();
            startOfArticle = true;

            List<TextPosition> line = new ArrayList<TextPosition>();

            Iterator<TextPosition> textIter = textList.iterator();    // start from the beginning again
            //Keeps track of the previous average character width
            float previousAveCharWidth = -1;
            while (textIter.hasNext())
            {
                TextPosition position = textIter.next();
                PositionWrapper current = new PositionWrapper(position);
                String characterValue = position.getCharacter();

                //Resets the average character width when we see a change in font
                // or a change in the font size
                if (lastPosition != null && ((position.getFont() != lastPosition.getTextPosition().getFont())
                        || (position.getFontSize() != lastPosition.getTextPosition().getFontSize())))
                {
                    previousAveCharWidth = -1;
                }

                float positionX;
                float positionY;
                float positionWidth;
                float positionHeight;

                if (getSortByPosition())
                {
                    positionX = position.getXDirAdj();
                    positionY = position.getYDirAdj();
                    positionWidth = position.getWidthDirAdj();
                    positionHeight = position.getHeightDir();
                }
                else
                {
                    positionX = position.getX();
                    positionY = position.getY();
                    positionWidth = position.getWidth();
                    positionHeight = position.getHeight();
                }

                //The current amount of characters in a word
                int wordCharCount = position.getIndividualWidths().length;

                float wordSpacing = position.getWidthOfSpace();
                float deltaSpace;
                if ((wordSpacing == 0) || (wordSpacing == Float.NaN))
                {
                    deltaSpace = Float.MAX_VALUE;
                }
                else
                {
                    if (lastWordSpacing < 0)
                    {
                        deltaSpace = (wordSpacing * getSpacingTolerance());
                    }
                    else
                    {
                        deltaSpace = (((wordSpacing + lastWordSpacing) / 2f) * getSpacingTolerance());
                    }
                }

                float averageCharWidth;
                if (previousAveCharWidth < 0)
                {
                    averageCharWidth = (positionWidth / wordCharCount);
                }
                else
                {
                    averageCharWidth = (previousAveCharWidth + (positionWidth / wordCharCount)) / 2f;
                }
                float deltaCharWidth = (averageCharWidth * getAverageCharTolerance());

                //Compares the values obtained by the average method and the wordSpacing method and picks
                //the smaller number.
                float expectedStartOfNextWordX = EXPECTEDSTARTOFNEXTWORDX_RESET_VALUE;
                if (endOfLastTextX != ENDOFLASTTEXTX_RESET_VALUE)
                {
                    if (deltaCharWidth > deltaSpace)
                    {
                        expectedStartOfNextWordX = endOfLastTextX + deltaSpace;
                    }
                    else
                    {
                        expectedStartOfNextWordX = endOfLastTextX + deltaCharWidth;
                    }
                }

                if (lastPosition != null)
                {
                    if (startOfArticle)
                    {
                        lastPosition.setArticleStart();
                        startOfArticle = false;
                    }
                    // RDD - Here we determine whether this getText object is on the current
                    // line.  We use the lastBaselineFontSize to handle the superscript
                    // case, and the size of the current font to handle the subscript case.
                    // Text must overlap with the last rendered baseline getText by at least
                    // a small amount in order to be considered as being on the same line.

                    if (!overlap(positionY, positionHeight, maxYForLine, maxHeightForLine))
                    {
                        newline(line);
                        line.clear();

                        lastLineStartPosition =
                                handleLineSeparation(current, lastPosition, lastLineStartPosition, maxHeightForLine);

                        endOfLastTextX = ENDOFLASTTEXTX_RESET_VALUE;
                        expectedStartOfNextWordX = EXPECTEDSTARTOFNEXTWORDX_RESET_VALUE;
                        maxYForLine = MAXYFORLINE_RESET_VALUE;
                        maxHeightForLine = MAXHEIGHTFORLINE_RESET_VALUE;
                        minYTopForLine = MINYTOPFORLINE_RESET_VALUE;
                    }

                    //Test if our TextPosition starts after a new word would be expected to start.
                    if (expectedStartOfNextWordX != EXPECTEDSTARTOFNEXTWORDX_RESET_VALUE
                            && expectedStartOfNextWordX < positionX &&
                            //only bother adding a space if the last character was not a space
                            lastPosition.getTextPosition().getCharacter() != null &&
                            !lastPosition.getTextPosition().getCharacter().endsWith(" "))
                    {
                        line.add(WordSeparator.getSeparator());
                    }
                }

                if (positionY >= maxYForLine)
                {
                    maxYForLine = positionY;
                }

                // RDD - endX is what PDF considers to be the x coordinate of the
                // end position of the getText.  We use it in computing our metrics below.
                endOfLastTextX = positionX + positionWidth;

                // add it to the list
                if (characterValue != null)
                {
                    if (startOfPage && lastPosition == null)
                    {
                        startParagraph();//not sure this is correct for RTL?
                    }
                    line.add(position);
                }
                maxHeightForLine = Math.max(maxHeightForLine, positionHeight);
                minYTopForLine = Math.min(minYTopForLine, positionY - positionHeight);
                lastPosition = current;
                if (startOfPage)
                {
                    lastPosition.setParagraphStart();
                    lastPosition.setLineStart();
                    lastLineStartPosition = lastPosition;
                    startOfPage = false;
                }
                lastWordSpacing = wordSpacing;
                previousAveCharWidth = averageCharWidth;
            }

            // print the final line
            if (line.size() > 0)
            {
                newline(line);
                endParagraph();
            }

            endArticle();
        }
    }

    private boolean overlap(float y1, float height1, float y2, float height2)
    {
        return within(y1, y2, .1f) || (y2 <= y1 && y2 >= y1 - height1) ||
                (y1 <= y2 && y1 >= y2 - height2);
    }

    private boolean within(float first, float second, float variance)
    {
        return second < first + variance && second > first - variance;
    }

    protected void processTextPosition(TextPosition text)
    {
        boolean showCharacter = true;
        if (suppressDuplicateOverlappingText)
        {
            showCharacter = false;
            String textCharacter = text.getCharacter();
            float textX = text.getX();
            float textY = text.getY();
            TreeMap<Float, TreeSet<Float>> sameTextCharacters = characterListMapping.get(textCharacter);
            if (sameTextCharacters == null)
            {
                sameTextCharacters = new TreeMap<Float, TreeSet<Float>>();
                characterListMapping.put(textCharacter, sameTextCharacters);
            }

            // RDD - Here we compute the value that represents the end of the rendered
            // getText.  This value is used to determine whether subsequent getText rendered
            // on the same line overwrites the current getText.
            //
            // We subtract any positive padding to handle cases where extreme amounts
            // of padding are applied, then backed off (not sure why this is done, but there
            // are cases where the padding is on the order of 10x the character width, and
            // the TJ just backs up to compensate after each character).  Also, we subtract
            // an amount to allow for kerning (a percentage of the width of the last
            // character).
            //
            boolean suppressCharacter = false;
            float tolerance = (text.getWidth() / textCharacter.length()) / 3.0f;

            SortedMap<Float, TreeSet<Float>> xMatches =
                    sameTextCharacters.subMap(textX - tolerance, textX + tolerance);
            for (TreeSet<Float> xMatch : xMatches.values())
            {
                SortedSet<Float> yMatches =
                        xMatch.subSet(textY - tolerance, textY + tolerance);
                if (!yMatches.isEmpty())
                {
                    suppressCharacter = true;
                    break;
                }
            }

            if (!suppressCharacter)
            {
                TreeSet<Float> ySet = sameTextCharacters.get(textX);
                if (ySet == null)
                {
                    ySet = new TreeSet<Float>();
                    sameTextCharacters.put(textX, ySet);
                }
                ySet.add(textY);
                showCharacter = true;
            }
        }

        if (showCharacter)
        {
            //if we are showing the character then we need to determine which
            //article it belongs to.
            int foundArticleDivisionIndex = -1;
            int notFoundButFirstLeftAndAboveArticleDivisionIndex = -1;
            int notFoundButFirstLeftArticleDivisionIndex = -1;
            int notFoundButFirstAboveArticleDivisionIndex = -1;
            float x = text.getX();
            float y = text.getY();
            if (shouldSeparateByBeads)
            {
                for (int i = 0; i < pageArticles.size() && foundArticleDivisionIndex == -1; i++)
                {
                    PDThreadBead bead = pageArticles.get(i);
                    if (bead != null)
                    {
                        PDRectangle rect = bead.getRectangle();
                        if (rect.contains(x, y))
                        {
                            foundArticleDivisionIndex = i * 2 + 1;
                        }
                        else if ((x < rect.getLowerLeftX() ||
                                y < rect.getUpperRightY()) &&
                                notFoundButFirstLeftAndAboveArticleDivisionIndex == -1)
                        {
                            notFoundButFirstLeftAndAboveArticleDivisionIndex = i * 2;
                        }
                        else if (x < rect.getLowerLeftX() &&
                                notFoundButFirstLeftArticleDivisionIndex == -1)
                        {
                            notFoundButFirstLeftArticleDivisionIndex = i * 2;
                        }
                        else if (y < rect.getUpperRightY() &&
                                notFoundButFirstAboveArticleDivisionIndex == -1)
                        {
                            notFoundButFirstAboveArticleDivisionIndex = i * 2;
                        }
                    }
                    else
                    {
                        foundArticleDivisionIndex = 0;
                    }
                }
            }
            else
            {
                foundArticleDivisionIndex = 0;
            }
            int articleDivisionIndex = -1;
            if (foundArticleDivisionIndex != -1)
            {
                articleDivisionIndex = foundArticleDivisionIndex;
            }
            else if (notFoundButFirstLeftAndAboveArticleDivisionIndex != -1)
            {
                articleDivisionIndex = notFoundButFirstLeftAndAboveArticleDivisionIndex;
            }
            else if (notFoundButFirstLeftArticleDivisionIndex != -1)
            {
                articleDivisionIndex = notFoundButFirstLeftArticleDivisionIndex;
            }
            else if (notFoundButFirstAboveArticleDivisionIndex != -1)
            {
                articleDivisionIndex = notFoundButFirstAboveArticleDivisionIndex;
            }
            else
            {
                articleDivisionIndex = charactersByArticle.size() - 1;
            }

            List<TextPosition> textList = charactersByArticle.get(articleDivisionIndex);

            if (textList.isEmpty())
            {
                textList.add(text);
            }
            else
            {
                TextPosition previousTextPosition = textList.get(textList.size() - 1);
                if (text.isDiacritic() && previousTextPosition.contains(text))
                {
                    previousTextPosition.mergeDiacritic(text, normalize);
                }
                else if (previousTextPosition.isDiacritic() && text.contains(previousTextPosition))
                {
                    text.mergeDiacritic(previousTextPosition, normalize);
                    textList.remove(textList.size() - 1);
                    textList.add(text);
                }
                else
                {
                    textList.add(text);
                }
            }
        }
    }

    int getStartPage()
    {
        return startPage;
    }

    void setStartPage(int startPageValue)
    {
        startPage = startPageValue;
    }

    int getEndPage()
    {
        return endPage;
    }

    void setEndPage(int endPageValue)
    {
        endPage = endPageValue;
    }

    boolean getSuppressDuplicateOverlappingText()
    {
        return suppressDuplicateOverlappingText;
    }

    protected int getCurrentPageNo()
    {
        return currentPageNo;
    }

    protected Vector<List<TextPosition>> getCharactersByArticle()
    {
        return charactersByArticle;
    }

    void setSuppressDuplicateOverlappingText(
            boolean suppressDuplicateOverlappingTextValue)
    {
        this.suppressDuplicateOverlappingText = suppressDuplicateOverlappingTextValue;
    }

    boolean getSeparateByBeads()
    {
        return shouldSeparateByBeads;
    }

    void setShouldSeparateByBeads(boolean aShouldSeparateByBeads)
    {
        this.shouldSeparateByBeads = aShouldSeparateByBeads;
    }

    PDOutlineItem getEndBookmark()
    {
        return endBookmark;
    }

    void setEndBookmark(PDOutlineItem aEndBookmark)
    {
        endBookmark = aEndBookmark;
    }

    PDOutlineItem getStartBookmark()
    {
        return startBookmark;
    }

    void setStartBookmark(PDOutlineItem aStartBookmark)
    {
        startBookmark = aStartBookmark;
    }

    boolean getSortByPosition()
    {
        return sortByPosition;
    }

    void setSortByPosition(boolean newSortByPosition)
    {
        sortByPosition = newSortByPosition;
    }

    float getSpacingTolerance()
    {
        return spacingTolerance;
    }

    void setSpacingTolerance(float spacingToleranceValue)
    {
        this.spacingTolerance = spacingToleranceValue;
    }

    float getAverageCharTolerance()
    {
        return averageCharTolerance;
    }

    void setAverageCharTolerance(float averageCharToleranceValue)
    {
        this.averageCharTolerance = averageCharToleranceValue;
    }


    float getIndentThreshold()
    {
        return indentThreshold;
    }

    void setIndentThreshold(float indentThresholdValue)
    {
        indentThreshold = indentThresholdValue;
    }

    float getDropThreshold()
    {
        return dropThreshold;
    }

    void setDropThreshold(float dropThresholdValue)
    {
        dropThreshold = dropThresholdValue;
    }


    protected String inspectFontEncoding(String str)
    {
        if (!sortByPosition || str == null || str.length() < 2)
        {
            return str;
        }
        for (int i = 0; i < str.length(); ++i)
        {
            if (Character.getDirectionality(str.charAt(i))
                    != Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC)
            {
                return str;
            }
        }

        StringBuilder reversed = new StringBuilder(str.length());
        for (int i = str.length() - 1; i >= 0; --i)
        {
            reversed.append(str.charAt(i));
        }
        return reversed.toString();
    }

    protected PositionWrapper handleLineSeparation(PositionWrapper current,
                                                   PositionWrapper lastPosition, PositionWrapper lastLineStartPosition, float maxHeightForLine)
            throws IOException
    {
        current.setLineStart();
        isParagraphSeparation(current, lastPosition, lastLineStartPosition, maxHeightForLine);
        lastLineStartPosition = current;
        if (current.isParagraphStart())
        {
            if (lastPosition.isArticleStart())
            {
                startParagraph();
            }
            else
            {
                endLine();
                endParagraph();
                startParagraph();
            }
        }
        else
        {
            endLine();
        }
        return lastLineStartPosition;
    }

    protected void isParagraphSeparation(PositionWrapper position,
                                         PositionWrapper lastPosition, PositionWrapper lastLineStartPosition, float maxHeightForLine)
    {
        boolean result = false;
        if (lastLineStartPosition == null)
        {
            result = true;
        }
        else
        {
            float yGap = Math.abs(position.getTextPosition().getYDirAdj() -
                    lastPosition.getTextPosition().getYDirAdj());
            float xGap = (position.getTextPosition().getXDirAdj() -
                    lastLineStartPosition.getTextPosition().getXDirAdj());//do we need to flip this for rtl?
            if (yGap > (getDropThreshold() * maxHeightForLine))
            {
                result = true;
            }
            else if (xGap > (getIndentThreshold() * position.getTextPosition().getWidthOfSpace()))
            {
                //getText is indented, but try to screen for hanging indent
                if (!lastLineStartPosition.isParagraphStart())
                {
                    result = true;
                }
                else
                {
                    position.setHangingIndent();
                }
            }
            else if (xGap < -position.getTextPosition().getWidthOfSpace())
            {
                //getText is left of previous line. Was it a hanging indent?
                if (!lastLineStartPosition.isParagraphStart())
                {
                    result = true;
                }
            }
            else if (Math.abs(xGap) < (0.25 * position.getTextPosition().getWidth()))
            {
                //current horizontal position is within 1/4 a char of the last
                //linestart.  We'll treat them as lined up.
                if (lastLineStartPosition.isHangingIndent())
                {
                    position.setHangingIndent();
                }
                else if (lastLineStartPosition.isParagraphStart())
                {
                    //check to see if the previous line looks like
                    //any of a number of standard list item formats
                    Pattern liPattern = matchListItemPattern(lastLineStartPosition);
                    if (liPattern != null)
                    {
                        Pattern currentPattern = matchListItemPattern(position);
                        if (liPattern == currentPattern)
                        {
                            result = true;
                        }
                    }
                }
            }
        }
        if (result)
        {
            position.setParagraphStart();
        }
    }

    protected Pattern matchListItemPattern(PositionWrapper pw)
    {
        TextPosition tp = pw.getTextPosition();
        String txt = tp.getCharacter();
        return matchPattern(txt, getListItemPatterns());
    }

    private static final String[] LIST_ITEM_EXPRESSIONS = {
            "\\.",
            "\\d+\\.",
            "\\[\\d+\\]",
            "\\d+\\)",
            "[A-Z]\\.",
            "[a-z]\\.",
            "[A-Z]\\)",
            "[a-z]\\)",
            "[IVXL]+\\.",
            "[ivxl]+\\.",

    };

    private List<Pattern> listOfPatterns = null;

    protected void setListItemPatterns(List<Pattern> patterns)
    {
        listOfPatterns = patterns;
    }


    protected List<Pattern> getListItemPatterns()
    {
        if (listOfPatterns == null)
        {
            listOfPatterns = new ArrayList<Pattern>();
            for (String expression : LIST_ITEM_EXPRESSIONS)
            {
                Pattern p = Pattern.compile(expression);
                listOfPatterns.add(p);
            }
        }
        return listOfPatterns;
    }

    protected static final Pattern matchPattern(String string, List<Pattern> patterns)
    {
        Pattern matchedPattern = null;
        for (Pattern p : patterns)
        {
            if (p.matcher(string).matches())
            {
                return p;
            }
        }
        return matchedPattern;
    }

    private void startDocument(PDDocument document)
    {
        listener.startDocument(document);
    }

    private void endDocument(PDDocument document)
    {
        listener.endDocument(document);
    }

    private void startArticle()
    {
        listener.startArticle();
    }

    private void endArticle()
    {
        listener.endArticle();
    }

    private void endLine()
    {
        listener.endLine();
    }

    private void startPage(PDPage page, int pageNo)
    {
        listener.startPage(page, pageNo);
    }

    private void endPage(PDPage page)
    {
        listener.endPage(page);
    }

    private void startParagraph()
    {
        if (inParagraph)
            endParagraph();
        inParagraph = true;
        listener.startParagraph();
    }

    private void endParagraph()
    {
        inParagraph = false;
        listener.endParagraph();
    }

    private void newline(List<TextPosition> line)
    {
        listener.newLine(line);
    }
}
