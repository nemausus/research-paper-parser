package in.codehub.examples;

import in.codehub.document.Document;
import in.codehub.paperparser.Paper;
import in.codehub.paperparser.PaperParser;
import in.codehub.pdfreader.PdfReader;

import java.io.IOException;

public class SimpleExample
{
    public Paper run(String filePath) throws IOException
    {
        Document document = PdfReader.getInstance().read(filePath);
        return PaperParser.getInstance().parse(document);
    }
}
