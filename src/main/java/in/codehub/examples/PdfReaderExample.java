package in.codehub.examples;

import in.codehub.document.Document;
import in.codehub.pdfreader.PdfReader;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class PdfReaderExample
{
    public void run(String inputFolder, String outFolder) throws IOException
    {
        PdfReader pdfReader = PdfReader.getInstance();
        File[] files = new File(inputFolder).listFiles(getFilter());
        ObjectMapper mapper = new ObjectMapper();
        for (final File fileEntry : files)
        {
            String inFilePath = fileEntry.getAbsolutePath();
            String inFilename = fileEntry.getName();
            Document document = pdfReader.read(inFilePath);
            document.setId(inFilename);
            String outFilePath = outFolder + StringUtils.removeEndIgnoreCase(inFilename, ".pdf") + ".json";
            mapper.writeValue(new File(outFilePath), document);
        }
    }

    private FilenameFilter getFilter()
    {
        return new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String name)
            {
                return StringUtils.endsWithIgnoreCase(name, ".pdf");
            }
        };
    }
}
