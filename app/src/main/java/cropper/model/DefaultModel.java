package cropper.model;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import cropper.CropperProps;
import util.Utils;

public class DefaultModel implements ModelAPI {

	private ModelView view;

	private ArrayList<File> files;
	private ArrayList<BufferedImage> originalImages;
	private ArrayList<BufferedImage> images;
	private int selectedIndex;

	private final ExecutorService pool = Executors.newSingleThreadExecutor();

	private CropperProps props;

	public DefaultModel(CropperProps props) {
		this.files = new ArrayList<>();
		this.originalImages = new ArrayList<>();
		this.images = new ArrayList<>();
		this.selectedIndex = -2;
		this.props = props;
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
						originalImages.add(image);
						images.add(image);
						files.add(f);
						view.imageWasLoaded(image, Utils.rmFileExt(f.getName()), images.size() - 1);
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
	public void selectImage(final int index) {
		_selectHelper(true, index);
	}

	@Override
	public void selectPreviousImage() {
		_selectHelper(false, -1);
	}

	@Override
	public void selectNextImage() {
		_selectHelper(false, 1);
	}

	private void _selectHelper(final boolean set, final int value) {
		pool.submit(() -> {
			int index = set ? value : selectedIndex + value;
			int bounded = Math.max(0, Math.min(index, images.size() - 1));
			if (bounded != selectedIndex && !images.isEmpty()) {
				this.selectedIndex = bounded;
				view.displayImage(bounded, images.get(bounded));
			}
		});
	}

	@Override
	public void deleteSelectedImage() {
	}

	@Override
	public void undoSelectedImageChanges() {
	}

	@Override
	public void redoSelectedImageChanges() {
	}

	@Override
	public void performCrop(final Rectangle cropRegion) {
		pool.submit(() -> {
			BufferedImage originalImage = images.get(selectedIndex);

			// Ensure that the crop region is within the bounds of the original image
			Rectangle crop = cropRegion.intersection(
					new Rectangle(0, 0, originalImage.getWidth(), originalImage.getHeight()));

			// Perform the actual cropping
			BufferedImage croppedImage = originalImage.getSubimage(
					crop.x, crop.y, crop.width, crop.height);

			images.set(selectedIndex, croppedImage);

			view.displayImage(selectedIndex, croppedImage);
			view.refreshThumbnail(selectedIndex, croppedImage);
		});

	}

	@Override
	public void save() {
		// TODO Auto-generated method stub

	}

}
