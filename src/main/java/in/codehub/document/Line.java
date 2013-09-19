package in.codehub.document;

public class Line
{
    private String text;
    private int fontSize;
    private int firstX;
    private int lastX;
    private int y;
    private String tag;

    public Line(String text, int fontSize, int firstX, int lastX, int y)
    {
        this.text = text;
        this.fontSize = fontSize;
        this.firstX = firstX;
        this.lastX = lastX;
        this.y = y;
    }

    public Line()
    {

    }

    public String getText()
    {
        return text;
    }

    public int getFirstX()
    {
        return firstX;
    }

    public int getLastX()
    {
        return lastX;
    }

    public int getY()
    {
        return y;
    }

    public int getFontSize()
    {
        return fontSize;
    }

    public int length()
    {
        return text.length();
    }

    public void setTag(String tag)
    {
        this.tag = tag;
    }

    public String tag()
    {
        return tag;
    }
}
