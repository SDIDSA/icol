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
                resizeOp.setFilter(ResampleFilters.getLanczos3Filter());
                return resizeOp.filter(Objects.requireNonNull(cropTransparency(ImageIO.read(tem))), null);
            }
            return null;
        }catch(Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static BufferedImage cropTransparency(BufferedImage image) {
        BufferedImage cropped = ImageCropper.cropTransparency(image);

        int exPad = 1;
        BufferedImage img = new BufferedImage(cropped.getWidth() + exPad * 2, cropped.getHeight() + exPad * 2, cropped.getType());
        img.getGraphics().drawImage(cropped, exPad, exPad, null);
        return img;
    }

}
