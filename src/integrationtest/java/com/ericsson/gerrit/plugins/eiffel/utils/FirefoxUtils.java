package com.ericsson.gerrit.plugins.eiffel.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

public class FirefoxUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(FirefoxUtils.class);

    private static File tempDownloadDirectory = Files.createTempDir();
    private static String firefoxVersion = "61.0.1";
    private static String firefoxFileName = "firefox-" + firefoxVersion + ".tar.bz2";
    private static String firefoxUrl = "https://ftp.mozilla.org/pub/firefox/releases/61.0.1/linux-x86_64/en-GB/" + firefoxFileName;

    public static FirefoxBinary installFirefoxBinary() {
        String firefoxBZip2FilePath = String.join(
                File.separator, tempDownloadDirectory.getPath(), firefoxFileName);
        downloadFileFromUrlToDestination(firefoxUrl, firefoxBZip2FilePath);

        LOGGER.info("File Path: {}", firefoxBZip2FilePath);
        listFilesAndDirs(tempDownloadDirectory.getPath());
        extractBZip2InDir(firefoxBZip2FilePath, tempDownloadDirectory.getPath());
        listFilesAndDirs(tempDownloadDirectory.getPath());
        File firefoxBinaryFilePath = new File(
                String.join(File.separator, tempDownloadDirectory.getPath(), "firefox", "firefox"));
        listFilesAndDirs(String.join(File.separator, tempDownloadDirectory.getPath(), "firefox"));
        //listFilesAndDirs(String.join(File.separator, tempDownloadDirectory.getPath(), "firefox", "firefox"));
        makeBinFileExecutable(firefoxBinaryFilePath);
        FirefoxBinary firefoxBinary = new FirefoxBinary(firefoxBinaryFilePath);
        return firefoxBinary;
    }

    private static void listFilesAndDirs(String path) {
        File file = new File(path);
        File[] listOfFiles = file.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                LOGGER.info("Files in Path: {}", listOfFiles[i].getName());
              System.out.println("File " + listOfFiles[i].getName());
            } else if (listOfFiles[i].isDirectory()) {
                LOGGER.info("Directory in Path: {}", listOfFiles[i].getName());
              System.out.println("Directory " + listOfFiles[i].getName());
            }
          }
    }

    /**
     * Extracts a BZip2 archive to a destination folder.
     *
     * @param firefoxTarballFilePath
     *            string containing the Firefox BZip2 file path.
     * @param destination
     *            string containing a destination path.
     */
    private static void extractBZip2InDir(final String firefoxBZip2FilePath, String destinationPath) {
        LOGGER.debug("Extracting firefox BZip2 archive...");
        try (TarArchiveInputStream fileInput = new TarArchiveInputStream(
                new BZip2CompressorInputStream(new FileInputStream(firefoxBZip2FilePath)))) {
            TarArchiveEntry entry;
            while ((entry = fileInput.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                final File curfile = new File(destinationPath, entry.getName());
                final File parent = curfile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                final FileOutputStream fileOutput = new FileOutputStream(curfile);
                IOUtils.copy(fileInput, fileOutput);
                fileOutput.close();
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("FileNotFoundException.\nError: {}", e.getMessage());
        } catch (IOException e) {
            LOGGER.error("IOException.\nError: {}", e.getMessage());
        }
    }

    /**
     * Downloads a file from a given URL to a given destination path.
     *
     * @param url
     *            string containing a URL.
     * @param destination
     *            string containing a destination path.
     */
    private static void downloadFileFromUrlToDestination(final String url, final String destination) {
        final File file = new File(destination);
        URL urlObj = null;

        try {
            urlObj = new URL(url);
            LOGGER.debug("Downloading file.\nSource: {}\nDestination: {}", url, destination);
            FileUtils.copyURLToFile(urlObj, file);
        } catch (MalformedURLException e) {
            LOGGER.error("Failed to create URL object.\nURL: {}\nError: {}", url, e.getMessage());
        } catch (IOException e) {
            LOGGER.error("Failed to download file.\nURL: {}\nError: {}", url, e.getMessage());
        }
    }

    /**
     * Makes file executable in filesystem.
     *
     * @param binFile
     *            file to make executable
     */
    private static void makeBinFileExecutable(final File binFile) {
        if (binFile.isFile()) {
            LOGGER.debug("Changing bin file to be executable.\nPath: {}", binFile.getPath());
            binFile.setExecutable(true);
        } else {
            LOGGER.error("Path is not a file.\nPath: {}", binFile.getPath());
        }
    }

}
