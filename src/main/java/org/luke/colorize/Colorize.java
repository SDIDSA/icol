package org.luke.colorize;

import javafx.scene.paint.Color;
import org.luke.files.AppData;
import org.luke.iconGrab.DesktopIcon;
import org.luke.files.FileDealer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class Colorize {
    private static File colorize(File input, Color c, boolean tint) {
        String name = input.getName();
        String extension = name.substring(name.lastIndexOf("."));
        name = name.substring(0, name.lastIndexOf("."));

        File output = new File(input.getParentFile().getAbsolutePath() + File.separator + name + "_tinted" + extension);
        output.deleteOnExit();

        if (tint) {
            String colorHex = toHexString(c);

            try {
                new Command("cmd", "/c", "magick \"" + input.getAbsolutePath() + "\" -colorspace gray -fill " + colorHex
                        + " -tint 100 \"" + output.getAbsolutePath() + "\"").execute(getMagick()).waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        } else {
            try {
                BufferedImage img = ImageIO.read(input);
                int w = img.getWidth();
                int h = img.getHeight();
                BufferedImage dyed = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = dyed.createGraphics();
                g.drawImage(img, 0, 0, null);
                g.setComposite(AlphaComposite.SrcAtop);
                g.setColor(new java.awt.Color((float) c.getRed(), (float) c.getGreen(), (float) c.getBlue(),
                        (float) c.getOpacity()));
                g.fillRect(0, 0, w, h);
                g.dispose();
                ImageIO.write(dyed, "png", output);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return output;
    }

    private static void setIcon(File link, File tempIcon) {
        if (!tempIcon.exists()) {
            return;
        }
        File icon = AppData.usableIcon(tempIcon);
        if (!icon.exists()) {
            return;
        }
        String name = link.getName();
        name = name.substring(0, name.lastIndexOf("."));
        String desktop = link.getAbsolutePath().toLowerCase().contains("public") ?
                "Dim obj, path\r\n"
                        + "Set obj = createobject(\"wscript.shell\")\r\n"
                        + "path = obj.specialfolders(\"AllUsersDesktop\")\r\n"
                        + "Set objShell = CreateObject(\"Shell.Application\")\r\n"
                        + "Set objFolder = objShell.NameSpace(path)" :
                "Const DESKTOP = &H10&\r\n"
                        + "Set objFolder = objShell.NameSpace(DESKTOP)";
        String script =  "Set objShell = CreateObject(\"Shell.Application\")\r\n" + desktop + "\r\n"
                + "Set objFolderItem = objFolder.ParseName(\""
                + link.getName() + "\")\r\n" + "Set objShortcut = objFolderItem.GetLink\r\n"
                + "objShortcut.SetIconLocation \"" + icon.getAbsolutePath() + "\", 0\r\n" + "objShortcut.Save";

        System.out.println(script);
        File scriptFile = new File(
                icon.getParentFile().getAbsolutePath().concat("/").concat(name + "_vbscript").concat(".vbs"));
        scriptFile.deleteOnExit();

        FileDealer.write(script, scriptFile);

        try {
            new Command("cmd", "/c", "cscript \"" + scriptFile.getName() + "\"").execute(scriptFile.getParentFile())
                    .waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    public static void apply(DesktopIcon ni, Color c, boolean tint) {
        try {
            File preOut = new File(ni.getOutput().getAbsolutePath().replace(".ico", ".png"));
            preOut.deleteOnExit();
            ImageIO.write(ni.getIcon(), "png", preOut);

            File colorized = colorize(preOut, c, tint);

            new Command("cmd", "/c",
                    "magick \"" + colorized.getAbsolutePath()
                            + "\" -define icon:auto-resize=256,128,96,70,64,48,32,16 \""
                            + ni.getOutput().getAbsolutePath() + "\"")
                    .execute(getMagick()).waitFor();

            setIcon(ni.getLink(), ni.getOutput());
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    public static void apply(DesktopIcon ni) {
        try {
            File preOut = new File(ni.getOutput().getAbsolutePath().replace(".ico", ".png"));
            ImageIO.write(ni.getIcon(), "png", preOut);
            preOut.deleteOnExit();

            new Command("cmd", "/c",
                    "magick \"" + preOut.getAbsolutePath()
                            + "\" -define icon:auto-resize=256,128,96,70,64,48,32,16 \""
                            + ni.getOutput().getAbsolutePath() + "\"")
                    .execute(getMagick()).waitFor();

            setIcon(ni.getLink(), ni.getOutput());
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    private static String format(double val) {
        String in = Integer.toHexString((int) Math.round(val * 255));
        return in.length() == 1 ? "0" + in : in;
    }

    public static String toHexString(Color value) {
        return "#" + (format(value.getRed()) + format(value.getGreen()) + format(value.getBlue())
                + format(value.getOpacity())).toUpperCase();
    }

    private static File getMagick() {
        return new File(URLDecoder.decode(Colorize.class.getResource("/magick.exe").getFile(), StandardCharsets.UTF_8))
                .getParentFile();
    }
}
