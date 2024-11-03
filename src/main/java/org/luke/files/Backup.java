package org.luke.files;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.JSONObject;
import org.luke.colorize.Colorize;
import org.luke.iconGrab.Desktop;
import org.luke.iconGrab.DesktopIcon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class Backup {

    private static final Random r = new Random();

    public static void restore(Stage ps, Label status, ProgressBar pb) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Desktop Icons Backup", "*.dib"));
        File saveTo = fc.showOpenDialog(ps);
        if (saveTo != null) {
            restore(saveTo, status, pb);
        }
    }

    private static void restore(File file, Label status, ProgressBar pb) {
        new Thread(() -> {
            JSONObject obj = new JSONObject(Objects.requireNonNull(FileDealer.read(file)));
            File saveTo = new File(System.getProperty("java.io.tmpdir") + "/icol_icons_" + r.nextInt(9999999));
            saveTo.mkdir();
            saveTo.deleteOnExit();

            ArrayList<DesktopIcon> icons = new ArrayList<>();

            Desktop.getDesktopLinks().forEach(link -> {
                String name = link.getName();
                name = name.substring(0, name.lastIndexOf("."));
                if (obj.has(name) && !obj.isNull(name)) {
                    String val = obj.getString(name);
                    BufferedImage img = base64StringToImg(val);

                    File iconFile = new File(saveTo.getAbsolutePath().concat("/").concat(name).concat(".ico"));

                    icons.add(new DesktopIcon(link, img, iconFile));
                }
            });

            Platform.runLater(() -> status.setText("Restoring icons..."));

            for(int i = 0; i < icons.size(); i++) {
                int finalI = i;
                Platform.runLater(() -> pb.setProgress((double) finalI / icons.size()));
                Colorize.apply(icons.get(i));
            }
            Platform.runLater(() -> status.setText("Done"));
            Platform.runLater(() -> pb.setProgress(-1));
        }).start();
    }

    public static void backup(Stage ps, Label status, ProgressBar pb) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Desktop Icons Backup", "*.dib"));
        File saveTo = fc.showSaveDialog(ps);
        if (saveTo != null) {
            backup(saveTo, status, pb);
        }
    }

    private static void backup(File file, Label status, ProgressBar pb) {
        Desktop.getDesktopIcons(status, pb, icons -> {
            JSONObject obj = new JSONObject();
            icons.forEach(icon -> {
                String name = icon.getLink().getName();
                name = name.substring(0, name.lastIndexOf("."));
                obj.put(name, imgToBase64String(icon.getIcon(), "png"));
            });
            FileDealer.write(obj.toString(), file);
        });
    }

    public static String imgToBase64String(final BufferedImage img, final String formatName) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (final OutputStream b64os = Base64.getEncoder().wrap(os)) {
            ImageIO.write(img, formatName, b64os);
        } catch (final IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        return os.toString();
    }

    public static BufferedImage base64StringToImg(final String base64String) {
        try {
            return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64String)));
        } catch (final IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }
}
