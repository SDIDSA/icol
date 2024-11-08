package org.luke;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.luke.colorize.Colorize;
import org.luke.files.Backup;
import org.luke.iconGrab.Desktop;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;

public class App extends Application {

    @Override
    public void start(Stage ps) throws AWTException {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        for(int i = 1; i <= 6; i++) {
            ps.getIcons().add(new Image(Objects.requireNonNull(
                    App.class.getResourceAsStream("/icon-" + i + ".png"))));
        }

        ColorPicker cp = new ColorPicker();
        Button pick = new Button("pick");

        Stage picker = new Stage(StageStyle.TRANSPARENT);
        picker.setAlwaysOnTop(true);
        ImageView view = new ImageView();
        ImageView preview = new ImageView();

        javafx.scene.shape.Rectangle disp = new javafx.scene.shape.Rectangle();
        disp.setEffect(new DropShadow());
        disp.setStroke(Color.BLACK);
        disp.setStrokeWidth(1);

        StackPane preDisp = new StackPane(disp);
        preDisp.setPadding(new Insets(10));
        preDisp.setAlignment(Pos.BOTTOM_RIGHT);

        Line hor = new Line();
        Line ver = new Line();

        StackPane prePrev = new StackPane(preview, hor, ver, preDisp);
        prePrev.setEffect(new DropShadow());
        prePrev.setMouseTransparent(true);
        prePrev.setBorder(
                new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(1))));

        disp.widthProperty().bind(prePrev.widthProperty().divide(3));
        disp.heightProperty().bind(prePrev.heightProperty().divide(3));
        preview.fitWidthProperty().bind(prePrev.widthProperty());
        preview.fitHeightProperty().bind(prePrev.heightProperty());
        preview.setSmooth(false);

        hor.setStartX(0);
        hor.endXProperty().bind(prePrev.widthProperty());
        hor.startYProperty().bind(prePrev.heightProperty().divide(2));
        hor.endYProperty().bind(prePrev.heightProperty().divide(2));

        ver.setStartY(0);
        ver.endYProperty().bind(prePrev.heightProperty());
        ver.startXProperty().bind(prePrev.widthProperty().divide(2));
        ver.endXProperty().bind(prePrev.widthProperty().divide(2));

        hor.setStroke(Color.BLACK);

        StackPane pickroot = new StackPane(view, prePrev);
        pickroot.setAlignment(Pos.TOP_LEFT);

        Scene scene = new Scene(new StackPane(pickroot));
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode().equals(KeyCode.ESCAPE)) {
                picker.close();
            }
        });
        picker.setScene(scene);

        view.setOnMouseMoved(e -> {
            Color col = view.getImage().getPixelReader().getColor((int) e.getX(), (int) e.getY());
            disp.setFill(col);

            int dispSize = (int) (prePrev.getWidth() / 2);

            prePrev.setTranslateX(e.getX() - dispSize);
            prePrev.setTranslateY(e.getY() + 30);

            int minX = (int) (e.getX() - dispSize / 2.0);
            int minY = (int) (e.getY() - dispSize / 2.0);
            preview.setViewport(new Rectangle2D(minX, minY, dispSize, dispSize));
        });

        view.setOnMouseClicked(_ -> {
            picker.close();
            cp.setValue((Color) disp.getFill());
        });

        Robot robot = new Robot();

        Runnable pickColor = () -> {
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
            BufferedImage capture = robot.createScreenCapture(new Rectangle(size));

            picker.setWidth(size.getWidth());
            picker.setHeight(size.getHeight());

            picker.setX(0);
            picker.setY(0);

            double prs = size.getHeight() / 5;
            prePrev.setMinSize(prs, prs);
            prePrev.setPrefSize(prs, prs);
            prePrev.setMaxSize(prs, prs);

            disp.setFill(Color.PINK);

            Image img = SwingFXUtils.toFXImage(capture, null);
            view.setImage(img);
            preview.setImage(img);

            picker.show();
        };

        Button applyB = new Button("apply");

        Button backup = new Button("backup");
        Button restore = new Button("restore");

        CheckBox tint = new CheckBox("Tint");
        tint.setSelected(true);
        HBox mid = new HBox(tint, hSpace(), backup, restore);
        mid.setAlignment(Pos.CENTER);

        HBox.setMargin(backup, new Insets(0, 10, 0, 0));

        HBox top = new HBox(10);

        ProgressBar pb = new ProgressBar(-1);
        Label status = new Label("Doing nothing");

        pb.prefWidthProperty().bind(top.widthProperty());

        top.getChildren().addAll(cp, pick, applyB);

        root.getChildren().addAll(top, mid, pb, status);

        ps.setResizable(false);
        ps.setScene(new Scene(root));
        ps.setTitle("iCol");
        ps.show();


        pick.setOnAction(_ -> pickColor.run());
        backup.setOnAction(_ -> Backup.backup(ps, status, pb));
        restore.setOnAction(_ -> Backup.restore(ps, status, pb));
        applyB.setOnAction(_ ->
                Desktop.getDesktopIcons(status, pb, icons -> {
                    Platform.runLater(() -> status.setText("Coloring icons..."));
                    for(int i = 0; i < icons.size(); i++) {
                        int finalI = i;
                        Platform.runLater(() -> pb.setProgress((double) finalI / icons.size()));
                        Colorize.apply(icons.get(i), cp.getValue(), tint.isSelected());
                    }

                    Platform.runLater(() -> status.setText("Done"));
                    Platform.runLater(() -> pb.setProgress(-1));
                }));
    }

    private static Pane hSpace() {
        Pane space = new Pane();
        HBox.setHgrow(space, Priority.ALWAYS);
        return space;
    }
}
