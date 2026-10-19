import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import javax.swing.SwingWorker;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class Port extends SwingWorker<Void, String> {
	public static int xpBarLength;
	public static int xpBarWidth;
	public static boolean failed;
	public static String originalDescription; // global so my manifest writer can access it
	public static String packPathLocation; // accessed in settings
	public static String skyPathLocation; // accessed in settings

	@Override
	protected Void doInBackground() throws Exception {
		// TODO Auto-generated method stub
		Port.port(new File(Window.workerPackPath));
		return null;
	}

	public static void port(File file) throws IOException {
		String packPath = Window.currentPackSavePath() + "\\";
		String filePath = file.getAbsolutePath();
		failed = false;

		try {
			File mcmeta = new File(file + "\\pack.mcmeta");
			Scanner reader = new Scanner(mcmeta);
			// this gets the packs description from the mcmeta file
			try {
				while (reader.hasNextLine()) {
					String data = reader.nextLine();
					if (data.contains("description")) {
						String[] splitName = data.split("\"", 4);
						String desc = splitName[3];
						originalDescription = desc.replaceAll("\\\\", " "); // filter out escape chars
						originalDescription = originalDescription.replaceAll("\\\"", " "); // filter out quotes
						System.out.println(originalDescription);
						// there could possibly be more escape chars to worry about having to filter
					}
				}
				reader.close();
			} catch (Exception e) {
				System.out.println("failed to read pack data");
			}
		} catch (FileNotFoundException ex) {
			Window.textArea.setText(" Failed to port: " + filePath + "\n could not locate pack.mcmeta");
			Window.textArea.setSelectedTextColor(Color.RED);
			Window.textArea.setForeground(Color.RED);
			failed = true;
			Window.busy = false;
		}

		if (!failed) {
			Window.updateConsoleText("Current Task: Processing File", false);
		}

		String name = FilenameUtils.removeExtension(file.getName());
		String packName = FilenameUtils.getName(name + " PORT");
		String description = originalDescription + "\\nPorted with Swimfan72's Auto Port";

		if (!failed) {
			new File(packPath + packName).mkdirs(); // folder of pack
			new File(packPath + packName + "\\textures").mkdirs(); // textures
			new File(packPath + packName + "\\textures\\ui").mkdirs(); // ui

			createManifest(packName, name, description, packPath);
			icon(file, packName, packPath);
			textures(file, packName, packPath); // does like 80 percent the porting in one folder copy
			blockItemFix(file, packName, packPath); // renames block directory to blocks if needed, same for items
			lowerCaseAll(new File(packPath + packName)); // lower cases all file and directory names recursively in the pack
			armor(file, packName, packPath);
			painting(file, packName, packPath);
			colorMap(file, packName, packPath);
			font(file, packName, packPath); // folder move + rename ascii to default8
			gui(file, packName, packPath);
			fire(file, packName, packPath);
			environment(file, packName, packPath);
			sky(file, packName, packPath);
			xp(file, packName, packPath); // part 1 of xp port
			barJson(file, packName, packPath); // part 2 of xp port
			guiFix(file, packName, packPath);
			crossHairFix(file, packName, packPath);
			chestFix(file, packName, packPath);
			potionEffectsUI(file, packName, packPath);
			panorama(file, packName, packPath);
			sounds(file, packName, packPath);
			itemsFix(file, packName, packPath); // get rid of alpha
			potions(file, packName, packPath); // grey tint bug sometimes, very rare, still don't know why
			inventoryPort(file, packName, packPath);
			grassSide(file, packName, packPath); // tga handling

			// finally we will need to zip the pack and turn its file extension to a .mcpack
			File mcpack = new File(packPath + packName + ".mcpack");
			if (Window.configs.get(0).equals("true")) {
				cleanUp(packName, packPath);
			} else {
				Window.textArea.setText(" Successfully Ported Pack: \n " + mcpack);
				Window.textArea.setSelectedTextColor(Color.GREEN);
				Window.textArea.setForeground(Color.GREEN);
			}
			Window.cleanAppData();
			Window.busy = false;
			System.out.println("no longer busy");
		}
	}

	public static void createManifest(String PackName, String name, String description, String packPath) {
		try {
			Window.progressConsoleUpdate("Generating Manifest");
			// File man = new File(packPath + name + "\\manifest.json");
			FileWriter manifest = new FileWriter(packPath + PackName + "\\manifest.json");
			// create the UUIDs for the manifest file
			String UUID = UUIDGenerator.generateType1UUID().toString();
			String UUID2 = UUIDGenerator.generateType1UUID().toString();
			// \n is required on each line to break onto the next line when writing
			manifest.write("{\n");
			manifest.write("    \"format_version\": 1,\n");
			manifest.write("    \"header\": {\n");
			manifest.write("        \"description\": \"" + description + "\",\n");
			manifest.write("        \"name\": \"" + name + "\",\n");
			manifest.write("        \"uuid\": \"" + UUID + "\",\n");
			manifest.write("        \"version\": [1, 0, 0],\n");
			manifest.write("        \"min_engine_version\": [1, 12, 1]\n");
			manifest.write("    },\n");
			manifest.write("    \"modules\": [\n");
			manifest.write("        {\n");
			manifest.write("            \"description\": \"" + description + "\",\n");
			manifest.write("            \"type\": \"resources\",\n");
			manifest.write("            \"uuid\": \"" + UUID2 + "\",\n");
			manifest.write("            \"version\": [1, 0, 0]\n");
			manifest.write("        }\n");
			manifest.write("    ]\n");
			manifest.write("}\n");

			manifest.close();

			System.out.println("Successfully created the manifest.json");
		} catch (IOException e) {
			System.out.println("An error occurred creating the manifest.json");
		}
	}

	public static void guiFix(File file, String packName, String packPath) throws IOException {
		try {
			Window.progressConsoleUpdate("Scaling GUI");
			File gui = new File(packPath + packName + "\\textures\\gui\\gui.png");
			BufferedImage guiIMG = ImageIO.read(new File(packPath + packName + "\\textures\\gui\\gui.png"));
			if (guiIMG.getHeight() != 256) {
				resizeImage(gui, gui, 256, 256, "png");
			}
		} catch (Exception e) {
			System.out.println("failed to scale gui");
		}
	}

	public static void icon(File file, String packName, String packPath) {
		Window.progressConsoleUpdate("Porting Pack Icon");
		File source = new File(file + "\\pack.png");
		File dest = new File(packPath + packName + "\\pack.png");
		try {
			FileUtils.copyFile(source, dest);
			File newIconName = new File(packPath + packName + "\\pack.png");
			File fixedName = new File(packPath + packName + "\\pack_icon.png");
			newIconName.renameTo(fixedName);
		} catch (IOException e) {
			System.out.println("icon not found");
		}
	}

	public static void chestFix(File file, String packName, String packPath) {
		try {
			Window.progressConsoleUpdate("Porting Chest");
			File chestOld = new File(packPath + packName + "\\textures\\entity\\chest\\normal_double.png");
			File chestNew = new File(packPath + packName + "\\textures\\entity\\chest\\double_normal.png");
			chestOld.renameTo(chestNew);
		} catch (Exception e) {
			System.out.println("failed to port chests");
		}
	}

	public static void textures(File file, String packName, String packPath) {
		Window.progressConsoleUpdate("Porting Textures..");
		File source = new File(file + "\\assets\\minecraft\\textures");
		File dest = new File(packPath + packName + "\\textures");
		try {
			FileUtils.copyDirectory(source, dest);
		} catch (IOException e) {
			System.out.println("failed to port textures");
		}
	}

	public static void grassSide(File file, String packName, String packPath) {
		try {
			BufferedImage grassSidePNG = ImageIO.read(new File(packPath + packName + "\\textures\\blocks\\grass_side.png"));
			Path grassTGA = new File(packPath + packName + "\\textures\\blocks\\grass_side.tga").toPath();
			Window.progressConsoleUpdate("Porting Grass Side TGA");
			TargaReader.writeTargaImageTo(grassTGA, grassSidePNG);
		} catch (IOException e) {
			System.out.println("error converting grass png to tga");
		}
		try {
			BufferedImage snowSidePNG = ImageIO.read(new File(packPath + packName + "\\textures\\blocks\\grass_side_snowed.png"));
			Path snowTGA = new File(packPath + packName + "\\textures\\blocks\\grass_side_snow.tga").toPath();
			Window.progressConsoleUpdate("Porting Snow Side TGA");
			TargaReader.writeTargaImageTo(snowTGA, snowSidePNG);
		} catch (IOException e) {
			System.out.println("error converting snow png to tga");
		}
	}

	public static void lowerCaseAll(File master) {
		System.out.println("lower casing all of directory: " + master);
		try {
			File[] directoryListing = master.listFiles();
			if (directoryListing != null) {
				for (File currentFile : directoryListing) {
					String lowerCasedName = FilenameUtils.getName(currentFile.toString().toLowerCase());
					System.out.println(lowerCasedName);
					File newName = new File(currentFile.getParentFile() + "\\" + lowerCasedName);
					currentFile.renameTo(newName);
					Window.progressConsoleUpdate("Lower Casing: " + currentFile.getName());
					if (currentFile.isDirectory()) {
						lowerCaseAll(currentFile);
					}
				}
			}
		} catch (Exception e) {
			System.out.println("failed to lower case files");
		}
	}

	public static void blockItemFix(File file, String packName, String packPath) {
		try {
			File blockPath = new File(packPath + packName + "\\textures\\block\\");
			File blockPathFixed = new File(packPath + packName + "\\textures\\blocks\\");
			if (blockPath.exists()) {
				Window.progressConsoleUpdate("Fixing blocks directory");
				blockPath.renameTo(blockPathFixed);
			}
			File itemPath = new File(packPath + packName + "\\textures\\item\\");
			File itemPathFixed = new File(packPath + packName + "\\textures\\items\\");
			if (itemPath.exists()) {
				Window.progressConsoleUpdate("Fixing items directory");
				itemPath.renameTo(itemPathFixed);
			}
		} catch (Exception e) {
			System.out.println("directory renaming not required for this pack");
		}
	}

	public static void potionEffectsUI(File file, String packName, String packPath) {
		try {
			File inventory = new File(packPath + packName + "\\textures\\gui\\container\\inventory.png");
			String path = packPath + packName + "\\textures\\ui\\";
			BufferedImage spriteSheet = ImageIO.read(inventory);
			double ugly = spriteSheet.getHeight() / 4.41379310345;
			int extra = (int) Math.round(ugly);
			int startingY = spriteSheet.getHeight() - extra;
			int x = 0;
			double hypo = spriteSheet.getHeight() / 14.2222222222;
			int change = (int) Math.round(hypo);
			String effects[] = { "speed_effect.png",
					"slowness_effect.png",
					"haste_effect.png",
					"mining_fatigue_effect.png",
					"strength_effect.png",
					"weakness_effect.png",
					"poison_effect.png",
					"regeneration_effect.png",
					"invisibility_effect.png",
					"saturation_effect.png",
					"jump_boost_effect.png",
					"nausea_effect.png",
					"night_vision_effect.png",
					"blindness_effect.png",
					"resistance_effect.png",
					"fire_resistance_effect.png",
					"water_breathing_effect.png",
					"wither_effect.png",
					"absorption_effect.png" };
			// now that we have all our variables, loop through and sub image
			int incremento = 0;
			for (int i = 0; i < 19; i++) {
				System.out.println("porting effect: " + effects[i]);
				if (i % 8 == 0 && i != 0) {
					x = 0;
					incremento = 1;
					startingY = startingY + change;
				} else {
					x = incremento * change;
					incremento++;
				}
				ImageIO.write(spriteSheet.getSubimage(x, startingY, change, change), "png", new File(path + effects[i]));
				Window.progressConsoleUpdate("Porting Effect UI Icon: " + (effects[i]).toString());
			}
		} catch (Exception ex) {
			System.out.println("inventory.png does not exist");
		}
	}

	public static void potions(File file, String packName, String packPath) throws IOException {
		try {
			String drinkPots[] = { "potion_bottle_blindness.png",
					"potion_bottle_damageBoost.png",
					"potion_bottle_fireResistance.png",
					"potion_bottle_harm.png",
					"potion_bottle_heal.png",
					"potion_bottle_invisibility.png",
					"potion_bottle_jump.png",
					"potion_bottle_luck.png",
					"potion_bottle_moveSlowdown.png",
					"potion_bottle_moveSpeed.png",
					"potion_bottle_nightVision.png",
					"potion_bottle_poison.png",
					"potion_bottle_regeneration.png",
					"potion_bottle_slowFall.png",
					"potion_bottle_turtleMaster.png",
					"potion_bottle_waterBreathing.png",
					"potion_bottle_weakness.png",
					"potion_bottle_wither.png",
					"potion_bottle_drinkable.png" };

			String splashPots[] = { "potion_bottle_splash_blindness.png",
					"potion_bottle_splash_damageBoost.png",
					"potion_bottle_splash_fireResistance.png",
					"potion_bottle_splash_harm.png",
					"potion_bottle_splash_heal.png",
					"potion_bottle_splash_invisibility.png",
					"potion_bottle_splash_jump.png",
					"potion_bottle_splash_luck.png",
					"potion_bottle_splash_moveSlowdown.png",
					"potion_bottle_splash_moveSpeed.png",
					"potion_bottle_splash_nightVision.png",
					"potion_bottle_splash_poison.png",
					"potion_bottle_splash_regeneration.png",
					"potion_bottle_splash_slowFall.png",
					"potion_bottle_splash_turtleMaster.png",
					"potion_bottle_splash_waterBreathing.png",
					"potion_bottle_splash_weakness.png",
					"potion_bottle_splash_wither.png",
					"potion_bottle_splash.png" };

			File drinkBlank = new File(packPath + packName + "\\textures\\items\\potion_bottle_drinkable.png");
			File splashBlank = new File(packPath + packName + "\\textures\\items\\potion_bottle_splash.png");
			File overlay = new File(packPath + packName + "\\textures\\items\\potion_overlay.png");

			try {
				BufferedImage overlayCompare = ImageIO.read(overlay);
				BufferedImage drinkCompare = ImageIO.read(drinkBlank);
				if (overlayCompare.getHeight() != drinkCompare.getHeight()) {
					System.out.println("correcting the potion overlay size (splash)");
					resizeImage(overlay, overlay, drinkCompare.getHeight(), drinkCompare.getWidth(), "png");
					Window.progressConsoleUpdate("Correcting Potion Overlay Size");
				}
			} catch (Exception e) {
				System.out.println("cant find overlay");
			}

			for (int i = 0; i < 19; i++) {
				File path = new File(packPath + packName + "\\textures\\items\\" + drinkPots[i]);
				BufferedImage source = ImageIO.read(drinkBlank);
				BufferedImage over = ImageIO.read(overlay);
				Window.progressConsoleUpdate("Generating Potion: " + path.getName());
				switch (i) {
				case 0:
					over = tint(over, Color.gray);
					break;
				case 1:
					over = tint(over, Color.RED);
					break;
				case 2:
					over = tint(over, Color.orange);
					break;
				case 3:
					over = tint(over, Color.RED);
					break;
				case 4:
					over = tint(over, Color.RED);
					break;
				case 5:
					over = tint(over, Color.gray);
					break;
				case 6:
					over = tint(over, Color.green);
					break;
				case 7:
					over = tint(over, Color.GREEN);
					break;
				case 8:
					over = tint(over, Color.cyan);
					break;
				case 9:
					over = tint(over, Color.CYAN);
					break;
				case 10:
					over = tint(over, Color.blue);
					break;
				case 11:
					over = tint(over, Color.green);
					break;
				case 12:
					over = tint(over, Color.PINK);
					break;
				case 13:
					over = tint(over, Color.LIGHT_GRAY);
					break;
				case 14:
					over = tint(over, Color.lightGray);
					break;
				case 15:
					over = tint(over, Color.BLUE);
					break;
				case 16:
					over = tint(over, Color.lightGray);
					break;
				case 17:
					over = tint(over, Color.black);
					break;
				case 18:
					over = tint(over, Color.WHITE);
					break;
				case 19:
					over = tint(over, Color.BLUE);
					break;
				default:
					over = tint(over, Color.BLUE);
					break;
				}
				Graphics g = source.getGraphics();
				g.drawImage(over, 0, 0, null);
				g.dispose();
				ImageIO.write(source, "png", path);
			}

			try {
				BufferedImage overlayCompare = ImageIO.read(overlay);
				BufferedImage splashCompare = ImageIO.read(splashBlank);
				if (overlayCompare.getHeight() != splashCompare.getHeight()) {
					System.out.println("correcting the potion overlay size (drink)");
					resizeImage(overlay, overlay, splashCompare.getWidth(), splashCompare.getHeight(), "png");
				}
			} catch (Exception e) {
				System.out.println("cant find overlay");
			}

			for (int i = 0; i < 19; i++) {
				File path = new File(packPath + packName + "\\textures\\items\\" + splashPots[i]);
				BufferedImage source = ImageIO.read(splashBlank);
				BufferedImage over = ImageIO.read(overlay);
				Window.progressConsoleUpdate("Generating Potion: " + path.getName());
				switch (i) {
				case 0:
					over = tint(over, Color.gray);
					break;
				case 1:
					over = tint(over, Color.RED);
					break;
				case 2:
					over = tint(over, Color.orange);
					break;
				case 3:
					over = tint(over, Color.RED);
					break;
				case 4:
					over = tint(over, Color.RED);
					break;
				case 5:
					over = tint(over, Color.gray);
					break;
				case 6:
					over = tint(over, Color.green);
					break;
				case 7:
					over = tint(over, Color.GREEN);
					break;
				case 8:
					over = tint(over, Color.cyan);
					break;
				case 9:
					over = tint(over, Color.CYAN);
					break;
				case 10:
					over = tint(over, Color.blue);
					break;
				case 11:
					over = tint(over, Color.green);
					break;
				case 12:
					over = tint(over, Color.PINK);
					break;
				case 13:
					over = tint(over, Color.LIGHT_GRAY);
					break;
				case 14:
					over = tint(over, Color.lightGray);
					break;
				case 15:
					over = tint(over, Color.BLUE);
					break;
				case 16:
					over = tint(over, Color.lightGray);
					break;
				case 17:
					over = tint(over, Color.black);
					break;
				case 18:
					over = tint(over, Color.WHITE);
					break;
				case 19:
					over = tint(over, Color.BLUE);
					break;
				default:
					over = tint(over, Color.BLUE);
					break;
				}
				Graphics g = source.getGraphics();
				g.drawImage(over, 0, 0, null);
				g.dispose();
				ImageIO.write(source, "png", path);
			}
		} catch (Exception e) {
			System.out.println("tint failure");
		}

	}

	// could reuse tint() as an auto recoloring method for future versions
	public static BufferedImage tint(BufferedImage image, Color color) {
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				Color pixelColor = new Color(image.getRGB(x, y), true);
				int r = (pixelColor.getRed() + color.getRed()) / 2;
				int g = (pixelColor.getGreen() + color.getGreen()) / 2;
				int b = (pixelColor.getBlue() + color.getBlue()) / 2;
				int a = pixelColor.getAlpha(); // maybe alpha pixels is what causes the grey tint issue
				int rgba = (a << 24) | (r << 16) | (g << 8) | b;
				image.setRGB(x, y, rgba);
			}
		}
		return image;
	}

	public static void itemsFix(File file, String packName, String packPath) throws IOException {
		try {
			File items = new File(packPath + packName + "\\textures\\items");
			String[] images = items.list();
			for (String currentImage : images) {
				try {
					File img = new File(packPath + packName + "\\textures\\items\\" + currentImage);
					BufferedImage item = ImageIO.read(img);
					int chromaValue = item.getRGB(item.getHeight() - 1, item.getWidth() - 1); // grab alpha value
					Color chromaKey = new Color(chromaValue, true);
					BufferedImage clean = colorToAlpha(item, chromaKey); // kind of like ultra/color keying
					ImageIO.write(clean, "png", img);
					Window.progressConsoleUpdate("Rasterizing Image: " + img.getName());
				} catch (Exception ex) {
					System.out.println("failed to fix image " + currentImage + " (might not be a .png file?)");
				}
			}
		} catch (Exception e) {
			System.out.println("failed to find items folder");
		}
	}

	public static BufferedImage colorToAlpha(BufferedImage raw, Color remove) {
		int WIDTH = raw.getWidth();
		int HEIGHT = raw.getHeight();
		BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		int pixels[] = new int[WIDTH * HEIGHT];
		raw.getRGB(0, 0, WIDTH, HEIGHT, pixels, 0, WIDTH);
		for (int i = 0; i < pixels.length; i++) {
			if (pixels[i] == remove.getRGB()) {
				pixels[i] = 0x00ffffff;
			}
		}
		image.setRGB(0, 0, WIDTH, HEIGHT, pixels, 0, WIDTH);
		return image;
	}

	public static void xp(File file, String packName, String packPath) throws IOException {
		try {
			String path = packPath + packName + "\\textures\\ui\\";
			BufferedImage icons = ImageIO.read(new File(packPath + packName + "\\textures\\gui\\icons.png"));

			int x = 0;
			int y = icons.getHeight() / 4;

			double ugly = icons.getWidth() / 3.45945945946; // calculate extra space between the xp bar and img width
			int extra = (int) Math.round(ugly); // round it to a whole integer
			xpBarLength = icons.getWidth() - extra; // subtract the extra white space to find the xp bar's length
			// 256x256 is probably the smallest icons.png can be
			xpBarWidth = 5 * (icons.getWidth() / 256); // height doubles as a multiple of 5, factor of width

			ImageIO.write(icons.getSubimage(x, y, xpBarLength, xpBarWidth), "png", new File(path + "experiencebarempty.png"));
			ImageIO.write(icons.getSubimage(x, y + xpBarWidth, xpBarLength, xpBarWidth), "png", new File(path + "experiencebarfull.png"));

			File xpBarEmpty = new File(path + "experiencebarempty.png");
			File xpBarFull = new File(path + "experiencebarfull.png");

			resizeImage(xpBarEmpty, xpBarEmpty, 728, 20, "png");
			resizeImage(xpBarFull, xpBarFull, 728, 20, "png");

			File source = new File(packPath + packName + "\\textures\\ui");
			File dest = new File(packPath + packName + "\\textures\\gui\\achievements");

			try {
				FileUtils.copyDirectory(source, dest);
				try {
					File xpBarEmpty2 = new File(packPath + packName + "\\textures\\gui\\achievements\\experiencebarempty.png");
					File xpBarFull2 = new File(packPath + packName + "\\textures\\gui\\achievements\\experiencebarfull.png");

					File hotdogempty = new File(packPath + packName + "\\textures\\gui\\achievements\\hotdogempty.png");
					File hotdogfull = new File(packPath + packName + "\\textures\\gui\\achievements\\hotdogfull.png");

					xpBarEmpty2.renameTo(hotdogempty);
					xpBarFull2.renameTo(hotdogfull);
					Window.progressConsoleUpdate("Making XP Bars");
				} catch (Exception b) {
					System.out.println("failed to copy xp bars");
				}
			} catch (IOException e) {
				System.out.println("failed to find xp bar");
			}

		} catch (Exception f) {
			System.out.println("ui pack failure");
		}

		// now we need to put in notnub.png and nub.png into the achievements folder
		try {
			BufferedImage notnub = ImageIO.read(Port.class.getResource("notnub.png"));
			BufferedImage nub = ImageIO.read(Port.class.getResource("nub.png"));

			File notNubPath = new File(packPath + packName + "\\textures\\gui\\achievements\\notnub.png");
			File nubPath = new File(packPath + packName + "\\textures\\gui\\achievements\\nub.png");

			ImageIO.write(notnub, "png", notNubPath);
			ImageIO.write(nub, "png", nubPath);
		} catch (IOException h) {
			System.out.println("failed to update nubs");
		}

		// a ghost achievements folder gets generated so I guess just delete it
		try {
			File ghost = new File(packPath + packName + "\\textures\\gui\\achievements\\achievements");
			ghost.delete();
		} catch (Exception j) {
			System.out.println("ghost folder not found *not a bad thing");
		}

		// now we need to make the xp bar json files
		// which will be done in barJson();
	}

	public static void barJson(File file, String packName, String packPath) {
		try {
			FileWriter experiencebarempty = new FileWriter(packPath + packName + "\\textures\\ui\\experiencebarempty.json");
			// \n is required on each line to break onto the next line when writing
			experiencebarempty.write("{\n");
			experiencebarempty.write("  \"nineslice_size\": [\n");
			experiencebarempty.write("    6,\n");
			experiencebarempty.write("    1,\n");
			experiencebarempty.write("    6,\n");
			experiencebarempty.write("    1\n");
			experiencebarempty.write("  ],\n");
			experiencebarempty.write("  \"base_size\": [\n");
			experiencebarempty.write("    " + 364 + ",\n");
			experiencebarempty.write("    " + 10 + "\n");
			experiencebarempty.write("  ]\n");
			experiencebarempty.write("}\n");

			experiencebarempty.close();

			Window.progressConsoleUpdate("Writing XP Bar JSON");
			System.out.println("Successfully created the experiencebarempty.json");
		} catch (IOException e) {
			System.out.println("An error occurred creating the experiencebarempty.json");
		}

		try {
			FileWriter experiencebarfull = new FileWriter(packPath + packName + "\\textures\\ui\\experiencebarfull.json");
			// \n is required on each line to break onto the next line when writing
			experiencebarfull.write("{\n");
			experiencebarfull.write("  \"nineslice_size\": [\n");
			experiencebarfull.write("    1,\n");
			experiencebarfull.write("    0,\n");
			experiencebarfull.write("    1,\n");
			experiencebarfull.write("    0\n");
			experiencebarfull.write("  ],\n");
			experiencebarfull.write("  \"base_size\": [\n");
			experiencebarfull.write("    " + 364 + ",\n");
			experiencebarfull.write("    " + 10 + "\n");
			experiencebarfull.write("  ]\n");
			experiencebarfull.write("}\n");

			experiencebarfull.close();

			System.out.println("Successfully created the experiencebarfull.json");
		} catch (IOException e) {
			System.out.println("An error occurred creating the experiencebarfull.json");
		}
	}

	private static void resizeImage(File originalImage, File resizedImage, int width, int height, String format) {
		try {
			BufferedImage original = ImageIO.read(originalImage);
			BufferedImage resized = new BufferedImage(width, height, original.getType());
			Graphics2D g2 = resized.createGraphics();
			g2.drawImage(original, 0, 0, width, height, null);
			g2.dispose();
			ImageIO.write(resized, format, resizedImage);
		} catch (IOException e) {
			System.out.println("failed to resize image");
		}
	}

	public static void sky(File file, String packName, String packPath) throws IOException {
		BufferedImage skyMap = null;
		Window.progressConsoleUpdate("Creating Cube Map..");
		// try to get any sky we can
		// is day night sunrise sunset cycle sky worth porting?
		try { // these try catches are very spaghetti
			skyMap = ImageIO.read(new File(file + "\\assets\\minecraft\\mcpatcher\\sky\\world0\\cloud1.png"));
		} catch (IOException a) {
			try {
				skyMap = ImageIO.read(new File(file + "\\assets\\minecraft\\mcpatcher\\sky\\world0\\cloud2.png"));
			} catch (IOException b) {
				try {
					skyMap = ImageIO.read(new File(file + "\\assets\\minecraft\\mcpatcher\\sky\\world0\\starfield03.png"));
				} catch (IOException c) {
					try {
						skyMap = ImageIO.read(new File(file + "\\assets\\minecraft\\mcpatcher\\sky\\world0\\starfield.png"));
					} catch (IOException d) {
						try {
							skyMap = ImageIO.read(new File(file + "\\assets\\minecraft\\mcpatcher\\sky\\world0\\skybox.png"));
						} catch (IOException e) {
							try {
								skyMap = ImageIO.read(new File(file + "\\assets\\minecraft\\mcpatcher\\sky\\world0\\skybox2.png"));
							} catch (IOException f) {
								System.out.println("failed to locate a sky box");
							}
							System.out.println("attempting to find a sky box");
						}
					}
					System.out.println("attempting to find a sky box");
				}
				System.out.println("attempting to find a sky box");
			}
			System.out.println("attempting to find a sky box");
		}
		// hopefully that managed to grab a sky box
		if (skyMap != null) {
			System.out.println("porting sky : " + skyMap);
			String path = packPath + packName + "\\textures\\environment\\overworld_cubemap\\";
			int x = skyMap.getWidth() / 3;
			int y = skyMap.getHeight() / 2;
			// top right (CORRECT)
			ImageIO.write(skyMap.getSubimage(skyMap.getHeight(), 0, x, y), "png", new File(path + "cubemap_" + 0 + ".png"));
			// bottom left (CORRECT)
			ImageIO.write(skyMap.getSubimage(0, skyMap.getHeight() / 2, x, y), "png", new File(path + "cubemap_" + 1 + ".png"));
			// bottom middle (CORRECT)
			ImageIO.write(skyMap.getSubimage(skyMap.getWidth() / 3, skyMap.getHeight() / 2, x, y), "png", new File(path + "cubemap_" + 2 + ".png"));
			// bottom right (CORRECT)
			ImageIO.write(skyMap.getSubimage(skyMap.getHeight(), skyMap.getHeight() / 2, x, y), "png", new File(path + "cubemap_" + 3 + ".png"));
			// top middle (CORRECT) // need to do 180 degree flip
			ImageIO.write(skyMap.getSubimage(skyMap.getWidth() / 3, 0, x, y), "png", new File(path + "cubemap_" + 4 + ".png"));
			File input = new File(path + "cubemap_" + 4 + ".png");
			BufferedImage lol = ImageIO.read(new File(input.getAbsolutePath()));
			AffineTransform tx = AffineTransform.getScaleInstance(-1, -1);
			AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			tx.translate(lol.getWidth(null) * -1, lol.getHeight(null) * -1);
			op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			BufferedImage image = op.filter(lol, null);
			File replace = new File(path + "cubemap_" + 4 + ".png");
			ImageIO.write(image, "png", replace);
			// top left (CORRECT) // need to do 180 degree flip
			ImageIO.write(skyMap.getSubimage(0, 0, x, y), "png", new File(path + "cubemap_" + 5 + ".png"));
			File input2 = new File(path + "cubemap_" + 5 + ".png");
			BufferedImage lol2 = ImageIO.read(new File(input2.getAbsolutePath()));
			AffineTransform tx2 = AffineTransform.getScaleInstance(-1, -1);
			AffineTransformOp op2 = new AffineTransformOp(tx2, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			tx2.translate(lol2.getWidth(null) * -1, lol2.getHeight(null) * -1);
			op2 = new AffineTransformOp(tx2, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			BufferedImage image2 = op2.filter(lol2, null);
			File replace2 = new File(path + "cubemap_" + 5 + ".png");
			ImageIO.write(image2, "png", replace2);
		}

	}

	public static void panorama(File file, String packName, String packPath) {
		try {
			File panorama = (new File(file + "//assets//minecraft//textures//gui//title//background//"));
			if (panorama.exists()) {
				Window.progressConsoleUpdate("Creating Panorama");
				new File(packPath + packName + "//textures//gui//background").mkdirs();
				FileUtils.copyDirectoryToDirectory(panorama, new File(packPath + packName + "//textures//gui//"));
			}
			System.out.println("copied over panorama");
		} catch (Exception e) {
			System.out.println("failed to port panorama");
		}

	}

	public static void sounds(File file, String packName, String packPath) {
		try {
			Window.progressConsoleUpdate("Porting Sounds");
			File soundsDir = new File(file + "//assets//minecraft//sounds");
			File dest = new File(packPath + packName);
			FileUtils.copyDirectoryToDirectory(soundsDir, dest);
		} catch (Exception e) {
			System.out.println("failed to port sounds, pack might not cotain sounds");
		}
	}

	public static void armor(File file, String packName, String packPath) {
		Window.progressConsoleUpdate("Porting Armor");
		String path = packPath + packName + "\\textures\\models\\armor\\";

		String[] oldNames = { "diamond_layer_1.png",
				"diamond_layer_2.png",
				"chainmail_layer_1.png",
				"chainmail_layer_2.png",
				"gold_layer_1.png",
				"gold_layer_2.png",
				"iron_layer_1.png",
				"iron_layer_2.png",
				"leather_layer_1.png",
				"leather_layer_2.png" };

		String[] newNames = { "diamond_1.png", "diamond_2.png", "chain_1.png", "chain_2.png", "gold_1.png", "gold_2.png", "iron_1.png", "iron_2.png", "cloth_1.png", "cloth_2.png" };

		for (int i = 0; i < oldNames.length; i++) {
			try {
				File original = new File(path + oldNames[i]);
				File newName = new File(path + newNames[i]);
				original.renameTo(newName);
			} catch (Exception e) {
				System.out.println("error renaming armor");
			}
		}
	}

	public static void colorMap(File file, String packName, String packPath) {
		try {
			Window.progressConsoleUpdate("Porting Color Map");
			File source = new File(file + "\\assets\\minecraft\\mcpatcher\\colormap");
			File dest = new File(packPath + packName + "\\textures\\colormap");
			FileUtils.copyDirectory(source, dest);
		} catch (Exception e) {
			System.out.println("failed to locate colormap");
		}
	}

	public static void environment(File file, String packName, String packPath) {
		try {
			Window.progressConsoleUpdate("Porting Environment");
			new File(packPath + packName + "\\textures\\environment\\overworld_cubemap").mkdirs();
			try {
				for (int i = 0; i < 10; i++) {
					File source = new File(file + "\\assets\\minecraft\\textures\\blocks\\destroy_stage_" + i + ".png");
					File dest = new File(packPath + packName + "\\textures\\environment\\destroy_stage_" + i + ".png");
					FileUtils.copyFile(source, dest);
				}
			} catch (Exception e) {
				System.out.println("failed to move destroy stage pngs | most likely do not exist");
			}
		} catch (Exception e) {
			System.out.println("failed to port environment folder");
		}

	}

	public static void font(File file, String packName, String packPath) {
		try {
			Window.progressConsoleUpdate("Porting Font");
			File source = new File(file + "\\assets\\minecraft\\mcpatcher\\font"); // copy the font
			File dest = new File(packPath + packName + "\\font");
			FileUtils.copyDirectory(source, dest);
		} catch (Exception e) {
			System.out.println("could not find font folder, trying other location");
			try {
				File source = new File(file + "\\assets\\minecraft\\font");
				File dest = new File(packPath + packName + "\\font");
				FileUtils.copyDirectory(source, dest);
				System.out.println("found font folder in other location(s), ported successfully");
			} catch (Exception b) {
				try {
					// sometimes font will be outside the mcpatcher folder
					File source = new File(file + "\\assets\\minecraft\\textures\\font");
					File dest = new File(packPath + packName + "\\font");
					FileUtils.copyDirectory(source, dest);
					System.out.println("found font folder in other location(s), ported successfully");
				} catch (Exception c) {
					System.out.println("could not find a font file");
				}
			}
		}

		try { // now rename the font from ascii to default8 so it works on bedrock
			String path = packPath + packName + "\\font\\";
			File original = new File(path + "ascii.png");
			File newName = new File(path + "default8.png");
			original.renameTo(newName);
		} catch (Exception e) {
			System.out.println("failed to port font");
		}

	}

	public static void gui(File file, String packName, String packPath) {
		try {
			Window.progressConsoleUpdate("Porting GUI");
			String path = packPath + packName + "\\textures\\gui\\";
			File original = new File(path + "widgets.png");
			File newName = new File(path + "gui.png");
			original.renameTo(newName);
		} catch (Exception e) {
			System.out.println("failed to port gui");
		}
	}

	public static void crossHairFix(File file, String packName, String packPath) {
		try {
			Window.progressConsoleUpdate("Porting Cross Hair");
			String path = packPath + packName + "\\textures\\gui\\";
			File icons = new File(path + "icons.png");
			BufferedImage readIcons = ImageIO.read(icons);
			// cross hair boxes are base 16 according to icons.png size
			int crossHairSize = readIcons.getWidth() / 16;
			ImageIO.write(readIcons.getSubimage(0, 0, crossHairSize, crossHairSize), "png", new File(path + "cross_hair.png"));
			BufferedImage image = new BufferedImage(crossHairSize, crossHairSize, BufferedImage.TYPE_INT_ARGB);
			Graphics g = image.getGraphics();
			g.setColor(Color.black);
			g.fillRect(0, 0, image.getWidth(), image.getHeight());
			g.drawImage(readIcons, 0, 0, null);
			g.dispose();
			ImageIO.write(image, "png", new File(path + "cross_hair.png"));
			// we need to move cross_hair.png into UI folder now
			File crosshair = new File(path + "cross_hair.png");
			File dest = new File(packPath + packName + "\\textures\\ui\\cross_hair.png");
			FileUtils.copyFile(crosshair, dest);
			crosshair.delete(); // get rid of the old one
		} catch (Exception e) {
			System.out.println("failed to port cross hair");
		}
	}

	public static void fire(File file, String packName, String packPath) {
		try {
			Window.progressConsoleUpdate("Porting Fire");
			String path = packPath + packName + "\\textures\\blocks\\";
			File original = new File(path + "fire_layer_1.png");
			File newName = new File(path + "fire_1.png");
			original.renameTo(newName);
			File original2 = new File(path + "fire_layer_0.png");
			File newName2 = new File(path + "fire_0.png");
			original2.renameTo(newName2);
		} catch (Exception e) {
			System.out.println("failed to port fire");
		}
	}

	public static void painting(File file, String packName, String packPath) {
		try {
			Window.progressConsoleUpdate("Porting Painting");
			String path = packPath + packName + "\\textures\\painting\\";
			File original = new File(path + "paintings_kristoffer_zetterstrand.png");
			File newName = new File(path + "kz.png");
			original.renameTo(newName);
		} catch (Exception e) {
			System.out.println("failed to port painting");
		}
	}

	// entire gui container UI port as well (chests and that stuff)
	public static void inventoryPort(File file, String packName, String packPath) throws IOException {
		try {
			Window.progressConsoleUpdate("Porting Inventory and Container GUI");
			String path = packPath + packName + "\\";
			// first thing to do is to put in the UI folder
			Path destUI = Paths.get(path.replaceAll("\\\\", "\\/"));
			File UI = new File(System.getenv("APPDATA") + "\\Swim Services\\assets\\ui");
			FileUtils.copyDirectoryToDirectory(UI, new File(destUI.toString()));
			// uidx
			Path destUIDX = Paths.get(path.replaceAll("\\\\", "\\/"));
			File UIDX = new File(System.getenv("APPDATA") + "\\Swim Services\\assets\\uidx");
			FileUtils.copyDirectoryToDirectory(UIDX, new File(destUIDX.toString()));
			// uidx textures folder
			Path texturesUIDXdest = Paths.get(path.replaceAll("\\\\", "\\/") + "textures");
			File texturesUIDX = new File(System.getenv("APPDATA") + "\\Swim Services\\assets\\textures_uidx\\uidx");
			FileUtils.copyDirectoryToDirectory(texturesUIDX, new File(texturesUIDXdest.toString()));
			// java assets
			File assets = (new File(file + "\\assets\\minecraft\\textures\\gui\\container"));
			File assetsDest = (new File(packPath + packName + "\\assets\\minecraft\\textures\\gui\\container\\"));
			try {
				FileUtils.copyDirectory(assets, assetsDest);
			} catch (Exception ex) {
				System.out.println("pack being ported does not have a container");
			}
			// recipe book
			File recipeBook = new File(System.getenv("APPDATA") + "\\Swim Services\\assets\\recipe_book");
			FileUtils.copyDirectoryToDirectory(recipeBook, new File(path + "assets\\uidx\\textures\\gui\\container"));
			FileUtils.copyDirectoryToDirectory(recipeBook, new File(texturesUIDXdest.toString()));
			// container
			File containerDest = (new File(packPath + packName + "\\assets\\minecraft\\textures\\gui\\container\\"));
			File container = (new File(file + "\\assets\\minecraft\\textures\\gui\\container"));
			try {
				FileUtils.copyDirectory(container, containerDest);
			} catch (Exception ex) {
				System.out.println("pack being ported does not have a container for assets");
			}
			// chest and end chest
			File generic54 = (new File(packPath + packName + "\\assets\\minecraft\\textures\\gui\\container\\generic_54.png"));
			File newGeneric = (new File(packPath + packName + "\\assets\\uidx\\textures\\gui\\container\\generic_54.png"));

			try {
				FileUtils.copyFile(generic54, newGeneric);
				File renameEndChest = (new File(packPath + packName + "\\assets\\uidx\\textures\\gui\\container\\generic_54.png"));
				File ender_chest = (new File(packPath + packName + "\\assets\\uidx\\textures\\gui\\container\\ender_chest.png"));
				renameEndChest.renameTo(ender_chest);
			} catch (Exception e) {
				System.out.println("generic54.png does not exist");
			}

			try {
				FileUtils.copyFile(generic54, newGeneric);
				File renameChest = (new File(packPath + packName + "\\assets\\uidx\\textures\\gui\\container\\generic_54.png"));
				File small_chest = (new File(packPath + packName + "\\assets\\uidx\\textures\\gui\\container\\small_chest.png"));
				renameChest.renameTo(small_chest);
			} catch (Exception e) {
				System.out.println("generic54.png does not exist");
			}

			File inventoryCreativeCheck = new File(packPath + packName + "\\assets\\minecraft\\textures\\gui\\container\\creative_inventory");
			if (!(inventoryCreativeCheck.exists())) {
				File creative = new File(System.getenv("APPDATA") + "\\Swim Services\\assets\\container\\creative_inventory");
				FileUtils.copyDirectoryToDirectory(creative, inventoryCreativeCheck.getParentFile());
				System.out.println("default creative inventory added");
			}
			
			try {
				// handle this later in check catch if it fails
				FileUtils.copyFile(generic54, newGeneric);
				File renameChest = (new File(packPath + packName + "\\assets\\uidx\\textures\\gui\\container\\generic_54.png"));
				File small_chest = (new File(packPath + packName + "\\assets\\uidx\\textures\\gui\\container\\small_chest.png"));
				renameChest.renameTo(small_chest);
			} catch (Exception e) {
				System.out.println("can not find generic54, copying over default container assets");
				File containerDir = new File(System.getenv("APPDATA") + "\\Swim Services\\assets\\container\\");
				new File(System.getenv("APPDATA") + "\\Swim Services\\" + packName + "\\tempIMGS");
				File tempIMGS = new File(System.getenv("APPDATA") + "\\Swim Services\\" + packName + "\\tempIMGS");
				FileUtils.copyDirectoryToDirectory(containerDir, tempIMGS);
				File temps = new File(tempIMGS + "\\container");
				File[] imgs = temps.listFiles();
				for (File currentGUI : imgs) {
					File checker = new File(packPath + packName + "\\assets\\minecraft\\textures\\gui\\container\\" + currentGUI.getName());
					if (!checker.exists() && !currentGUI.isDirectory()) {
						System.out.println("Importing default container image " + currentGUI);
						try {
							FileUtils.copyFile(currentGUI, checker);
						} catch (Exception ex) {
							System.out.println("save location does not exist (no container)");
						}
					}
				}
				try {
				// small_chest handling from here
				File uidxContainer = new File(packPath + packName + "\\assets\\uidx\\textures\\gui\\container");
				File generic = new File(packPath + packName + "\\assets\\minecraft\\textures\\gui\\container\\generic_54.png");
				File genDest = new File(uidxContainer + "\\generic_54.png");
				FileUtils.copyFile(generic, genDest);
				File smallChest = new File(uidxContainer + "\\small_chest.png");
				genDest.renameTo(smallChest);
				FileUtils.copyFile(generic, genDest);
				File eChest = new File(uidxContainer + "\\ender_chest.png");
				genDest.renameTo(eChest);
				FileUtils.copyFile(generic, genDest);
				} catch(Exception e1) {
					System.out.println("failed to port small_chest.png");
				}
			}
		} catch (Exception e) {
			System.out.println("failed to port container");
		}

		// scale method via global_variables.json editing
		try {
			int aspect;
			Path global_variables = Paths.get(packPath + packName + "\\ui\\_global_variables.json");
			Charset charset = StandardCharsets.UTF_8;
			String content = new String(Files.readAllBytes(global_variables), charset);
			// inventory scaling
			File inv = (new File(packPath + packName + "\\assets\\minecraft\\textures\\gui\\container\\inventory.png"));
			BufferedImage inventory = ImageIO.read(inv);
			aspect = inventory.getHeight();
			content = content.replaceAll("\"\\$inventory_resolution\"\\: \"256x\",", "\"\\$inventory_resolution\"\\: \"" + String.valueOf(aspect) + "x\",");
			Files.write(global_variables, content.getBytes(charset));
			// generic 54 scaling
			File generic_54 = (new File(packPath + packName + "\\assets\\minecraft\\textures\\gui\\container\\generic_54.png"));
			BufferedImage gen = ImageIO.read(generic_54);
			aspect = gen.getHeight();
			content = content.replaceAll("\"\\$generic_54_resolution\"\\: \"256x\",", "\"\\$generic_54_resolution\"\\: \"" + String.valueOf(aspect) + "x\",");
			Files.write(global_variables, content.getBytes(charset));
			// brewing stand
			File brewing = (new File(packPath + packName + "\\assets\\minecraft\\textures\\gui\\container\\brewing_stand.png"));
			BufferedImage brew = ImageIO.read(brewing);
			aspect = brew.getHeight();
			content = content.replaceAll("\"\\$brewing_stand_resolution\"\\: \"256x\",", "\"\\$brewing_stand_resolution\"\\: \"" + String.valueOf(aspect) + "x\",");
			Files.write(global_variables, content.getBytes(charset));
			// crafting table
			File crafting = (new File(packPath + packName + "\\assets\\minecraft\\textures\\gui\\container\\crafting_table.png"));
			BufferedImage craft = ImageIO.read(crafting);
			aspect = craft.getHeight();
			content = content.replaceAll("\"\\$crafting_table_resolution\"\\: \"256x\",", "\"\\$crafting_table_resolution\"\\: \"" + String.valueOf(aspect) + "x\",");
			Files.write(global_variables, content.getBytes(charset));
			// furnace and blast furnace and smoker
			File furnace = (new File(packPath + packName + "\\assets\\minecraft\\textures\\gui\\container\\furnace.png"));
			BufferedImage furn = ImageIO.read(furnace);
			aspect = furn.getHeight();
			content = content.replaceAll("\"\\$furnace_resolution\"\\: \"256x\",", "\"\\$furnace_resolution\"\\: \"" + String.valueOf(aspect) + "x\",");
			Files.write(global_variables, content.getBytes(charset));
			content = content.replaceAll("\"\\$blast_furnace_resolution\"\\: \"256x\",", "\"\\$blast_furnace_resolution\"\\: \"" + String.valueOf(aspect) + "x\",");
			Files.write(global_variables, content.getBytes(charset));
			content = content.replaceAll("\"\\$smoker_resolution\"\\: \"256x\",", "\"\\$smoker_resolution\"\\: \"" + String.valueOf(aspect) + "x\",");
			Files.write(global_variables, content.getBytes(charset));
			// enchantment table
			File enchantment = (new File(packPath + packName + "\\assets\\minecraft\\textures\\gui\\container\\enchanting_table.png"));
			BufferedImage ench = ImageIO.read(enchantment);
			aspect = ench.getHeight();
			content = content.replaceAll("\"\\$enchanting_table_resolution\"\\: \"256x\",", "\"\\$enchanting_table_resolution\"\\: \"" + String.valueOf(aspect) + "x\",");
			Files.write(global_variables, content.getBytes(charset));
			// anvil
			File anvil = (new File(packPath + packName + "\\assets\\minecraft\\textures\\gui\\container\\anvil.png"));
			BufferedImage anv = ImageIO.read(anvil);
			aspect = anv.getHeight();
			content = content.replaceAll("\"\\$anvil_resolution\"\\: \"256x\",", "\"\\$anvil_resolution\"\\: \"" + String.valueOf(aspect) + "x\",");
			Files.write(global_variables, content.getBytes(charset));
			// hopper
			File hopper = (new File(packPath + packName + "\\assets\\minecraft\\textures\\gui\\container\\hopper.png"));
			BufferedImage hop = ImageIO.read(hopper);
			aspect = hop.getHeight();
			content = content.replaceAll("\"\\$hopper_resolution\"\\: \"256x\",", "\"\\$hopper_resolution\"\\: \"" + String.valueOf(aspect) + "x\",");
			Files.write(global_variables, content.getBytes(charset));
			// dispenser
			File dispenser = (new File(packPath + packName + "\\assets\\minecraft\\textures\\gui\\container\\dispenser.png"));
			BufferedImage dispense = ImageIO.read(dispenser);
			aspect = dispense.getHeight();
			content = content.replaceAll("\"\\$dispenser_resolution\"\\: \"256x\",", "\"\\$dispenser_resolution\"\\: \"" + String.valueOf(aspect) + "x\",");
			Files.write(global_variables, content.getBytes(charset));
			// beacon
			File beacon = (new File(packPath + packName + "\\assets\\minecraft\\textures\\gui\\container\\beacon.png"));
			BufferedImage bea = ImageIO.read(beacon);
			aspect = bea.getHeight();
			content = content.replaceAll("\"\\$beacon_resolution\"\\: \"256x\",", "\"\\$beacon_resolution\"\\: \"" + String.valueOf(aspect) + "x\",");
			Files.write(global_variables, content.getBytes(charset));
			// horse
			File horse = (new File(packPath + packName + "\\assets\\minecraft\\textures\\gui\\container\\horse.png"));
			BufferedImage hor = ImageIO.read(horse);
			aspect = hor.getHeight();
			content = content.replaceAll("\"\\$horse_resolution\"\\: \"256x\",", "\"\\$horse_resolution\"\\: \"" + String.valueOf(aspect) + "x\",");
			Files.write(global_variables, content.getBytes(charset));
		} catch (Exception e) {
			System.out.println("some files had to be defaulted for container packaging");
		}
	}

	public static void cleanUp(String packName, String packPath) throws IOException {
		try {
			Window.progressConsoleUpdate("Zipping to MCPACK Format..");
			File pack = new File(packPath + packName);
			compress(pack.getAbsolutePath()); // this copies the file to a zip
			File zippedPack = new File(packPath + packName + ".zip");
			File mcpack = new File(packPath + packName + ".mcpack");
			zippedPack.renameTo(mcpack); // the fact that this even works this easily is incredible
			FileUtils.deleteDirectory(pack); // dont need it any more now that we have our mcpack
			Window.textArea.setText(" Successfully Ported Pack: \n " + mcpack);
			Window.textArea.setSelectedTextColor(Color.GREEN);
			Window.textArea.setForeground(Color.GREEN);
			Window.cleanAppData();
		} catch (Exception e) {
			System.out.println("failed in final port stage clean up");
		}
	}

	public static void compress(String dirPath) {
		final Path sourceDir = Paths.get(dirPath);
		String zipFileName = dirPath.concat(".zip");
		try {
			final ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(zipFileName));
			Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
					try {
						Path targetFile = sourceDir.relativize(file);
						outputStream.putNextEntry(new ZipEntry(targetFile.toString()));
						byte[] bytes = Files.readAllBytes(file);
						outputStream.write(bytes, 0, bytes.length);
						outputStream.closeEntry();
					} catch (IOException e) {
						System.out.println("file compression error");
					}
					return FileVisitResult.CONTINUE;
				}
			});
			outputStream.close();
		} catch (IOException e) {
			System.out.println("directory/zip not found");
		}
	}

	public static class UUIDGenerator {
		public static UUID generateType1UUID() {
			long most64SigBits = get64MostSignificantBitsForVersion1();
			long least64SigBits = get64LeastSignificantBitsForVersion1();
			return new UUID(most64SigBits, least64SigBits);
		}

		private static long get64LeastSignificantBitsForVersion1() {
			Random random = new Random();
			long random63BitLong = random.nextLong() & 0x3FFFFFFFFFFFFFFFL;
			long variant3BitFlag = 0x8000000000000000L;
			return random63BitLong + variant3BitFlag;
		}

		private static long get64MostSignificantBitsForVersion1() {
			LocalDateTime start = LocalDateTime.of(1582, 10, 15, 0, 0, 0);
			Duration duration = Duration.between(start, LocalDateTime.now());
			long seconds = duration.getSeconds();
			long nanos = duration.getNano();
			long timeForUuidIn100Nanos = seconds * 10000000 + nanos * 100;
			long least12SignificatBitOfTime = (timeForUuidIn100Nanos & 0x000000000000FFFFL) >> 4;
			long version = 1 << 12;
			return (timeForUuidIn100Nanos & 0xFFFFFFFFFFFF0000L) + version + least12SignificatBitOfTime;
		}
	}

}
