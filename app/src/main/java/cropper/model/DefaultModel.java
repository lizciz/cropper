package cropper.model;

import java.awt.Desktop;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import util.Utils;

public class DefaultModel implements ModelAPI {

	private ModelView view;

	private Map<String, LoadedImage> imageMap;

	private final ExecutorService pool = Executors.newSingleThreadExecutor();

	public DefaultModel() {
		this.imageMap = new LinkedHashMap<>();
	}

	@Override
	public void setView(ModelView view) {
		this.view = view;
	}

	@Override
	public void openImageDirectory(File dir) {
		pool.submit(() -> {
			List<Path> imageFiles;
			try {
				imageFiles = findImageFiles(dir.toPath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			List<File> failedToLoad = new ArrayList<>();
			for (Path path : imageFiles) {
				try {
					File f = path.toFile();
					BufferedImage image = ImageIO.read(f);
					LoadedImage loadedImage = new LoadedImage(f, image);
					imageMap.put(loadedImage.getId(), loadedImage);
					view.imageWasLoaded(loadedImage.getId(), loadedImage.getFilename(), image);
				} catch (IOException e) {
					failedToLoad.add(path.toFile());
				}
			}

			if (!failedToLoad.isEmpty())
				view.imagesFailedToLoad(failedToLoad);
		});

	}

	@Override
	public void trashImage(String id) {
		pool.submit(() -> {
			LoadedImage loadedImage = imageMap.get(id);
			if (loadedImage == null) {
				return;
			}
			File file = loadedImage.getFile();
			if (Desktop.getDesktop().moveToTrash(file)) {
				imageMap.remove(id);
				view.imageWasRemoved(id);
			}
		});
	}

	@Override
	public void deleteImage(String id) {
		pool.submit(() -> {
			LoadedImage loadedImage = imageMap.get(id);
			if (loadedImage == null) {
				return;
			}
			File file = loadedImage.getFile();
			try {
				Files.deleteIfExists(file.toPath());
				imageMap.remove(id);
				view.imageWasRemoved(id);
			} catch (IOException e) {
				// TODO show error in UI
				e.printStackTrace();
			}
		});

	}

	@Override
	public void undoImageChanges(String id) {
		pool.submit(() -> {
			LoadedImage loadedImage = imageMap.get(id);
			if (loadedImage == null) {
				return;
			}
			loadedImage.undoChanges();
			view.imageWasUpdated(id, loadedImage.getImage(), loadedImage.hasUnsavedChanges());
		});
	}

	@Override
	public void redoImageChanges(String id) {
	}

	@Override
	public void performCrop(String id, final Rectangle cropRegion) {
		pool.submit(() -> {
			LoadedImage loadedImage = imageMap.get(id);
			if (loadedImage == null) {
				return;
			}

			BufferedImage image = loadedImage.getImage();

			// Ensure that the crop region is within the bounds of the original image
			Rectangle crop = cropRegion.intersection(
					new Rectangle(0, 0, image.getWidth(), image.getHeight()));

			// Perform the actual cropping
			BufferedImage croppedImage = image.getSubimage(
					crop.x, crop.y, crop.width, crop.height);

			loadedImage.updateImage(croppedImage);

			view.imageWasUpdated(loadedImage.getId(), croppedImage, loadedImage.hasUnsavedChanges());
		});

	}

	@Override
	public void save() {
		pool.submit(() -> {
			view.showOverlay("Saving", "");
			for (LoadedImage loadedImage : imageMap.values()) {
				if (loadedImage.hasUnsavedChanges()) {
					String id = loadedImage.getId();
					File file = loadedImage.getFile();
					BufferedImage image = loadedImage.getImage();
					try {
						ImageIO.write(image, "jpg", file);
						LoadedImage newImage = new LoadedImage(loadedImage);
						imageMap.put(id, newImage);
						view.imageWasUpdated(id, image, newImage.hasUnsavedChanges());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			view.hideOverlay();
		});
	}

	@Override
	public void resize(int width, int height) {
		pool.submit(() -> {
			int totalCount = imageMap.size();
			int currentCount = 1;
			for (LoadedImage loadedImage : imageMap.values()) {
				view.showOverlay("Resizing images", currentCount + "/" + totalCount);
				currentCount++;
				File imageFile = loadedImage.getFile();
				BufferedImage image = loadedImage.getImage();

				// Get the parent directory of the image file
				File parentDirectory = imageFile.getParentFile();

				// Create the 'resized' directory
				File resizedDirectory = new File(parentDirectory, "resized");
				resizedDirectory.mkdirs(); // Make sure the directory is created

				BufferedImage scaledImage = Utils.resizeImage(image, width, height);

				// Get the file name
				String fileName = imageFile.getName();
				File resizedImage = new File(resizedDirectory, fileName);

				try {
					ImageIO.write(scaledImage, "jpg", resizedImage);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			view.hideOverlay();
		});

	}

	@Override
	public int[] getSizeInfo(String id) {
		LoadedImage loadedImage = imageMap.get(id);
		BufferedImage image = loadedImage.getImage();
		int fileSize = (int) (loadedImage.getFile().length() >> 10);
		if (loadedImage.hasUnsavedChanges()) {
			BufferedImage originalImage = loadedImage.getOriginalImage();
			int estimate = fileSize * image.getWidth() * image.getHeight()
					/ (originalImage.getWidth() * originalImage.getHeight());
			return new int[] { image.getWidth(), image.getHeight(), estimate };
		}
		return new int[] { image.getWidth(), image.getHeight(), fileSize };
	}

	private static List<Path> findImageFiles(Path directory) throws IOException {
		List<Path> imageFiles = new ArrayList<>();

		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (Files.isRegularFile(file) && isImageFile(file) && !isInResizedDirectory(file)) {
					imageFiles.add(file);
				}
				return FileVisitResult.CONTINUE;
			}
		});

		return imageFiles;
	}

	private static boolean isImageFile(Path file) {
		String fileName = file.getFileName().toString().toLowerCase();
		return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")
				|| fileName.endsWith(".gif");
	}

	private static boolean isInResizedDirectory(Path file) {
		return file.getParent() != null && file.getParent().endsWith("resized");
	}

}
