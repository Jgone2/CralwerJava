package com.crawlerExam.kakaoMapCrawler;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

@Slf4j
public class ChromeDriverDownloader {
    public static void main(String[] args) throws IOException {

        // get the path of the chromedriver.exe
        String url = "https://edgedl.me.gvt1.com/edgedl/chrome/chrome-for-testing/121.0.6167.85/mac-x64/chromedriver-mac-x64.zip";
        log.info("Download URL: {}", url);
        // Set the path to the chromedriver.exe
        String outputPath = "/Users/jgone2/Downloads/chromedriver-mac-x64.zip";

        // Download the file
        HttpURLConnection urlConnection = (HttpURLConnection)new URL(url).openConnection();
        try (BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
             FileOutputStream fileOS = new FileOutputStream(outputPath)) {
            byte data[] = new byte[1024];
            int byteContent;
            while ((byteContent = inputStream.read(data, 0, 1024)) != -1) {
                fileOS.write(data, 0, byteContent);
            }
            log.info("Download Successful!");
        } catch (IOException e) {
            log.error("Download Failed! ", e);
        }

        // Check if the target file exists, and if so, delete it
        File chromedriverFile = new File("/Users/jgone2/Downloads/chromedriver");
        if (chromedriverFile.exists()) {
            chromedriverFile.delete();
        }

        // Unzip the file
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(Paths.get(outputPath)))) {
            ZipEntry entry = zis.getNextEntry();
            Path extractPath = Paths.get("/Users/jgone2/Downloads/chromedriver");
            Files.copy(zis, extractPath);
            log.info("Unzip Successful!");
        } catch (ZipException e) {
            log.error("Unzip Failed! ", e);
        }

    }
}
