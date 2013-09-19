package in.codehub.document;

import java.util.ArrayList;
import java.util.List;

public class Document
{
    private int leftMargin;
    private int rightMargin;
    private int topMargin;
    private int bottomMargin;
    private int contentFontSize;
    private List<Page> pages = new ArrayList<Page>();

    private String id;

    public Document()
    {

    }

    public Document(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    public List<Page> getPages()
    {
        return pages;
    }

    public void addPage(Page page)
    {
        pages.add(page);
    }

    public void setMargins(int l, int r, int t, int b)
    {
        this.leftMargin = l;
        this.rightMargin = r;
        this.topMargin = t;
        this.bottomMargin = b;
    }

    public void setContentFontSize(int contentFontSize)
    {
        this.contentFontSize = contentFontSize;
    }

    public int getLeftMargin()
    {
        return leftMargin;
    }

    public int getRightMargin()
    {
        return rightMargin;
    }

    public int getTopMargin()
    {
        return topMargin;
    }

    public int getBottomMargin()
    {
        return bottomMargin;
    }

    public int getContentFontSize()
    {
        return contentFontSize;
    }

    public DocumentIterator iterator()
    {
        return new DocumentIterator(this);
    }

    public void setId(String id)
    {
        this.id = id;
    }
}

