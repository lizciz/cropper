package util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cropper.CropperProps;

public class Lang {

	public static final String OVERLAY_SAVING = "OVERLAY_SAVING";
	public static final String OVERLAY_SAVING_EXTRA = "OVERLAY_SAVING_EXTRA";
	public static final String OVERLAY_RESIZING = "OVERLAY_RESIZING";
	public static final String OVERLAY_RESIZING_EXTRA = "OVERLAY_RESIZING_EXTRA";

	public static final String LOAD_FAILED_TITLE = "LOAD_FAILED_TITLE";
	public static final String LOAD_FAILED_MESSAGE = "LOAD_FAILED_MESSAGE";

	public static final String SELECT_IMG_DIR_BUTTON_TEXT = "SELECT_IMG_DIR_BUTTON_TEXT";
	public static final String LIMIT_ZOOM_LABEL = "LIMIT_ZOOM_LABEL";

	public static final String PREFERRED_SIZE_TEXT = "PREFERRED_SIZE_TEXT";
	public static final String INVALID_SIZE_MESSAGE = "INVALID_SIZE_MESSAGE";
	public static final String INVALID_SIZE_TITLE = "INVALID_SIZE_TITLE";

	public static final String COPYRIGHT_LABEL = "COPYRIGHT_LABEL";

	public static final String SAVE_ALL_BUTTON_TEXT = "SAVE_ALL_BUTTON_TEXT";
	public static final String SAVE_RESIZE_BUTTON_TEXT = "SAVE_RESIZE_BUTTON_TEXT";

	public static final String CONFIRM_DELETE_MESSAGE = "CONFIRM_DELETE_MESSAGE";
	public static final String CONFIRM_DELETE_TITLW = "CONFIRM_DELETE_TITLW";

	private final Map<String, String> langMap;

	public Lang(CropperProps props) {
		String lang = props.getLanguage();
		String defaultLang = "EN";

		Map<String, String> map = readLanguageFile(lang);
		if (map == null)
			map = readLanguageFile(defaultLang);
		langMap = map != null ? map : Collections.emptyMap();
	}

	public String get(String key, Object... args) {
		String pattern = langMap.get(key);
		if (pattern == null) {
			return "";
		}
		if (args.length == 0)
			return pattern;
		return String.format(pattern, args);
	}

	private Map<String, String> readLanguageFile(String lang) {

		try {
			String pathValue = String.format("/lang/%s.txt", lang);
			Path path = Paths.get(getClass().getResource(pathValue).toURI());
			
			if (!Files.exists(path)) {
				return null;
			}
			List<String> lines = Files.readAllLines(path);
			
			Map<String, String> langMap = new HashMap<>();
			for (String line : lines) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String[] split = line.split("=", 2);
				if (split.length == 2)
					langMap.put(split[0].trim(), split[1].trim());
			}
			return langMap;

		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}

		return null;
	}

}
