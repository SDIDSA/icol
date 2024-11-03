package org.luke.iconGrab;

import java.awt.image.BufferedImage;
import java.io.File;

public class DesktopIcon {
    private File link;
    private BufferedImage icon;
    private File output;

    public DesktopIcon(File link, BufferedImage icon, File output) {
        this.link = link;
        this.icon = icon;
        this.output = output;
    }

    public BufferedImage getIcon() {
        return icon;
    }

    public File getLink() {
        return link;
    }

    public File getOutput() {
        return output;
    }
}