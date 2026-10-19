import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.swing.SwingWorker;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class JavaPort extends SwingWorker<Void, String> {
	public static boolean failed;
	public static String tempPackName; // SUPER IMPORTANT

	@Override
	protected Void doInBackground() throws Exception {
		// TODO Auto-generated method stub
		JavaPort.port(new File(Window.workerPackPath));
		return null;
	}

	public static void port(File file) throws IOException {
		Window.textArea.setForeground(Color.WHITE);
		Window.textArea.setSelectedTextColor(Color.WHITE);
		System.out.println("porting mcpack");
		String pax[] = file.getName().split(".mcpack", 2);
		String mcpackName = pax[0];
		// copy pack to swim services folder
		File packTempLocation = new File(System.getenv("APPDATA") + "\\Swim Services\\" + file.getName());
		FileUtils.copyFile(file, packTempLocation);
		unzipMCPACK(packTempLocation); // unzip the mcpack
		String packPath = Window.currentPackSavePath() + "\\";
		String packName = FilenameUtils.removeExtension(file.getName());
		String path = packPath + packName;
		new File(path).mkdirs(); // folder of pack
		// porting begins here:
		packMcMeta(path);
		packIcon(path, tempPackName, mcpackName);
		textures(path, tempPackName, mcpackName);
		armor(path);
		painting(path);
		mcpatcher(path); // moves over the colormap as well
		font(path);
		fire(path);
		chest(path);
		gui(path, tempPackName, mcpackName); // container and panorama and widgets
		sky(path);
		blockFix(path);
		grassSide(path);
		destroyStage(path);
		potions(path);
		sounds(path, tempPackName, mcpackName);
		cleanUp(mcpackName, path);
	}

	public static void unzipMCPACK(File pack) throws IOException {
		try {
			Window.textArea.setText(" Current Task: Extracting MCPACK..");
			String[] packNoExtension = pack.toString().split(".mcpack", 2);
			File zip = new File(packNoExtension[0] + ".zip");
			pack.renameTo(zip);
			UnzipUtility.extractFolder(zip.getAbsolutePath());
			FileUtils.forceDelete(zip);
			System.out.println("extracted mcpack: " + pack);
		} catch (Exception e) {
			System.out.println("error extracting mcpack: " + pack);
			e.printStackTrace();
		}
	}

	public static void packMcMeta(String path) throws IOException {
		try {
			Window.textArea.setText(" Current Task: Generating pack.mcmeta");
			FileWriter packMeta = new FileWriter(path + "\\pack.mcmeta");
			packMeta.write("{\n");
			packMeta.write("  \"pack\": {\n");
			packMeta.write("    \"pack_format\": 1,\n");
			packMeta.write("    \"description\": \"Ported From MCPE Using Swimfan72's Pack Porter\"\n");
			packMeta.write("  }\n");
			packMeta.write("}");
			packMeta.close();
		} catch (Exception e) {
			System.out.println("error writing pack.mcmeta");
			e.printStackTrace();
		}
	}

	public static void packIcon(String path, String pack, String name) {
		try {
			Window.textArea.setText(" Current Task: Porting Pack Icon");
			File pack_icon = new File(System.getenv("APPDATA") + "\\Swim Services\\" + name + "\\" + pack + "\\pack_icon.png");
			if (pack_icon.exists()) {
				FileUtils.copyFile(pack_icon, new File(path + "\\pack_icon.png"));
				File pax1 = new File(path + "\\pack_icon.png");
				File pax2 = new File(path + "\\pack.png");
				pax1.renameTo(pax2);
			} else { // sometimes only one directory deep instead
				pack_icon = new File(System.getenv("APPDATA") + "\\Swim Services\\" + name + "\\pack_icon.png");
				FileUtils.copyFile(pack_icon, new File(path + "\\pack_icon.png"));
				File pax1 = new File(path + "\\pack_icon.png");
				File pax2 = new File(path + "\\pack.png");
				pax1.renameTo(pax2);
			}

		} catch (Exception e) {
			System.out.println("error moving pack_icon.png | might not exist?");
		}
	}

	public static void textures(String path, String pack, String name) {
		try {
			Window.textArea.setText(" Current Task: Porting Textures..");
			File textures = new File(System.getenv("APPDATA") + "\\Swim Services\\" + name + "\\" + pack + "\\textures");
			if (textures.exists()) {
				System.out.println("textures folder found: " + textures.getAbsolutePath());
				File dest = new File(path + "\\assets\\minecraft\\textures");
				FileUtils.copyDirectory(textures, dest);
			} else { // sometimes only one directory deep instead
				textures = new File(System.getenv("APPDATA") + "\\Swim Services\\" + name + "\\textures");
				File dest = new File(path + "\\assets\\minecraft\\textures");
				FileUtils.copyDirectory(textures, dest);
				System.out.println("textures folder found: " + textures.getAbsolutePath());
			}
		} catch (Exception e) {
			System.out.println("error copying over textures folder");
			e.printStackTrace();
		}
	}

	public static void potions(String path) {
		try {
			Window.textArea.setText(" Current Task: Porting Potions..");
			Path potionDir = Paths.get(path + "\\assets\\minecraft\\textures\\items\\potion_bottle_heal.png");
			BufferedImage pot = ImageIO.read(new File(potionDir.toString()));
			int xMidPoint = (int) (pot.getWidth() * (46.88 / 100.0f));
			int yMidPoint = (int) (pot.getWidth() * (62.5 / 100.0f));
			int midPixel = pot.getRGB(xMidPoint, yMidPoint);
			Color midColor = new Color(midPixel, true);
			int red = midColor.getRed();
			int blue = midColor.getBlue();
			int green = midColor.getGreen();
			System.out.println("RGB: " + red + ", " + green + ", " + blue + " | X: " + xMidPoint + " Y: " + yMidPoint);
			potionOverlay(pot, 70, midColor, potionDir, potionDir.getParent().toString() + "\\potion_bottle_drinkable.png");
			// now finally make an empty splash pot
			Path splashPotDir = Paths.get(path + "\\assets\\minecraft\\textures\\items\\potion_bottle_splash_heal.png");
			BufferedImage splashPot = ImageIO.read(new File(splashPotDir.toString()));
			int xMidPointSplash = (int) (splashPot.getWidth() * (46.88 / 100.0f));
			int yMidPointSplash = (int) (splashPot.getWidth() * (62.5 / 100.0f));
			int midPixelSplash = pot.getRGB(xMidPointSplash, yMidPointSplash);
			Color midColorSplash = new Color(midPixelSplash, true);
			int redSplash = midColorSplash.getRed();
			int greenSplash = midColorSplash.getGreen();
			int blueSplash = midColorSplash.getBlue();
			final int TRANSPARENT = 0;
			final int THRESHOLD = 70;
			BufferedImage image = new BufferedImage(splashPot.getWidth(), splashPot.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics pot2 = image.getGraphics();
			pot2.drawImage(splashPot, 0, 0, null);
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					int pixel = image.getRGB(x, y);
					Color color = new Color(pixel);
					int dr = Math.abs(color.getRed() - redSplash), dg = Math.abs(color.getGreen() - greenSplash), db = Math.abs(color.getBlue() - blueSplash);
					if (dr < THRESHOLD && dg < THRESHOLD && db < THRESHOLD) {
						image.setRGB(x, y, TRANSPARENT);
					}
				}
			}
			File done = new File(splashPotDir.getParent() + "\\potion_bottle_splash.png");
			ImageIO.write(image, "PNG", done);
			pot2.dispose();
		} catch (Exception e) {
			System.out.println("potions not found");
		}
	}

	public static void potionOverlay(BufferedImage input, int THRESHOLD, Color backColor, Path path, String export) throws IOException {
		Window.textArea.setText(" Current Task: Creating Potion Overlay");
		final int TRANSPARENT = 0;
		BufferedImage image = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics pot = image.getGraphics();
		pot.drawImage(input, 0, 0, null);
		BufferedImage canvas = new BufferedImage(input.getWidth(), input.getHeight(), input.getType());
		Graphics2D g2 = canvas.createGraphics();
		int yLimitDerivative = (int) (input.getHeight() * (31.25 / 100.0f));
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int pixel = image.getRGB(x, y);
				Color color = new Color(pixel);
				int dr = Math.abs(color.getRed() - backColor.getRed()), dg = Math.abs(color.getGreen() - backColor.getGreen()), db = Math.abs(color.getBlue() - backColor.getBlue());
				if (dr < THRESHOLD && dg < THRESHOLD && db < THRESHOLD && y > yLimitDerivative) {
					Color currentPixelColor = new Color(image.getRGB(x, y));
					g2.setColor(currentPixelColor);
					image.setRGB(x, y, TRANSPARENT);
					g2.drawRect(x, y, 1, 1);
				}
			}
		}
		File done = new File(export);
		ImageIO.write(image, "PNG", done); // drinkable export
		pot.dispose();
		File overlay = new File(path.getParent().toString() + "\\swim overlay.png"); // gets deleted afterwards
		ImageIO.write(canvas, "PNG", overlay);
		g2.dispose();

		BufferedImage defaultOverlay = new BufferedImage(input.getWidth(), input.getHeight(), input.getType());
		defaultOverlay = ImageIO.read(overlay);
		for (int y = 0; y < defaultOverlay.getHeight(); y++) {
			for (int x = 0; x < defaultOverlay.getWidth(); x++) {
				int p = defaultOverlay.getRGB(x, y);
				int a = (p >> 24) & 0xff;
				int r = (p >> 16) & 0xff;
				int g = (p >> 8) & 0xff;
				int b = p & 0xff;
				// calculate average
				int avg = (r + g + b) / 3;
				// replace RGB value with avg
				p = (a << 24) | (avg << 16) | (avg << 8) | avg;
				defaultOverlay.setRGB(x, y, p);
			}
		}
		// write the overlay file and get rid of the temp one
		File greyScaledOverlay = new File(path.getParent().toString() + "\\potion_overlay.png");
		ImageIO.write(defaultOverlay, "PNG", greyScaledOverlay);
		FileUtils.forceDelete(overlay);
	}

	public static void armor(String path) {
		try {
			Window.textArea.setText(" Current Task: Porting Armor");
			String armorDir = path + "\\assets\\minecraft\\textures\\models\\armor\\";

			String[] javaNames = { "diamond_layer_1.png",
					"diamond_layer_2.png",
					"chainmail_layer_1.png",
					"chainmail_layer_2.png",
					"gold_layer_1.png",
					"gold_layer_2.png",
					"iron_layer_1.png",
					"iron_layer_2.png",
					"leather_layer_1.png",
					"leather_layer_2.png" };

			String[] mcpeNames = { "diamond_1.png", "diamond_2.png", "chain_1.png", "chain_2.png", "gold_1.png", "gold_2.png", "iron_1.png", "iron_2.png", "cloth_1.png", "cloth_2.png" };

			for (int i = 0; i < mcpeNames.length; i++) {
				try {
					File original = new File(armorDir + mcpeNames[i]);
					File newName = new File(armorDir + javaNames[i]);
					original.renameTo(newName);
				} catch (Exception e) {
					System.out.println("error renaming armor");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void sounds(String path, String pack, String name) {
		try {
			Window.textArea.setText(" Current Task: Porting Sounds");
			File sounds = new File(System.getenv("APPDATA") + "\\Swim Services\\" + name + "\\" + pack + "\\sounds");
			File soundsDest = new File(path + "\\assets\\minecraft");
			if (sounds.exists()) {
				FileUtils.copyDirectoryToDirectory(sounds, soundsDest);
			} else { // if only one directory deep
				sounds = new File(System.getenv("APPDATA") + "\\Swim Services\\" + name + "\\sounds");
				FileUtils.copyDirectoryToDirectory(sounds, soundsDest);
			}
		} catch (Exception e) {
			System.out.println("No sounds directory found");
		}
	}

	public static void destroyStage(String path) {
		try {
			Window.textArea.setText(" Current Task: Porting Destroy Stage");
			String destroyDir = path + "\\assets\\minecraft\\textures\\environment\\";
			String destDir = path + "\\assets\\minecraft\\textures\\blocks\\";
			for (int i = 0; i < 9; i++) {
				File destroyStage = new File(destroyDir + "destroy_stage_" + i + ".png");
				File destroyDest = new File(destDir + "destroy_stage_" + i + ".png");
				FileUtils.copyFile(destroyStage, destroyDest);
				System.out.println("copying stage: " + destroyStage + " to " + destroyDest);
			}
		} catch (Exception e) {
			System.out.println("error moving over destroy stage, might not exist in environment folder?");
		}
	}

	public static void painting(String path) {
		try {
			Window.textArea.setText(" Current Task: Porting Painting");
			String paintingDir = path + "\\assets\\minecraft\\textures\\painting\\";
			File original = new File(paintingDir + "kz.png");
			File newName = new File(paintingDir + "paintings_kristoffer_zetterstrand.png");
			original.renameTo(newName);
		} catch (Exception e) {
			System.out.println("painting not found");
		}
	}

	public static void mcpatcher(String path) {
		try {
			Window.textArea.setText(" Current Task: Generating mcpatcher and Color Map");
			String mcpatcherDir = path + "\\assets\\minecraft\\mcpatcher\\colormap";
			String colormapMCPE = path + "\\assets\\minecraft\\textures\\colormap";
			new File(path + "\\assets\\minecraft\\mcpatcher\\sky\\world0").mkdirs();
			FileUtils.moveDirectory(new File(colormapMCPE), new File(mcpatcherDir));
		} catch (Exception e) {
			System.out.println("colormap not found");
		}
	}

	public static void font(String path) {
		try {
			Window.textArea.setText(" Current Task: Porting Font");
			String javaFontDir = path + "\\assets\\minecraft\\mcpatcher\\font";
			String mcpeFontDir = path + "\\assets\\minecraft\\textures\\font";
			FileUtils.moveDirectory(new File(mcpeFontDir), new File(javaFontDir));
			System.out.println("porting font");
		} catch (Exception e) {
			System.out.println("font not found");
		}
	}

	public static void fire(String path) {
		try {
			Window.textArea.setText(" Current Task: Porting Fire");
			String blocksDir = path + "\\assets\\minecraft\\textures\\blocks\\";
			File newName = new File(blocksDir + "fire_layer_1.png");
			File original = new File(blocksDir + "fire_1.png");
			original.renameTo(newName);
			File newName2 = new File(blocksDir + "fire_layer_0.png");
			File original2 = new File(blocksDir + "fire_0.png");
			original2.renameTo(newName2);
			System.out.println("porting fire");
		} catch (Exception e) {
			System.out.println("error porting fire");
		}
	}

	public static void chest(String path) {
		try {
			Window.textArea.setText(" Current Task: Porting Chest");
			File chestOld = new File(path + "\\assets\\minecraft\\textures\\entity\\chest\\normal_double.png");
			File chestNew = new File(path + "\\assets\\minecraft\\textures\\entity\\chest\\double_normal.png");
			chestOld.renameTo(chestNew);
			System.out.println("porting chest");
		} catch (Exception e) {
			System.out.println("error porting chest");
		}
	}

	public static void gui(String path, String pack, String name) {
		// copy over container if it exists
		Window.textArea.setText(" Current Task: Porting GUI");
		try {
			File containerDir = new File(System.getenv("APPDATA") + "\\Swim Services\\" + name + "\\" + pack + "\\assets\\minecraft\\textures\\gui\\container");
			if (containerDir.exists()) {
				System.out.println("textures folder found: " + containerDir.getAbsolutePath());
				File dest = new File(path + "\\assets\\minecraft\\textures\\gui\\container");
				FileUtils.copyDirectory(containerDir, dest);
			} else { // sometimes only one directory deep instead
				containerDir = new File(System.getenv("APPDATA") + "\\Swim Services\\" + name + "\\assets\\minecraft\\textures\\gui\\container");
				File dest = new File(path + "\\assets\\minecraft\\textures\\gui\\container");
				FileUtils.copyDirectory(containerDir, dest);
				System.out.println("container folder found: " + containerDir.getAbsolutePath());
			}
		} catch (Exception e) {
			System.out.println("error copying over container folder, probably doesn't exist");
		}
		// rename gui.png to widgets.png
		try {
			File guiPNG = new File(path + "\\assets\\minecraft\\textures\\gui\\gui.png");
			File widgets = new File(path + "\\assets\\minecraft\\textures\\gui\\widgets.png");
			guiPNG.renameTo(widgets);
			System.out.println("renamed gui.png to widgets.png");
		} catch (Exception e) {
			System.out.println("error renaming gui.png | might not exist?");
		}
		// move panorama background folder into title dir
		try {
			File background = new File(path + "\\assets\\minecraft\\textures\\gui\\background");
			File title = new File(path + "\\assets\\minecraft\\textures\\gui\\title\\background");
			FileUtils.moveDirectory(background, title);
			System.out.println("porting panorama");
		} catch (Exception e) {
			System.out.println("error porting panorama | might not exist?");
		}
	}

	public static void sky(String path) {
		try {
			Window.textArea.setText(" Current Task: Porting Sky..");
			String cubemapDir = (path + "\\assets\\minecraft\\textures\\environment\\overworld_cubemap");
			String exportDir = (path + "\\assets\\minecraft\\mcpatcher\\sky\\world0");
			JavaSky.cubemapToSky(cubemapDir, exportDir, false);
			// write the sky properties.mcmeta
			try {
				FileWriter sky = new FileWriter(exportDir + "\\sky1.properties");
				sky.write("startFadeIn=6:00\n");
				sky.write("endFadeIn=12:00\n");
				sky.write("startFadeOut=18:00\n");
				sky.write("endFadeOut=4:00\n");
				sky.write("blend=replace\n");
				sky.write("rotate=true\n");
				sky.write("axis=0.0 0.0 0.0\n");
				sky.write("source=./sky.png");
				sky.close();
			} catch (Exception e) {
				System.out.println("error writing sk1.properties");
				e.printStackTrace();
			}
		} catch (Exception e) {
			System.out.println("error porting sky");
			e.printStackTrace();
		}
	}

	public static void grassSide(String path) throws IOException {
		try {
			Window.textArea.setText(" Current Task: Porting Grass Side TGA to PNG format");
			File grass_sideTGA = new File(path + "\\assets\\minecraft\\textures\\blocks\\grass_side.tga");
			BufferedImage grassSide = TargaReader.getImage(grass_sideTGA.getAbsolutePath());
			File grassSidePNG = new File(path + "\\assets\\minecraft\\textures\\blocks\\grass_side.png");
			ImageIO.write(grassSide, "png", grassSidePNG);
		} catch (Exception e) {
			System.out.println("error fixing grass side");
		}

		try {
			File temp = new File(path + "\\assets\\minecraft\\textures\\blocks\\temp");
			temp.mkdirs();
			File tempOverlay = new File(path + "\\assets\\minecraft\\textures\\blocks\\temp\\grass_side.png");
			File overlay = new File(path + "\\assets\\minecraft\\textures\\blocks\\grass_side_overlay.png");
			FileUtils.forceDelete(overlay);
			File grassSidePNG = new File(path + "\\assets\\minecraft\\textures\\blocks\\grass_side.png");
			FileUtils.copyFile(grassSidePNG, tempOverlay);
			File newOverlay = new File(tempOverlay.getParentFile() + "\\grass_side_overlay.png");
			tempOverlay.renameTo(newOverlay);
			FileUtils.moveFile(newOverlay, overlay);
			FileUtils.forceDelete(temp);
		} catch (Exception e) {
			System.out.println("overlay swap failure");
		}

		try {
			File snow_sideTGA = new File(path + "\\assets\\minecraft\\textures\\blocks\\grass_side_snow.tga");
			BufferedImage snowSide = TargaReader.getImage(snow_sideTGA.getAbsolutePath());
			File snowSidePNG = new File(path + "\\assets\\minecraft\\textures\\blocks\\grass_side_snow.png");
			ImageIO.write(snowSide, "png", snowSidePNG);
		} catch (Exception e) {
			System.out.println("error fixing snow side");
		}
	}

	public static void blockFix(String path) {
		try {
			File dir = new File(path + "\\assets\\minecraft\\textures\\blocks");
			File[] directoryListing = dir.listFiles();
			if (directoryListing != null) {
				for (File currentBlock : directoryListing) {
					String extension = FilenameUtils.getExtension(currentBlock.getAbsolutePath());
					if (extension.equals("mcmeta")) {
						// get rid of any stray mcmeta files that would cause errors
						FileUtils.forceDelete(currentBlock);
					}
				}
				for (File currentBlock : directoryListing) {
					Window.textArea.setText(" Current Task: Writing Block Definition: " + currentBlock.getName());
					String extension = FilenameUtils.getExtension(currentBlock.getAbsolutePath());
					if (extension.equals("png")) {
						System.out.println("Found png: " + currentBlock.toString());
						BufferedImage dimensionCheck = ImageIO.read(currentBlock);
						if (dimensionCheck.getHeight() != dimensionCheck.getWidth()) {
							System.out.println("animated texture found");
							String fileName = FilenameUtils.getName(currentBlock.toString());
							FileWriter animationMcMeta = new FileWriter(dir + "\\" + fileName + ".mcmeta");
							animationMcMeta.write("{\n");
							animationMcMeta.write("  \"animation\": {}\n");
							animationMcMeta.write("}");
							animationMcMeta.close();
						}
					} else {
						System.out.println("Not a png file: " + extension + " : " + currentBlock.toString());
					}
				}
			}
		} catch (Exception e) {
			System.out.println("error fixing block");
			e.printStackTrace();
		}
	}

	public static void cleanUp(String name, String path) {
		try {
			Window.textArea.setText(" Current Task: Cleaning up Pack..");
			Window.cleanAppData(); 
			File pack = new File(path);
			File renamePack = new File(path + " MCBEPORT");
			pack.renameTo(renamePack);
			Window.textArea.setText("");
			Window.updateConsoleText("Successfully Ported Pack:\n " + renamePack.getAbsolutePath(), false);
			Window.busy = false;
			System.out.println("no longer busy");
		} catch (Exception e) {
			System.out.println("error completing pack clean up");
			e.printStackTrace();
		}
	}

}
