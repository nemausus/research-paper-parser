package in.codehub.document;

import java.util.ArrayList;
import java.util.List;

public class Page
{
    private int serialNo;
    private int width;
    private int height;
    private List<Paragraph> paragraphs = new ArrayList<Paragraph>();

    public Page(int serialNo, int width, int height)
    {
        this.serialNo = serialNo;
        this.width = width;
        this.height = height;
    }

    public Page()
    {

    }

    public List<Paragraph> getParagraphs()
    {
        return paragraphs;
    }

    public void addParagraph(Paragraph paragraph)
    {
        paragraphs.add(paragraph);
    }

    public int getSerialNo()
    {
        return serialNo;
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

}
