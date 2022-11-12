package icol;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.AWTException;
import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;

import org.json.JSONObject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Icol extends Application {

	private static final int THN = 4;

	public void start(Stage ps) throws AWTException {
		VBox root = new VBox(10);
		root.setPadding(new Insets(10));

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

		view.setOnMouseClicked(e -> {
			picker.close();
			cp.setValue((Color) disp.getFill());
		});

		Robot robot = new Robot();
		pick.setOnAction(e -> {
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
		});

		Button applyB = new Button("apply");

		Button backup = new Button("backup");
		Button restore = new Button("restore");

		CheckBox tint = new CheckBox("Tint");
		HBox mid = new HBox(tint, hSpace(), backup, restore);
		mid.setAlignment(Pos.CENTER);

		HBox.setMargin(backup, new Insets(0, 10, 0, 0));

		HBox top = new HBox(10);

		ProgressBar pb = new ProgressBar(-1);
		Label status = new Label("Doing nothing");

		pb.prefWidthProperty().bind(top.widthProperty());

		applyB.setOnAction(e -> {
			Color c = cp.getValue();
			applyB.setDisable(true);

			status.setText("Reading icons...");
			new Thread(() -> {
				List<DesktopIcon> nicons = getDesktopIcons();

				Platform.runLater(() -> status.setText("Coloring icons..."));

				ThreadedTask<DesktopIcon> colorize = new ThreadedTask<>(nicons, ni -> {
					try {
						File preOut = new File(ni.output.getAbsolutePath().replace(".ico", ".png"));
						ImageIO.write(ni.icon, "png", preOut);

						File colorized = colorize(preOut, c, tint.isSelected());

						new Command("cmd", "/c",
								"magick \"" + colorized.getAbsolutePath()
										+ "\" -define icon:auto-resize=256,128,96,70,64,48,32,16 \""
										+ ni.output.getAbsolutePath() + "\"")
								.execute(getMagick()).waitFor();
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
						Thread.currentThread().interrupt();
					}

					setIcon(ni.link, ni.output);
				}, (pos, count) -> {
					status.setText("Writing icons " + pos + "/" + count);
					pb.setProgress(pos / (double) count);
				}, THN);
				colorize.execute();

				Platform.runLater(() -> {
					applyB.setDisable(false);
					status.setText("Done");
					pb.setProgress(-1);
				});
			}).start();
		});

		backup.setOnAction(e -> backup(ps));
		restore.setOnAction(e -> restore(ps, status, pb));

		top.getChildren().addAll(cp, pick, applyB);

		root.getChildren().addAll(top, mid, pb, status);

		ps.setResizable(false);
		ps.setScene(new Scene(root));
		ps.setTitle("iCol");
		ps.show();
	}

	private static File getMagick() {
		try {
			return new File(URLDecoder.decode(Icol.class.getResource("/magick.exe").getFile(), "utf-8"))
					.getParentFile();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static File colorize(File input, Color c, boolean tint) {
		String name = input.getName();
		String extension = name.substring(name.lastIndexOf("."));
		name = name.substring(0, name.lastIndexOf("."));

		File output = new File(input.getParentFile().getAbsolutePath() + File.separator + name + "_tinted" + extension);

		if (tint) {
			String colorHex = "#" + Integer.toHexString(c.hashCode());

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

	private static Random r = new Random();

	private static List<File> getDesktopLinks() {
		File desktop = new File(System.getProperty("user.home") + "/Desktop");
		return Arrays.asList(desktop.listFiles((file, name) -> name.endsWith(".lnk")));
	}

	private static List<DesktopIcon> getDesktopIcons() {
		List<File> links = getDesktopLinks();

		File saveTo = new File(System.getProperty("java.io.tmpdir") + "/icol_icons_" + r.nextInt(9999999));
		saveTo.mkdir();

		ArrayList<DesktopIcon> res = new ArrayList<>();
		links.forEach(link -> {
			String name = link.getName();
			name = name.substring(0, name.lastIndexOf("."));

			File iconFile = new File(saveTo.getAbsolutePath().concat("/").concat(name).concat(".ico"));

			BufferedImage origin = typeIcon(link, 256);
			res.add(new DesktopIcon(link, origin, iconFile));
		});

		return res;
	}

	private static BufferedImage typeIcon(File file, int size) {
		Icon icon = FileSystemView.getFileSystemView().getSystemIcon(file, size, size);
		BufferedImage bImg = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);

		Graphics2D graphics = bImg.createGraphics();
		graphics.drawImage(((ImageIcon) icon).getImage(), 0, 0, null);
		graphics.dispose();
		return bImg;
	}

	private static void setIcon(File link, File icon) {
		if (!icon.exists()) {
			return;
		}
		String name = link.getName();
		name = name.substring(0, name.lastIndexOf("."));
		String script = "Const DESKTOP = &H10&\r\n" + "Set objShell = CreateObject(\"Shell.Application\")\r\n"
				+ "Set objFolder = objShell.NameSpace(DESKTOP)\r\n" + "Set objFolderItem = objFolder.ParseName(\""
				+ link.getName() + "\")\r\n" + "Set objShortcut = objFolderItem.GetLink\r\n"
				+ "objShortcut.SetIconLocation \"" + icon.getAbsolutePath() + "\", 0\r\n" + "objShortcut.Save";

		File scriptFile = new File(
				icon.getParentFile().getAbsolutePath().concat("/").concat(name + "_vbscript").concat(".vbs"));

		FileDealer.write(script, scriptFile);

		try {
			new Command("cmd", "/c", "cscript \"" + scriptFile.getName() + "\"").execute(scriptFile.getParentFile())
					.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}

	private static void backup(Stage ps) {
		FileChooser fc = new FileChooser();
		fc.getExtensionFilters().add(new ExtensionFilter("Desktop Icons Backup", "*.dib"));
		File saveTo = fc.showSaveDialog(ps);
		if (saveTo != null) {
			backup(saveTo);
		}
	}

	private static void restore(Stage ps, Label status, ProgressBar pb) {
		FileChooser fc = new FileChooser();
		fc.getExtensionFilters().add(new ExtensionFilter("Desktop Icons Backup", "*.dib"));
		File saveTo = fc.showOpenDialog(ps);
		if (saveTo != null) {
			restore(saveTo, status, pb);
		}
	}

	private static void backup(File file) {
		JSONObject obj = new JSONObject();
		getDesktopIcons().forEach(icon -> {
			String name = icon.link.getName();
			name = name.substring(0, name.lastIndexOf("."));
			obj.put(name, imgToBase64String(icon.icon, "png"));
		});
		FileDealer.write(obj.toString(), file);
	}

	private static void restore(File file, Label status, ProgressBar pb) {
		JSONObject obj = new JSONObject(FileDealer.read(file));

		File saveTo = new File(System.getProperty("java.io.tmpdir") + "/icol_icons_" + r.nextInt(9999999));
		saveTo.mkdir();

		ArrayList<DesktopIcon> icons = new ArrayList<>();

		getDesktopLinks().forEach(link -> {
			String name = link.getName();
			name = name.substring(0, name.lastIndexOf("."));
			if (obj.has(name) && !obj.isNull(name)) {
				String val = obj.getString(name);
				BufferedImage img = base64StringToImg(val);

				File iconFile = new File(saveTo.getAbsolutePath().concat("/").concat(name).concat(".ico"));

				icons.add(new DesktopIcon(link, img, iconFile));
			}
		});

		Platform.runLater(() -> status.setText("Coloring icons..."));

		ThreadedTask<DesktopIcon> colorize = new ThreadedTask<>(icons, ni -> {
			try {
				File preOut = new File(ni.output.getAbsolutePath().replace(".ico", ".png"));
				ImageIO.write(ni.icon, "png", preOut);

				new Command("cmd", "/c",
						"magick \"" + preOut.getAbsolutePath()
								+ "\" -define icon:auto-resize=256,128,96,70,64,48,32,16 \""
								+ ni.output.getAbsolutePath() + "\"")
						.execute(getMagick()).waitFor();
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				Thread.currentThread().interrupt();
			}

			setIcon(ni.link, ni.output);
		}, (pos, count) -> {
			status.setText("Writing icons " + pos + "/" + count);
			pb.setProgress(pos / (double) count);
		}, THN);

		new Thread(() -> {
			colorize.execute();

			Platform.runLater(() -> {
				status.setText("Done");
				pb.setProgress(-1);
			});
		}).start();
	}

	private String format(double val) {
		String in = Integer.toHexString((int) Math.round(val * 255));
		return in.length() == 1 ? "0" + in : in;
	}

	public String toHexString(Color value) {
		return "#" + (format(value.getRed()) + format(value.getGreen()) + format(value.getBlue())
				+ format(value.getOpacity())).toUpperCase();
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

	private static Pane hSpace() {
		Pane space = new Pane();
		HBox.setHgrow(space, Priority.ALWAYS);
		return space;
	}

	public static class DesktopIcon {
		File link;
		BufferedImage icon;
		File output;

		public DesktopIcon(File link, BufferedImage icon, File output) {
			this.link = link;
			this.icon = icon;
			this.output = output;
		}
	}
}
