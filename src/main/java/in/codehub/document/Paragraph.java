package in.codehub.document;

import java.util.ArrayList;
import java.util.List;

public class Paragraph
{
    private List<Line> lines = new ArrayList<Line>();

    public int textLength()
    {
        int size = 0;
        for (Line line : lines)
            size += line.length();
        return size;
    }

    public void addLine(Line line)
    {
        lines.add(line);
    }

    public List<Line> getLines()
    {
        return lines;
    }

    public int lineCount()
    {
        return lines.size();
    }

    public String text()
    {
        StringBuilder sb = new StringBuilder();
        for (Line line : lines)
        {
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '-')
                sb.deleteCharAt(sb.length() - 1);
            else if (sb.length() > 0)
                sb.append(" ");
            sb.append(line.getText());
        }
        return sb.toString();
    }

    public int fontSize()
    {
        int fontSize = 0, count = 0;
        for (Line line : lines)
        {
            count += (fontSize == line.getFontSize() ? 1 : -1) * line.getText().length();
            fontSize = (count < 0 ? line.getFontSize() : fontSize);
        }
        return fontSize;
    }
}

