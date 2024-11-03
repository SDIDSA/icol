package org.luke.iconGrab;

public class ExtractOptions {
    private final String inputPath;
    private String outputPath;
    private String size;
    private String format;

    public ExtractOptions(String inputPath) {
        this.inputPath = inputPath;
    }

    public ExtractOptions outputPath(String outputPath) {
        this.outputPath = outputPath;
        return this;
    }

    public ExtractOptions size(int size) {
        this.size = String.valueOf(size);
        return this;
    }

    public ExtractOptions format(String format) {
        this.format = format;
        return this;
    }

    public String getFormat() {
        return format;
    }

    public String getInputPath() {
        return inputPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public String getSize() {
        return size;
    }
}