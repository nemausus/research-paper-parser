package io.nemausus.examples;

import io.nemausus.document.Document;
import io.nemausus.paperparser.Paper;
import io.nemausus.paperparser.PaperParser;
import io.nemausus.pdfreader.PdfReader;

import java.io.IOException;

public class SimpleExample
{
    public Paper run(String filePath) throws IOException
    {
        Document document = PdfReader.getInstance().read(filePath);
        return PaperParser.getInstance().parse(document);
    }
}
