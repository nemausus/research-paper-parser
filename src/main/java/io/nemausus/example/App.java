package io.nemausus.example;

import java.io.IOException;

public class App
{
    public static String pdfFolder = "/home/kumarishan/research-papers/";
    public static String docFolder = "/home/kumarishan/research-papers/results/docs/";
    public static String paperFolder = "/home/kumarishan/research-papers/results/papers/";

    public static void main(String[] args) throws IOException
    {
        long startTime = System.nanoTime();
        System.out.println("Time taken in seconds: " + (System.nanoTime() - startTime) / 1000000000);
    }
}
