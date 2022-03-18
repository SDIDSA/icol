package icol;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.sf.image4j.codec.ico.ICOEncoder;

public class Icol extends Application {
	private static final int THN = 4;

	@SuppressWarnings("unchecked")
	public void start(Stage ps) {
		VBox root = new VBox(10);
		root.setPadding(new Insets(10));

		ColorPicker cp = new ColorPicker();
		Button apply = new Button("apply");

		HBox top = new HBox(10);

		ProgressBar pb = new ProgressBar(-1);
		Label status = new Label("Doing nothing");

		pb.prefWidthProperty().bind(top.widthProperty());

		apply.setOnAction(e -> {
			Color c = cp.getValue();
			apply.setDisable(true);

			ArrayList<Thread> ths = new ArrayList<>();

			status.setText("Reading icons...");
			new Thread(() -> {

				List<DesktopIcon> icons = getDesktopIcons();

				ArrayList<DesktopIcon> nicons = new ArrayList<>();
				ths.clear();

				ArrayList<DesktopIcon>[] tempsIco = new ArrayList[1];
				tempsIco[0] = new ArrayList<>();

				int[] colored = new int[] { 0 };
				Platform.runLater(() -> {
					status.setText("Coloring icons...");
				});

				icons.forEach(icon -> {
					tempsIco[0].add(icon);

					if (tempsIco[0].size() >= (icons.size() / THN)) {
						final ArrayList<DesktopIcon> tempIco = tempsIco[0];
						Thread th = new Thread(() -> {
							tempIco.forEach(i -> {
								colored[0]++;
								Platform.runLater(() -> {
									status.setText("Coloring icons " + colored[0] + "/" + icons.size());
									pb.setProgress(colored[0] / (double) icons.size());
								});
								nicons.add(new DesktopIcon(i.link, colorize(i.icon, c), i.output));
							});
						});
						ths.add(th);

						tempsIco[0] = new ArrayList<>();
					}
				});

				if (!tempsIco[0].isEmpty()) {
					final ArrayList<DesktopIcon> tempIco = tempsIco[0];
					Thread th = new Thread(() -> {
						tempIco.forEach(i -> {
							colored[0]++;
							Platform.runLater(() -> {
								status.setText("Coloring icons " + colored[0] + "/" + icons.size());
								pb.setProgress(colored[0] / (double) icons.size());
							});
							nicons.add(new DesktopIcon(i.link, colorize(i.icon, c), i.output));
						});
					});
					ths.add(th);
				}

				ths.forEach(Thread::start);
				ths.forEach(t -> {
					try {
						t.join();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				});

				ths.clear();

				ArrayList<DesktopIcon>[] tempsNico = new ArrayList[1];
				tempsNico[0] = new ArrayList<>();

				int[] written = new int[] { 0 };
				Platform.runLater(() -> {
					status.setText("Writing icons...");
				});

				nicons.forEach(nicon -> {
					tempsNico[0].add(nicon);

					if (tempsNico[0].size() >= (nicons.size() / THN)) {
						final ArrayList<DesktopIcon> tempNico = tempsNico[0];
						Thread th = new Thread(() -> {
							tempNico.forEach(ni -> {
								written[0]++;
								Platform.runLater(() -> {
									status.setText("Writing icons " + written[0] + "/" + nicons.size());
									pb.setProgress(written[0] / (double) nicons.size());
								});
								try {
									File preOut = new File(ni.output.getAbsolutePath().replace(".ico", ".png"));
									ImageIO.write(ni.icon, "png", preOut);

									File magick = new File(
											URLDecoder.decode(getClass().getResource("/magick.exe").getFile(), "utf-8"))
													.getParentFile();

									new Command("cmd", "/c",
											"magick.exe \"" + preOut.getAbsolutePath()
													+ "\" -define icon:auto-resize=256,128,96,70,64,48,32,16 \""
													+ ni.output.getAbsolutePath() + "\"").execute(magick).waitFor();
								} catch (IOException e1) {
									e1.printStackTrace();
								} catch (InterruptedException e1) {
									e1.printStackTrace();
									Thread.currentThread().interrupt();
								}
							});
						});
						ths.add(th);

						tempsNico[0] = new ArrayList<>();
					}
				});

				if (!tempsNico[0].isEmpty()) {
					final ArrayList<DesktopIcon> tempNico = tempsNico[0];
					Thread th = new Thread(() -> {
						tempNico.forEach(ni -> {
							written[0]++;
							Platform.runLater(() -> {
								status.setText("Writing icons " + written[0] + "/" + nicons.size());
								pb.setProgress(written[0] / (double) nicons.size());
							});
							try {
								File preOut = new File(ni.output.getAbsolutePath().replace(".ico", ".png"));
								ImageIO.write(ni.icon, "png", preOut);

								File magick = new File(
										URLDecoder.decode(getClass().getResource("/magick.exe").getFile(), "utf-8"))
												.getParentFile();

								new Command("cmd", "/c",
										"magick.exe \"" + preOut.getAbsolutePath()
												+ "\" -define icon:auto-resize=256,128,96,70,64,48,32,16 \""
												+ ni.output.getAbsolutePath() + "\"").execute(magick).waitFor();
							} catch (IOException e1) {
								e1.printStackTrace();
							} catch (InterruptedException e1) {
								e1.printStackTrace();
								Thread.currentThread().interrupt();
							}
						});
					});
					ths.add(th);
				}

				ths.forEach(Thread::start);
				ths.forEach(t -> {
					try {
						t.join();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
						Thread.currentThread().interrupt();
					}
				});

				Platform.runLater(() -> {
					status.setText("Applying icons...");
				});
				int[] applied = new int[] { 0 };

				nicons.forEach(ni -> {
					applied[0]++;
					Platform.runLater(() -> {
						status.setText("Applying icons " + applied[0] + "/" + nicons.size());
						pb.setProgress(applied[0] / (double) nicons.size());
					});
					setIcon(ni.link, ni.output);
				});

				try {
					new Command("cmd", "/c", "ie4uinit.exe -show").execute(File.listRoots()[0]).waitFor();
					new Command("cmd", "/c", "taskkill /IM explorer.exe /F").execute(File.listRoots()[0]).waitFor();
					new Command("cmd", "/c",
							"DEL /A /F /Q \"%localappdata%\\Microsoft\\Windows\\Explorer\\iconcache*\"")
									.execute(File.listRoots()[0]).waitFor();
					new Command("cmd", "/c", "start explorer").execute(File.listRoots()[0]);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}

				Platform.runLater(() -> {
					apply.setDisable(false);
					status.setText("Done");
					pb.setProgress(-1);
				});
			}).start();
		});

		top.getChildren().addAll(cp, apply);

		root.getChildren().addAll(top, pb, status);

		ps.setScene(new Scene(root));
		ps.setOnShown(e -> {
			ps.sizeToScene();
		});
		ps.setTitle("iCol");
		ps.show();
	}

	private static BufferedImage colorize(BufferedImage input, Color c) {
		WritableImage img = new WritableImage(input.getWidth(), input.getHeight());
		PixelWriter pw = img.getPixelWriter();

		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				int pixel = input.getRGB(x, y);

//				int blue = pixel & 0xFF;
//				int green = (pixel >> 8) & 0xFF;
//				int red = (pixel >> 16) & 0xFF;
				int alpha = pixel >>> 24;

//				float[] hsb = java.awt.Color.RGBtoHSB(red, green, blue, null);

				pw.setColor(x, y, c.deriveColor(0, 1, 1, (alpha / 255.0)));
			}
		}

		return SwingFXUtils.fromFXImage(img, null);
	}

	private static List<DesktopIcon> getDesktopIcons() {
		// Find the desktop
		File desktop = new File(System.getProperty("user.home") + "/Desktop");

		ArrayList<File> links = new ArrayList<>();

		// loop through the files in the desktop, store shortcuts in a List
		for (File file : desktop.listFiles()) {
			if (file.isFile()) {
				String name = file.getName();
				String extension = name.substring(name.lastIndexOf(".") + 1);

				if (extension.equalsIgnoreCase("lnk")) {
					links.add(file);
				}
			}
		}

		File saveTo = new File(System.getProperty("java.io.tmpdir") + "/icol_icons_" + (int) (Math.random() * 999999));
		saveTo.mkdir();

		// loop through the shortcuts, get their icons in different sizes
		// 16 32 64 128 256
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
		}
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
