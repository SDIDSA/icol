package org.luke.iconGrab;

import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ShellImageExtractor {
    private final String executablePath;
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    public ShellImageExtractor(String executablePath) {
        this.executablePath = executablePath;
    }

    public boolean extractIcon(ExtractOptions options) throws Exception {
        List<String> command = buildCommand(options);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        boolean completed = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            throw new Exception("Process timed out after " + DEFAULT_TIMEOUT_SECONDS + " seconds");
        }

        return process.exitValue() == 0;
    }

    private List<String> buildCommand(ExtractOptions options) {
        List<String> command = new ArrayList<>();
        command.add(executablePath);
        command.add(options.getInputPath());

        if (options.getOutputPath() != null) {
            command.add(options.getOutputPath());
        }

        if (options.getSize() != null) {
            command.add(options.getSize());
        }

        if (options.getFormat() != null) {
            command.add(options.getFormat());
        }

        return command;
    }

    // Utility method to get default output path
    private static String getDefaultOutputPath(String inputPath, String format) {
        Path path = Paths.get(inputPath);
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = (dotIndex != -1) ? fileName.substring(0, dotIndex) : fileName;
        return path.getParent().resolve(baseName + "_icon." + (format != null ? format : "png")).toString();
    }

    private static ShellImageExtractor extractor;
    public static BufferedImage getIcon(File file) {
        if(extractor == null) {
            String executable = ShellImageExtractor.class.getResource("/getIcon.exe").getFile().substring(1);
            System.out.println(executable);
            extractor = new ShellImageExtractor(URLDecoder.decode(executable, StandardCharsets.UTF_8));
        }

        try {
            File tem = File.createTempFile("icol_", ".png");
            ExtractOptions options = new ExtractOptions(file.getAbsolutePath())
                    .format("png")
                    .size(256)
                    .outputPath(tem.getAbsolutePath());
            if(extractor.extractIcon(options)) {
                tem.deleteOnExit();
                ResampleOp resizeOp = new ResampleOp(256, 256);
                resizeOp.setFilter(ResampleFilters.getBiCubicHighFreqResponse());
                return resizeOp.filter(Objects.requireNonNull(cropTransparency(ImageIO.read(tem))), null);
            }
            return null;
        }catch(Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static BufferedImage cropTransparency(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int top = height, left = width, bottom = 0, right = 0;

        int pre = width / 10;
        for (int y = pre; y < height - pre; y++) {
            for (int x = pre; x < width - pre; x++) {
                int pixel = image.getRGB(x, y);
                if ((pixel >> 24) != 0x00) {  // If not fully transparent
                    if (x < left) left = x;
                    if (x > right) right = x;
                    if (y < top) top = y;
                    if (y > bottom) bottom = y;
                }
            }
        }
        int pad = Math.min(top, left);
        int size = Math.max(right, bottom);
        if(pad <= pre) {
            pad = 0;
            size = height - 1;
        }else {
            pad-=5;
            size+=5;
        }

        if (top > bottom || left > right) {
            return null;
        }

        return image.getSubimage(pad, pad, size - pad + 1, size - pad + 1);
    }

}
