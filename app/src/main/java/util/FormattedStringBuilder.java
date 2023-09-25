package util;

public class FormattedStringBuilder {

	private final StringBuilder sb;

	public FormattedStringBuilder() {
		this.sb = new StringBuilder();
	}

	public FormattedStringBuilder a(String str) {
		sb.append(str);
		return this;
	}

	public FormattedStringBuilder f(String str, Object... args) {
		sb.append(String.format(str, args));
		return this;
	}

	@Override
	public String toString() {
		return sb.toString();
	}

}
