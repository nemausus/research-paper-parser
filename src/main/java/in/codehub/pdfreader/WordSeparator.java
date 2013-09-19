package in.codehub.pdfreader;

import org.apache.pdfbox.util.TextPosition;

class WordSeparator extends TextPosition
{
    private static final WordSeparator separator = new WordSeparator();

    private WordSeparator()
    {
    }

    static WordSeparator getSeparator()
    {
        return separator;
    }
}
