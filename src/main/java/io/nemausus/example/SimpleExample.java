package io.nemausus.example;

import io.nemausus.document.Document;
import io.nemausus.paperparser.Paper;
import io.nemausus.paperparser.paperParser;
import io.nemausus.pdfreader.PdfReader;

import java.io.IOException;

public class SimpleExample
{
    public Paper run(String filePath) throws IOException
    {
        Document document = PdfReader.getInstance().buildDocument(filePath);
        return paperParser.getInstance().buildPaper(document);
    }
}
