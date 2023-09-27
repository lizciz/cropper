package cropper.model;

import java.awt.Desktop;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

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
			List<File> failedToLoad = new ArrayList<>();
			for (File f : dir.listFiles()) {
				if (f.getName().toLowerCase().endsWith(".jpg")) {
					try {
						BufferedImage image = ImageIO.read(f);
						LoadedImage loadedImage = new LoadedImage(f, image);
						imageMap.put(loadedImage.getId(), loadedImage);
						view.imageWasLoaded(image, loadedImage.getFilename(), loadedImage.getId());
					} catch (IOException e) {
						failedToLoad.add(f);
					}
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
			view.imageWasUpdated(id, loadedImage.getImage());
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

			BufferedImage originalImage = loadedImage.getOriginalImage();

			// Ensure that the crop region is within the bounds of the original image
			Rectangle crop = cropRegion.intersection(
					new Rectangle(0, 0, originalImage.getWidth(), originalImage.getHeight()));

			// Perform the actual cropping
			BufferedImage croppedImage = originalImage.getSubimage(
					crop.x, crop.y, crop.width, crop.height);

			loadedImage.updateImage(croppedImage);

			view.imageWasUpdated(loadedImage.getId(), croppedImage);
		});

	}

	@Override
	public void save() {
		// TODO Auto-generated method stub

	}

}
