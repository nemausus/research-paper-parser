package in.codehub.pdfreader;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.TextPosition;

import java.util.List;

abstract class PDFParserListener
{
    void startDocument(PDDocument pdf)
    {
    }

    void endDocument(PDDocument pdf)
    {
    }

    void startPage(PDPage page, int currentPageNo)
    {
    }

    void endPage(PDPage page)
    {
    }

    void startArticle()
    {
    }

    void endArticle()
    {
    }

    void startParagraph()
    {
    }

    void endParagraph()
    {
    }

    void newLine(List<TextPosition> line)
    {
    }

    void endLine()
    {
    }

}
