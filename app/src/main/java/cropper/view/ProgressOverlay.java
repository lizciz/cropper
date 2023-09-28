package cropper.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;

public class ProgressOverlay extends JPanel {

	private static final long serialVersionUID = 1L;

	/**
	 * Create the panel.
	 */
	public ProgressOverlay() {
		setBackground(new Color(0, 0, 0, 200));
		setOpaque(false);
		setLayout(new MigLayout("", "[grow][][grow]", "[grow][][grow]"));
		setFocusable(true);
		setFocusCycleRoot(true);
		requestFocus();

		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(200, 100));
		panel.setBorder(new TitledBorder(null, "", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		add(panel, "cell 1 1,grow");
		panel.setLayout(new MigLayout("", "[grow]", "[grow]"));

		JLabel lvlProgressMessage = new JLabel("Resizing images");
		panel.add(lvlProgressMessage, "flowy,cell 0 0,alignx center");
		lvlProgressMessage.setHorizontalAlignment(SwingConstants.CENTER);

		JLabel lblExtraInfo = new JLabel("13/88");
		panel.add(lblExtraInfo, "cell 0 0,alignx center");
		lblExtraInfo.setHorizontalAlignment(SwingConstants.CENTER);

		addMouseListener(new MouseAdapter() {
		});
		addMouseMotionListener(new MouseMotionAdapter() {
		});
	}

	@Override
	public boolean isFocusTraversalPolicySet() {
		return true;
	}

	@Override
	public FocusTraversalPolicy getFocusTraversalPolicy() {
		return new FocusTraversalPolicy() {
			@Override
			public Component getComponentAfter(Container aContainer, Component aComponent) {
				return ProgressOverlay.this;
			}

			@Override
			public Component getComponentBefore(Container aContainer, Component aComponent) {
				return ProgressOverlay.this;
			}

			@Override
			public Component getFirstComponent(Container aContainer) {
				return ProgressOverlay.this;
			}

			@Override
			public Component getLastComponent(Container aContainer) {
				return ProgressOverlay.this;
			}

			@Override
			public Component getDefaultComponent(Container aContainer) {
				return ProgressOverlay.this;
			}
		};
	}

	public void showOverlay(String message, String extraInfo) {
		setFocusCycleRoot(true);
		setVisible(true);
		requestFocus();
	}

	public void hideOverlay() {
		setFocusCycleRoot(false);
		setVisible(false);
	}

}
