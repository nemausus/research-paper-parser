package in.codehub.paperparser;

import java.util.ArrayList;
import java.util.List;

public class Paper
{
    private final String id;
    private String abstract_ = "";
    private String title = "";
    private List<String> authors = new ArrayList<String>();
    private List<String> keywords = new ArrayList<String>();
    private List<String> references = new ArrayList<String>();
    private List<String> heading = new ArrayList<String>();

    public Paper(String id)
    {
        this.id = id;
    }

    public void setAbstract(String abs)
    {
        this.abstract_ = abs;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getAbstract()
    {
        return abstract_;
    }

    public String getTitle()
    {
        return title;
    }

    public List<String> getAuthors()
    {
        return authors;
    }

    public List<String> getKeywords()
    {
        return keywords;
    }

    public List<String> getReferences()
    {
        return references;
    }

    public List<String> getHeading()
    {
        return heading;
    }

    public String getId()
    {
        return id;
    }
}
