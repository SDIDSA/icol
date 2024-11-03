package org.luke.iconGrab;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class Desktop {

    private static final Random r = new Random();

    public static List<File> getDesktopLinks() {
        File desktop = FileSystemView.getFileSystemView().getHomeDirectory();
        File pesktop = new File("C:\\Users\\Public\\Desktop");
        List<File> links = Arrays.asList(Objects.requireNonNull(desktop.listFiles(
                (_, name) -> name.endsWith(".lnk") || name.endsWith(".url"))));
        List<File> pinks = Arrays.asList(Objects.requireNonNull(pesktop.listFiles(
                (_, name) -> name.endsWith(".lnk") || name.endsWith(".url"))));

        ArrayList<File> all = new ArrayList<>(links);
        all.addAll(pinks);
        return all;
    }

    public static void getDesktopIcons(Label status, ProgressBar pb, Consumer<List<DesktopIcon>> onRes) {
        new Thread(() -> {
            Platform.runLater(() -> status.setText("Extracting desktop icons"));
            List<File> links = getDesktopLinks();

            File saveTo = new File(System.getProperty("java.io.tmpdir") + "/icol_icons_" + r.nextInt(9999999));
            saveTo.mkdir();
            saveTo.deleteOnExit();

            ArrayList<DesktopIcon> res = new ArrayList<>();
            for(int i = 0; i < links.size(); i++) {
                int finalI = i;
                Platform.runLater(() -> pb.setProgress((double) finalI / links.size()));
                File link = links.get(i);
                String name = link.getName();
                name = name.substring(0, name.lastIndexOf("."));

                File iconFile = new File(saveTo.getAbsolutePath().concat("/").concat(name).concat(".ico"));
                iconFile.deleteOnExit();

                BufferedImage origin = ShellImageExtractor.getIcon(link);
                res.add(new DesktopIcon(link, origin, iconFile));
            }
            Platform.runLater(() -> status.setText("Done"));
            Platform.runLater(() -> pb.setProgress(-1));
            onRes.accept(res);
        }).start();
    }
}
