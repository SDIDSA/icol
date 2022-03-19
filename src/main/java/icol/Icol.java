package icol;

import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Icol extends Application {
	private static final int THN = 4;

	public void start(Stage ps) {
		VBox root = new VBox(10);
		root.setPadding(new Insets(10));

		ColorPicker cp = new ColorPicker();
		Button applyB = new Button("apply");

		CheckBox tint = new CheckBox("Tint");

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
										+ ni.output.getAbsolutePath() + "\"").execute(getMagick()).waitFor();
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
						Thread.currentThread().interrupt();
					}
				}, (pos, count) -> {
					status.setText("Writing icons " + pos + "/" + count);
					pb.setProgress(pos / (double) count);
				}, THN);
				colorize.execute();

				Platform.runLater(() -> status.setText("Applying icons..."));

				ThreadedTask<DesktopIcon> apply = new ThreadedTask<>(nicons, ni -> setIcon(ni.link, ni.output),
						(pos, count) -> {
							status.setText("Applying icons " + pos + "/" + count);
							pb.setProgress(pos / (double) count);
						}, THN);
				apply.execute();

				try {
					new Command("cmd", "/c", "ie4uinit.exe -show").execute(File.listRoots()[0]).waitFor();
					new Command("cmd", "/c", "taskkill /IM explorer.exe /F").execute(File.listRoots()[0]).waitFor();
					new Command("cmd", "/c",
							"DEL /A /F /Q \"%localappdata%\\Microsoft\\Windows\\Explorer\\iconcache*\"")
									.execute(File.listRoots()[0]).waitFor();
					new Command("cmd", "/c", "start explorer").execute(File.listRoots()[0]);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
					Thread.currentThread().interrupt();
				}

				Platform.runLater(() -> {
					applyB.setDisable(false);
					status.setText("Done");
					pb.setProgress(-1);
				});
			}).start();
		});

		top.getChildren().addAll(cp, applyB);

		root.getChildren().addAll(top, tint, pb, status);

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

	private static List<DesktopIcon> getDesktopIcons() {
		File desktop = new File(System.getProperty("user.home") + "/Desktop");

		ArrayList<File> links = new ArrayList<>();

		for (File file : desktop.listFiles()) {
			if (file.isFile()) {
				String name = file.getName();
				String extension = name.substring(name.lastIndexOf(".") + 1);

				if (extension.equalsIgnoreCase("lnk")) {
					links.add(file);
				}
			}
		}

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

	private String format(double val) {
		String in = Integer.toHexString((int) Math.round(val * 255));
		return in.length() == 1 ? "0" + in : in;
	}

	public String toHexString(Color value) {
		return "#" + (format(value.getRed()) + format(value.getGreen()) + format(value.getBlue())
				+ format(value.getOpacity())).toUpperCase();
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
