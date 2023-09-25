package util;

public class Utils {

	public static String rmFileExt(String fileName) {
		int lastDotIndex = fileName.lastIndexOf('.');
		if (lastDotIndex != -1) {
			return fileName.substring(0, lastDotIndex);
		}
		return fileName;
	}

}
