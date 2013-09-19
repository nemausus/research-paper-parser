package in.codehub.util;

import java.io.*;
import java.util.UUID;

public class FileUtils
{
    public static void writeToFile(String filePath, String text) throws IOException
    {
        FileWriter fileWriter = new FileWriter(filePath);
        fileWriter.write(text);
        fileWriter.close();
    }

    public static String readFromFile(String filePath) throws IOException
    {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = reader.readLine()) != null)
        {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    public static void renameFiles(String inputFolder, FilenameFilter filenameFilter) throws IOException
    {
        File[] files = new File(inputFolder).listFiles(filenameFilter);
        for (final File fileEntry : files)
        {
            int index = fileEntry.getName().lastIndexOf('.');
            String ext = index > 0 ? fileEntry.getName().substring(index) : "";
            File file2 = new File(UUID.randomUUID().toString() + ext);
            fileEntry.renameTo(file2);
        }
    }
}
