package cropper.model;

import java.awt.Desktop;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.TiffDirectoryType;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoAscii;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import util.Lang;
import util.Utils;

public class DefaultModel implements ModelAPI {

	private ModelView view;
	private Lang lang;

	private Map<String, LoadedImage> imageMap;

	private final ExecutorService pool = Executors.newSingleThreadExecutor();

	public DefaultModel(Lang lang) {
		this.imageMap = new LinkedHashMap<>();
		this.lang = lang;
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
				e.printStackTrace();
				return;
			}

			List<File> failedToLoad = new ArrayList<>();
			ImageReader reader = ImageIO.getImageReadersByFormatName("jpg").next();
			for (Path path : imageFiles) {
				try {
					File f = path.toFile();
					reader.setInput(ImageIO.createImageInputStream(f));
					// BufferedImage image = Imaging.getBufferedImage(f);
					BufferedImage image = reader.read(0);
					if (image == null) {
						failedToLoad.add(path.toFile());
						continue;
					}
					LoadedImage loadedImage = new LoadedImage(f, image);
					imageMap.put(loadedImage.getId(), loadedImage);
					view.imageWasLoaded(loadedImage.getId(), loadedImage.getFilename(), image);
				} catch (Throwable e) {
					e.printStackTrace();
					failedToLoad.add(path.toFile());
				}
			}
			reader.dispose();

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
	public void save(String copyrightText) {
		pool.submit(() -> {
			view.showOverlay(lang.get(Lang.OVERLAY_SAVING), lang.get(Lang.OVERLAY_SAVING_EXTRA));
			ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			for (LoadedImage loadedImage : imageMap.values()) {
				// if (loadedImage.hasUnsavedChanges()) {
				String id = loadedImage.getId();
				File file = loadedImage.getFile();
				BufferedImage image = loadedImage.getImage();

				try {
					baos.reset();
					writer.setOutput(ImageIO.createImageOutputStream(baos));
					writer.write(null, new IIOImage(image, null, null), null);
					setCopyrightAndWriteImage(file, new ByteArrayInputStream(baos.toByteArray()), file, copyrightText);

					LoadedImage newImage = new LoadedImage(loadedImage);
					imageMap.put(id, newImage);
					view.imageWasUpdated(id, image, newImage.hasUnsavedChanges());
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
			// }
			writer.dispose();
			view.hideOverlay();
		});
	}

	// private static void attr(Node node, String indent) {
	// if (node instanceof IIOMetadataNode) {
	// IIOMetadataNode iioNode = (IIOMetadataNode) node;
	// Object userObject = iioNode.getUserObject();
	// System.out.println(indent + node.getNodeName() + " = " + (userObject != null
	// ? userObject.getClass() : null));
	// }
	// for (int i = 0; i < node.getAttributes().getLength(); i++) {
	// Node attr = node.getAttributes().item(i);
	// System.out.println(indent + " " + attr.getNodeName() + " = " +
	// attr.getNodeValue());
	// }
	// }

	// private static void rec(Node node, String indent) {
	// System.out.println(indent + node.getNodeName());
	// attr(node, indent);
	// if (node.hasChildNodes()) {
	// for (int i = 0; i < node.getChildNodes().getLength(); i++) {
	// rec((IIOMetadataNode) node.getChildNodes().item(i), indent + " ");
	// }
	// }
	// }

	// public static void updateCopyright(IIOMetadata metadata, String
	// copyrightText) throws IIOInvalidTreeException {

	// System.out.println(metadata.getNativeMetadataFormatName());
	// System.out.println(metadata.getAsTree(metadata.getNativeMetadataFormatName()));

	// Arrays.stream(metadata.getMetadataFormatNames()).forEach(f ->
	// rec(metadata.getAsTree(f), ""));

	// String exifNamespace = metadata.getNativeMetadataFormatName();

	// // Create a new IIOMetadataNode for the EXIF tag
	// IIOMetadataNode root = new
	// IIOMetadataNode(metadata.getNativeMetadataFormatName());
	// System.out.println();
	// rec(root, "");
	// System.out.println();

	// // Create a new IIOMetadataNode for the APP1 marker segment
	// // IIOMetadataNode root = new IIOMetadataNode(exifNamespace, "app1");

	// // Create nodes for JPEGvariety and markerSequence (required)
	// IIOMetadataNode variety = new IIOMetadataNode("JPEGvariety");
	// IIOMetadataNode sequence = new IIOMetadataNode("markerSequence");

	// // Create a IIOMetadataNode for the Copyright tag
	// IIOMetadataNode copyrightNode = new IIOMetadataNode("Copyright");
	// copyrightNode.setUserObject(copyrightText);

	// // Add the Copyright node to the APP1 node
	// root.appendChild(variety);
	// variety.appendChild(sequence);
	// sequence.appendChild(copyrightNode);

	// // Create a IIOMetadataNode for the Copyright tag
	// // IIOMetadataNode copyrightNode = new IIOMetadataNode("Copyright");
	// copyrightNode.setUserObject(copyrightText);

	// // Add the Copyright node to the EXIF node
	// root.appendChild(copyrightNode);

	// // Set the EXIF node as the root node in the metadata
	// metadata.mergeTree(exifNamespace, root);
	// }

	public void setCopyrightAndWriteImage(
			final File jpegImageFile, final InputStream is, final File outputJpegImageFile, String copyrightText)
			throws IOException, ImagingException, ImagingException {

		TagInfoAscii copyrightTag = new TagInfoAscii("Copyright", 0x8298, -1,
				TiffDirectoryType.EXIF_DIRECTORY_IFD0);

		TiffOutputSet outputSet = getTiffOutputSet(jpegImageFile);

		// Remove old value, if present, and replace it with our new value.
		final TiffOutputDirectory rootDirectory = outputSet.getOrCreateRootDirectory();
		if (rootDirectory.findField(copyrightTag) != null)
			rootDirectory.removeField(copyrightTag);
		rootDirectory.add(copyrightTag, copyrightText);

		try (OutputStream os = Files.newOutputStream(outputJpegImageFile.toPath())) {
			new ExifRewriter().updateExifMetadataLossless(is, os, outputSet);
		} catch (Exception e) {
			throw e;
		}

	}

	private TiffOutputSet getTiffOutputSet(final File jpegImageFile)
			throws ImageWriteException, ImageReadException, IOException {
		TiffOutputSet outputSet = null;

		// note that metadata might be null if no metadata is found.
		final ImageMetadata metadata = Imaging.getMetadata(jpegImageFile);
		final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
		if (jpegMetadata != null) {
			final TiffImageMetadata exif = jpegMetadata.getExif();

			if (exif != null) {
				outputSet = exif.getOutputSet();
			}

		}

		// if file does not contain any exif metadata, we create an empty
		// set of exif metadata. Otherwise, we keep all of the other
		// existing tags.
		if (outputSet == null) {
			outputSet = new TiffOutputSet();
		}

		return outputSet;
	}

	@Override
	public void resize(int width, int height, String copyrightText) {
		pool.submit(() -> {
			int totalCount = imageMap.size();
			int currentCount = 1;
			ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			for (LoadedImage loadedImage : imageMap.values()) {
				view.showOverlay(lang.get(Lang.OVERLAY_RESIZING),
						lang.get(Lang.OVERLAY_RESIZING_EXTRA, currentCount, totalCount));
				currentCount++;
				File imageFile = loadedImage.getFile();
				BufferedImage image = loadedImage.getImage();

				File parentDirectory = imageFile.getParentFile();
				File resizedDirectory = new File(parentDirectory, "resized");
				resizedDirectory.mkdirs();
				String fileName = imageFile.getName();
				File resizedImage = new File(resizedDirectory, fileName);

				BufferedImage scaledImage = Utils.resizeImage(image, width, height);

				try (OutputStream os = Files.newOutputStream(resizedImage.toPath())) {
					baos.reset();
					writer.setOutput(ImageIO.createImageOutputStream(baos));
					writer.write(null, new IIOImage(scaledImage, null, null), null);
					setCopyrightAndWriteImage(imageFile, new ByteArrayInputStream(baos.toByteArray()), resizedImage,
							copyrightText);
					writer.reset();
				} catch (Throwable t) {
					t.printStackTrace();
				}

				// try {
				// ImageIO.write(scaledImage, "jpg", resizedImage);
				// InputStream is = Files.newInputStream(resizedImage.toPath());
				// setCopyrightAndWriteImage(imageFile, is, resizedImage2, copyrightText);
				// } catch (Throwable e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }

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
