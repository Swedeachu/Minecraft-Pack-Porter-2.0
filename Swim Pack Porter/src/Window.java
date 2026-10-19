import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.Color;

public class Window {

	public static JFrame frame = new JFrame("Swim Services Pack Porter v2.0");
	public static JTextArea textArea = new JTextArea();
	public static JButton btnPortSky;
	public static String packSavePath;
	public static String skySavePath;
	public static String currentDraggedFile;
	public static String workerSkyPath;
	public static String workerPackPath;
	public static boolean busy = false;
	public static List<String> configs = new ArrayList<>();
	public static String batchSavePath = null; // only set while a pack folder is being batch ported, overrides the save location
	public static String batchStatus = ""; // "Pack 2 of 5: name" header shown above the current task during a batch

	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException {
		String path = new File(".").getCanonicalPath();
		File assets = new File(path + "\\assets");
		try {
			if (assets.exists()) {
				System.out.println("assets folder found");
				File assetsDest = new File(System.getenv("APPDATA") + "\\Swim Services");
				FileUtils.copyDirectoryToDirectory(assets, assetsDest);
			} else {
				System.out.println("assets folder not found");
			}
		} catch (Exception e) {
			System.out.println("Error moving assets folder");
		}
		System.out.println(path);
		File swimFolder = new File(System.getenv("APPDATA") + "\\Swim Services");
		try {
			if (swimFolder.exists()) {
				cleanAppData();
			}
		} catch (Exception e) {
			System.out.println("Swim folder not found");
		}
		File home = FileSystemView.getFileSystemView().getHomeDirectory();
		String desktop = home.getAbsolutePath();
		String defaultCube = desktop + "\\overworld_cubemap";
		// first thing to do is load config file
		File config = new File(System.getenv("APPDATA") + "\\Swim Services\\swim_pack.config");
		if (config.exists()) {
			System.out.println("config file found");
			// read the configs
			configs.clear();
			Scanner configReader = new Scanner(config);
			while (configReader.hasNextLine()) {
				String data = configReader.nextLine();
				configs.add(data);
			}
			System.out.println(configs.get(0));
			System.out.println(configs.get(1));
			System.out.println(configs.get(2));
			packSavePath = configs.get(1);
			skySavePath = configs.get(2);
			configReader.close();
		} else {
			System.out.println("config not found or failed to load, generating new config file");
			new File(System.getenv("APPDATA") + "\\Swim Services").mkdirs();
			config.createNewFile();
			configs.clear();
			FileWriter configWriter = new FileWriter(config);
			configWriter.write("true\n");
			configWriter.write(desktop + "\n");
			configWriter.write(defaultCube);
			configWriter.close();
			Scanner configReader = new Scanner(config);
			while (configReader.hasNextLine()) {
				String data = configReader.nextLine();
				configs.add(data);
			}
			System.out.println(configs.get(0));
			System.out.println(configs.get(1));
			packSavePath = configs.get(1);
			System.out.println(configs.get(2));
			skySavePath = configs.get(2);
			configReader.close();
		}

		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		frame.getContentPane().setBackground(new Color(51, 51, 51));

		frame.setResizable(false);
		frame.setBounds(100, 100, 900, 500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		ImageIcon logo = new ImageIcon(loadImage("packConvert.png"));
		frame.setIconImage(logo.getImage());

		JButton portButton = new JButton("Select Pack To Port");
		portButton.setForeground(new Color(0, 0, 0));
		portButton.setBackground(new Color(0, 0, 0));
		portButton.setFont(new Font("Tahoma", Font.PLAIN, 30));
		portButton.setBounds(225, 335, 450, 110);
		frame.getContentPane().add(portButton);
		updateConsoleText("Swim Services Minecraft Texture Pack Porter v2.0\n Now Supports MCPACK to Java Conversion and File Upload Drag and Drop\n Drop (or select) a folder of packs to port all of them in one go", true);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(48, 23, 800, 295);
		frame.getContentPane().add(scrollPane);
		textArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
		scrollPane.setViewportView(textArea);

		textArea.setForeground(new Color(153, 153, 153));
		textArea.setBackground(new Color(0, 0, 0));
		textArea.setEditable(false);

		JButton btnPortSky = new JButton();
		btnPortSky.setText("Port Sky");
		btnPortSky.setForeground(Color.BLACK);
		btnPortSky.setFont(new Font("Tahoma", Font.PLAIN, 27));
		btnPortSky.setBackground(Color.BLACK);
		btnPortSky.setBounds(48, 336, 137, 110);
		frame.getContentPane().add(btnPortSky);

		JButton btnSettings = new JButton("Settings");
		btnSettings.setForeground(Color.BLACK);
		btnSettings.setFont(new Font("Tahoma", Font.PLAIN, 27));
		btnSettings.setBackground(Color.BLACK);
		btnSettings.setBounds(711, 335, 137, 110);
		frame.getContentPane().add(btnSettings);

		frame.setVisible(true);

		// individual sky porting
		btnPortSky.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnPortSky.setText("Porting..");
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setCurrentDirectory(new File("C:\\Users\\" + System.getProperty("user.name") + "\\Desktop")); // sets current directory
				fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				fileChooser.setAcceptAllFileFilterUsed(false);
				int response = fileChooser.showOpenDialog(null);
				if (response == JFileChooser.APPROVE_OPTION) {
					String directCheck = configs.get(2);
					File checker = new File(directCheck);
					if (!checker.exists()) {
						System.out.println(directCheck + " not found, creating directory");
						new File(directCheck).mkdirs();
					} else {
						btnPortSky.setText("Port Sky");
						System.out.println(directCheck + " found");
					}
					processSkyPort(fileChooser.getSelectedFile().getAbsolutePath());
				} else {
					btnPortSky.setText("Port Sky");
				}
			}
		});

		// drag and drop sky porting
		btnPortSky.setDropTarget(new DropTarget() {
			private static final long serialVersionUID = 1L;

			public synchronized void drop(DropTargetDropEvent evt) {
				try {
					evt.acceptDrop(DnDConstants.ACTION_COPY);
					@SuppressWarnings("unchecked")
					List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
					int i = 0;
					for (File currentFile : droppedFiles) {
						System.out.println(currentFile);
						currentDraggedFile = currentFile.getAbsolutePath();
						i++;
					}
					if (i > 1) {
						System.out.println("to many files");
						updateConsoleText("Only port one sky at a time!", true);
					} else {
						processSkyPort(currentDraggedFile);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});

		// drag and drop pack porting
		portButton.setDropTarget(new DropTarget() {
			private static final long serialVersionUID = 1L;

			public synchronized void drop(DropTargetDropEvent evt) {
				try {
					if (!busy) {
						evt.acceptDrop(DnDConstants.ACTION_COPY);
						@SuppressWarnings("unchecked")
						List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
						int i = 0;
						for (File currentFile : droppedFiles) {
							System.out.println(currentFile);
							currentDraggedFile = currentFile.getAbsolutePath();
							i++;
						}
						if (i > 1) {
							System.out.println("to many files");
							updateConsoleText("Only port one pack at a time!", true);
						} else {
							processPackPort(currentDraggedFile);
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});

		// texture pack porting
		portButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!busy) {
					portButton.setText("Currently Porting Pack..");
					JFileChooser fileChooser = new JFileChooser();
					fileChooser.setCurrentDirectory(new File("C:\\Users\\" + System.getProperty("user.name") + "\\Desktop")); // sets current directory
					fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
					fileChooser.setAcceptAllFileFilterUsed(false);
					int response = fileChooser.showOpenDialog(null);
					if (response == JFileChooser.APPROVE_OPTION) {
						processPackPort(fileChooser.getSelectedFile().getAbsolutePath());
					} else {
						portButton.setText("Select Pack To Port");
					}
				}
			}
		});

		// open the settings tab
		btnSettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Settings.initialize();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	public static void processPackPort(String uploadedFile) {
		String directCheck = configs.get(1);
		File checker = new File(directCheck);
		if (!checker.exists()) {
			System.out.println(directCheck + " not found, creating directory");
			new File(directCheck).mkdirs();
		} else {
			System.out.println(directCheck + " found");
		}
		File file = new File(uploadedFile); // this is the pack which we will port
		if (BatchPort.isBatchFolder(file)) { // a folder full of packs instead of a single pack
			processBatchPort(file);
			return;
		}
		workerPackPath = uploadedFile;
		try {
			String extension = FilenameUtils.getExtension(file.getAbsolutePath()).toLowerCase();
			System.out.println("extension is: " + extension);
			if (extension.equals("zip")) {
				String name = FilenameUtils.removeExtension(file.getName());
				File zipPack = new File(System.getenv("APPDATA") + "\\Swim Services\\" + name);
				FileUtils.copyFileToDirectory(file, zipPack.getParentFile());
				try {
					busy = true;
					textArea.setText("");
					UnzipUtility.extractFolder(zipPack.getAbsolutePath() + ".zip");
					workerPackPath = zipPack.getAbsolutePath();
					File mcmetaCheck = new File(workerPackPath + "\\pack.mcmeta");
					if (mcmetaCheck.exists()) {
						Port task = new Port();
						task.execute();
						FileUtils.forceDelete(new File(zipPack + ".zip"));
					} else {
						System.out.println("mcmeta not found, attempting to go one directory deeper");
						File[] deepMeta = new File(workerPackPath).listFiles();
						for (File metaSearch : deepMeta) {
							System.out.println(metaSearch.getAbsolutePath());
							if (metaSearch.isDirectory()) {
								mcmetaCheck = new File(metaSearch.getAbsolutePath() + "\\pack.mcmeta");
								if (mcmetaCheck.exists()) {
									System.out.println("found mcmeta: " + mcmetaCheck.getAbsolutePath() + " | parent dir to port: " + mcmetaCheck.getParent());
									workerPackPath = mcmetaCheck.getParent();
									Port task = new Port();
									task.execute();
									break;
								}
							}
						}
					}
				} catch (Exception ex) {
					textArea.setSelectedTextColor(Color.RED);
					textArea.setForeground(Color.RED);
					// this shouldn't happen unless the client does something very weird
					textArea.setText(" File Extraction Error\n If this happens just manually extract the folder then try porting it again");
					busy = false;
					FileUtils.forceDelete(new File(zipPack + ".zip"));
					cleanAppData();
					ex.printStackTrace();
				}
			} else {
				if (extension.equals("mcpack")) {
					busy = true;
					textArea.setText("");
					JavaPort task = new JavaPort();
					task.execute();
				} else {
					busy = true;
					textArea.setText("");
					Port task = new Port();
					task.execute();
				}
			}
		} catch (Exception e1) {
			textArea.setSelectedTextColor(Color.RED);
			textArea.setForeground(Color.RED);
			// this shouldn't happen ever
			textArea.setText(" Unknown File Processing Error");
			e1.printStackTrace();
		}
	}

	public static void processBatchPort(File folder) {
		List<File> packs = BatchPort.findPacks(folder);
		System.out.println("batch porting " + packs.size() + " packs from: " + folder.getAbsolutePath());
		busy = true;
		BatchPort task = new BatchPort(folder, packs);
		task.execute();
	}

	// where ported packs get written, the batch output folder while a batch is running, otherwise the configured save location
	public static String currentPackSavePath() {
		return batchSavePath != null ? batchSavePath : configs.get(1);
	}

	public static void processSkyPort(String uploadedFile) {
		busy = true;
		String directCheck = configs.get(2);
		File checker = new File(directCheck);
		if (!checker.exists()) {
			System.out.println(directCheck + " not found, creating directory");
			new File(directCheck).mkdirs();
		} else {
			System.out.println(directCheck + " found");
		}
		File file = new File(uploadedFile); // this is the sky which we will port
		System.out.println(file);
		try {
			String extension = FilenameUtils.getExtension(file.toString()).toLowerCase();
			if (extension.equals("png")) { // java sky to cubemap
				System.out.println("java sky porting to mcpe cubemap");
				workerSkyPath = uploadedFile;
				SkyPort task = new SkyPort();
				task.execute();
			} else {
				if (file.isDirectory()) { // cubemap to java sky
					System.out.println("mcpe cubemap porting to java sky");
					DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd HH_mm_ss");
					LocalDateTime now = LocalDateTime.now();
					String skyLocation = Window.configs.get(2) + "\\java sky " + dtf.format(now) + "\\";
					new File(Window.configs.get(2) + "\\java sky " + dtf.format(now) + "\\").mkdirs();
					try {
						workerSkyPath = uploadedFile;
						JavaSky task = new JavaSky();
						task.execute();
					} catch (Exception e) { // if not a proper cubemap folder
						textArea.setText("");
						updateConsoleText("Error porting sky:\n " + file.getAbsolutePath() + "\n not a proper cubemap folder", true);
						System.out.println("failed to turn cubemap folder: " + file.getAbsolutePath() + " into java sky");
						FileUtils.forceDelete(new File(skyLocation));
						busy = false;
						e.printStackTrace();
					}
				} else { // if not a png
					System.out.println("error porting sky");
					textArea.setText("");
					updateConsoleText("Error porting sky:\n " + file.getAbsolutePath() + "\n not a png or proper cubemap folder", true);
					busy = false;
				}
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			busy = false;
			e1.printStackTrace();
		}
	}

	public static void updateConsoleText(String text, Boolean fail) {
		if (fail) {
			textArea.setSelectedTextColor(Color.RED);
			textArea.setForeground(Color.RED);
		} else {
			textArea.setSelectedTextColor(Color.GREEN);
			textArea.setForeground(Color.GREEN);
		}
		textArea.append(" " + text + "\n");
	}

	public static void progressConsoleUpdate(String text) {
		textArea.setSelectedTextColor(Color.WHITE);
		textArea.setForeground(Color.WHITE);
		String header = batchStatus.isEmpty() ? "" : " " + batchStatus + "\n";
		textArea.setText(header + " Current Task: " + text);
	}

	public static BufferedImage loadImage(String str) throws IOException {
		BufferedImage image = ImageIO.read(Window.class.getResource(str));
		return image;
	}

	public static void cleanAppData() throws IOException {
		File swimServicesDir = new File(System.getenv("APPDATA") + "\\Swim Services");
		File[] swimFiles = swimServicesDir.listFiles();
		for (File file : swimFiles) {
			if (!(file.getName().equals("swim_pack.config")) && !(file.getName().equals("assets"))) {
				FileUtils.forceDelete(file);
				System.out.println("cleaning out file: " + file.getAbsolutePath());
			}
		}
	}

}
