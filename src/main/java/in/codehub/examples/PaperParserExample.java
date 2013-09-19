package in.codehub.examples;

import in.codehub.document.Document;
import in.codehub.paperparser.Paper;
import in.codehub.paperparser.PaperParser;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class PaperParserExample
{
    public void run(String inputFolder, String outFolder) throws IOException
    {
        File[] files = new File(inputFolder).listFiles(getFilter());
        PaperParser builder = PaperParser.getInstance();
        ObjectMapper mapper = new ObjectMapper();
        for (final File file : files)
        {
            String inFilePath = file.getAbsolutePath();
            String inFilename = file.getName();
          Document document = mapper.readValue(new File(inFilePath), Document.class);
            Paper paper = builder.parse(document);
            String outFilename = outFolder + StringUtils.removeEndIgnoreCase(inFilename, ".json") + "-paper.json";
            mapper.writeValue(new File(outFilename), paper);
        }
    }

    private FilenameFilter getFilter()
    {
        return new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String name)
            {
                return StringUtils.endsWithIgnoreCase(name, ".json");
            }
        };
    }
}
