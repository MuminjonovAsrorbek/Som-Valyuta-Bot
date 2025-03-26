package org.example.File;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileUtil {
    public static Path getResourceFilePath(String fileName) {
        try {
            ClassLoader classLoader = FileUtil.class.getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream(fileName);

            if (inputStream == null) {
                throw new IllegalArgumentException("Fayl topilmadi: " + fileName);
            }

            // Faylni vaqtincha joyga saqlash
            Path tempFile = Files.createTempFile("flags", ".txt");

            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
