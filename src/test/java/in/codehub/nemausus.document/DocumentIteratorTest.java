package in.codehub.document;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DocumentIteratorTest
{
    private Document document;

    @Before
    public void init()
    {
        document = new Document();
    }

    @Test
    public void emptyDocument()
    {
        DocumentIterator iterator = document.iterator();
        assertFalse(iterator.hasNextLine());
        assertFalse(iterator.hasNextPage());
        assertFalse(iterator.hasNextParagraph());
    }

    @Test
    public void textNextPage()
    {
        page().para(3).page().para(1).page().para(2);
        DocumentIterator iterator = document.iterator();
        StringBuilder sb = new StringBuilder();
        while (iterator.hasNextPage())
        {
            iterator.nextPage();
            sb.append(iterator.nextParagraph().text());
        }
        assertEquals("11 12 131111 12", sb.toString());
    }

    @Test
    public void testNextPara()
    {
        page().para(3).para(1).para(2);
        DocumentIterator iterator = document.iterator();
        StringBuilder sb = new StringBuilder();
        while (iterator.hasNextParagraph())
        {
            iterator.nextParagraph();
            sb.append(iterator.nextLine().getText());
        }
        assertEquals("112131", sb.toString());
    }

    @Test
    public void testNextLine()
    {
        page().para(3).para(1).para(2);
        DocumentIterator iterator = document.iterator();
        StringBuilder sb = new StringBuilder();
        while (iterator.hasNextLine())
        {
            sb.append(iterator.nextLine().getText());
        }
        assertEquals("111213213132", sb.toString());
    }

    @Test
    public void testNextLineWithNextPara()
    {
        page().para(3).para(4).para(2);
        DocumentIterator iterator = document.iterator();
        StringBuilder sb = new StringBuilder();
        while (iterator.hasNextParagraph())
        {
            iterator.nextParagraph();
            sb.append(iterator.nextLine().getText());
            sb.append(iterator.nextLine().getText());
        }
        assertEquals("111221223132", sb.toString());
    }

    private DocumentIteratorTest page()
    {
        document.addPage(new Page(document.getPages().size(), 0, 0));
        return this;
    }

    private DocumentIteratorTest para(int lineCount)
    {
        Page page = document.getPages().get(document.getPages().size() - 1);
        Paragraph paragraph = new Paragraph();
        int paraNo = page.getParagraphs().size();
        for (int i = 0; i < lineCount; ++i)
            paragraph.addLine(new Line(String.valueOf(10 * (paraNo + 1) + i + 1), 0, 0, 0, 0));
        page.addParagraph(paragraph);
        return this;
    }
}

