package in.codehub.document;

import java.util.ArrayList;
import java.util.ListIterator;

public class DocumentIterator
{
    private ListIterator<Page> pageIterator;
    private ListIterator<Paragraph> paragraphIterator;
    private ListIterator<Line> lineIterator;

    private Page currPage;
    private Paragraph currParagraph;
    private Line currLine;

    public DocumentIterator(Document document)
    {
        pageIterator = document.getPages().listIterator();
        paragraphIterator = new ArrayList<Paragraph>().listIterator();
        lineIterator = new ArrayList<Line>().listIterator();
    }

    public boolean hasNextPage()
    {
        return pageIterator.hasNext();
    }

    public boolean hasNextParagraph()
    {
        return paragraphIterator.hasNext() || pageIterator.hasNext();
    }

    public boolean hasNextLine()
    {
        return lineIterator.hasNext() || paragraphIterator.hasNext() || pageIterator.hasNext();
    }

    public Page nextPage()
    {
        currPage = pageIterator.next();
        initParaIterator(currPage);
        return currPage;
    }

    public Paragraph nextParagraph()
    {
        if (!paragraphIterator.hasNext())
            nextPage();
        currParagraph = paragraphIterator.next();
        initLineIterator(currParagraph);
        return currParagraph;
    }

    public Line nextLine()
    {
        if (!lineIterator.hasNext())
            nextParagraph();
        return (currLine = lineIterator.next());
    }

    public Page currPage()
    {
        return currPage;
    }

    public Line currLine()
    {
        return currLine;
    }

    private void initParaIterator(Page page)
    {
        if (page != null)
        {
            paragraphIterator = page.getParagraphs().listIterator();
            if (paragraphIterator.hasNext())
            {
                initLineIterator(paragraphIterator.next());
                paragraphIterator.previous();
            }
        }
    }

    private void initLineIterator(Paragraph paragraph)
    {
        if (paragraph != null)
        {
            lineIterator = paragraph.getLines().listIterator();
        }
    }

    public Paragraph currParagraph()
    {
        return currParagraph;
    }
}

