import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingWorker;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

/**
 * Ports every pack found at the top level of a folder, one after another.
 * Picks up the same things the normal porter accepts: Java pack folders (contain a pack.mcmeta),
 * Java .zip packs and Bedrock .mcpack files. Ported packs are written to a fresh
 * "<folder name> PORTED" folder inside the usual save location.
 */
public class BatchPort extends SwingWorker<Void, String> {

	private final File folder;
	private final List<File> packs;
	private final List<String> failures = new ArrayList<>();
	private int succeeded = 0;
	private String outputPath = "";

	public BatchPort(File folder, List<File> packs) {
		this.folder = folder;
		this.packs = packs;
	}

	// a folder with a pack.mcmeta is a single pack, not a folder of packs
	public static boolean isPack(File dir) {
		return new File(dir, "pack.mcmeta").isFile();
	}

	// every pack sitting directly inside the folder, sorted by name
	public static List<File> findPacks(File folder) {
		List<File> found = new ArrayList<>();
		File[] children = folder.listFiles();
		if (children == null) {
			return found;
		}
		for (File child : children) {
			if (child.isDirectory()) {
				if (isPack(child)) {
					found.add(child);
				}
			} else {
				String extension = FilenameUtils.getExtension(child.getName()).toLowerCase();
				if (extension.equals("zip") || extension.equals("mcpack")) {
					found.add(child);
				}
			}
		}
		found.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
		return found;
	}

	// true when the folder is not a pack itself but holds at least one pack
	public static boolean isBatchFolder(File folder) {
		return folder.isDirectory() && !isPack(folder) && !findPacks(folder).isEmpty();
	}

	@Override
	protected Void doInBackground() {
		// never reuse an old output folder, re-running a batch should not collide with earlier results
		File saveDir = uniqueDir(new File(Window.configs.get(1)), folder.getName() + " PORTED");
		outputPath = saveDir.getAbsolutePath();
		saveDir.mkdirs();
		Window.batchSavePath = outputPath;
		try {
			for (int i = 0; i < packs.size(); i++) {
				File pack = packs.get(i);
				Window.batchStatus = "Pack " + (i + 1) + " of " + packs.size() + ": " + pack.getName();
				Window.textArea.setText(" " + Window.batchStatus + "\n");
				Port.originalDescription = null; // keep each pack as if it were ported on its own, don't inherit the last pack's description
				try {
					portOne(pack);
					succeeded++;
				} catch (Exception e) {
					String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
					failures.add(pack.getName() + " (" + reason + ")");
					e.printStackTrace();
				} finally {
					// the single pack porters clear the busy flag when they finish, keep it set until the whole batch is done
					Window.busy = true;
				}
			}
		} finally {
			Window.batchSavePath = null;
			Window.batchStatus = "";
			Window.busy = false;
			System.out.println("batch finished, no longer busy");
		}
		return null;
	}

	@Override
	protected void done() {
		try {
			get(); // surfaces anything that killed the whole batch
		} catch (Exception e) {
			failures.add("batch stopped early (" + e.getCause() + ")");
		}
		boolean ok = failures.isEmpty();
		Window.textArea.setText("");
		Window.updateConsoleText("Batch complete: " + succeeded + " of " + packs.size() + " packs ported", !ok);
		Window.updateConsoleText("Saved to:\n " + outputPath, !ok);
		if (!ok) {
			Window.updateConsoleText("Failed:", true);
			for (String failure : failures) {
				Window.updateConsoleText(" " + failure, true);
			}
		}
	}

	private static void portOne(File pack) throws Exception {
		String extension = FilenameUtils.getExtension(pack.getName()).toLowerCase();
		if (extension.equals("zip")) {
			portZip(pack);
		} else if (extension.equals("mcpack")) {
			Window.workerPackPath = pack.getAbsolutePath();
			JavaPort.port(pack);
		} else {
			portJava(pack);
		}
	}

	private static void portJava(File root) throws IOException {
		Window.workerPackPath = root.getAbsolutePath();
		Port.port(root);
		if (Port.failed) {
			throw new IOException("could not locate pack.mcmeta");
		}
	}

	// same flow as dropping a single zip: copy to the Swim Services folder, extract, port, clean up
	private static void portZip(File zip) throws Exception {
		File swimDir = new File(System.getenv("APPDATA"), "Swim Services");
		String name = FilenameUtils.removeExtension(zip.getName());
		File zipCopy = new File(swimDir, name + ".zip");
		File extracted = new File(swimDir, name);
		try {
			FileUtils.copyFile(zip, zipCopy);
			UnzipUtility.extractFolder(zipCopy.getAbsolutePath());
			File root = findPackRoot(extracted);
			if (root == null) {
				throw new IOException("could not locate pack.mcmeta");
			}
			portJava(root);
		} finally {
			FileUtils.deleteQuietly(zipCopy);
			FileUtils.deleteQuietly(extracted);
		}
	}

	// pack.mcmeta is either at the top of the extracted zip or one folder down
	private static File findPackRoot(File extracted) {
		if (isPack(extracted)) {
			return extracted;
		}
		File[] children = extracted.listFiles();
		if (children != null) {
			for (File child : children) {
				if (child.isDirectory() && isPack(child)) {
					return child;
				}
			}
		}
		return null;
	}

	private static File uniqueDir(File parent, String name) {
		File dir = new File(parent, name);
		for (int n = 2; dir.exists(); n++) {
			dir = new File(parent, name + " (" + n + ")");
		}
		return dir;
	}

}
