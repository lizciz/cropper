package cropper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class CropperProps {

	private static final String PROP_WIDTH = "WIDTH";
	private static final String PROP_HEIGHT = "HEIGHT";
	private static final String PROP_COPYRIGHT = "COPYRIGHT";
	private static final String PROP_LAST_PATH = "LAST_PATH";
	private static final String PROP_MAX_ZOOM_100 = "MAX_ZOOM_100";
	private static final String PROP_LANGUAGE = "LANGUAGE";

	private final Properties props;
	private final Path path;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public CropperProps() {
		props = new Properties();
		String userHome = System.getProperty("user.home");
		path = Path.of(userHome, ".cropperprops");
		try {
			props.load(Files.newInputStream(path));
		} catch (IOException e) {
		}
	}

	public String getWidth() {
		return props.getProperty(PROP_WIDTH, "");
	}

	public void setWidth(String width) {
		props.setProperty(PROP_WIDTH, width);
		save();
	}

	public String getHeight() {
		return props.getProperty(PROP_HEIGHT, "");
	}

	public void setHeight(String height) {
		props.setProperty(PROP_HEIGHT, height);
		save();
	}

	public String getCopyright() {
		return props.getProperty(PROP_COPYRIGHT, "");
	}

	public void setCopyright(String copyright) {
		props.setProperty(PROP_COPYRIGHT, copyright);
		save();
	}

	public String getLastPath() {
		return props.getProperty(PROP_LAST_PATH, "");
	}

	public void setLastPath(String lastPath) {
		props.setProperty(PROP_LAST_PATH, lastPath);
		save();
	}

	public boolean isMaxZoom100() {
		return Boolean.parseBoolean(props.getProperty(PROP_MAX_ZOOM_100, "false"));
	}

	public void setMaxZoom100(boolean maxZoom100) {
		props.setProperty(PROP_MAX_ZOOM_100, Boolean.toString(maxZoom100));
		save();
	}

	public String getLanguage() {
		return props.getProperty(PROP_LANGUAGE, "EN");
	}

	public void setLanguage(String lang) {
		props.setProperty(PROP_LANGUAGE, lang);
		save();
	}

	private void save() {
		try {
			props.store(Files.newOutputStream(path), "Cropper props, " + dateFormat.format(new Date()));
		} catch (IOException e) {
		}
	}

}
