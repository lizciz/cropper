package cropper.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;

public class ProgressOverlay extends JPanel {

	private static final long serialVersionUID = 1L;

	private boolean isShowing;

	private JLabel lblProgressMessage;

	private JLabel lblExtraInfo;

	/**
	 * Create the panel.
	 */
	public ProgressOverlay() {
		isShowing = false;

		setBackground(new Color(0, 0, 0, 200));
		setOpaque(false);
		setLayout(new MigLayout("", "[grow][][grow]", "[grow][][grow]"));
		setFocusable(true);
		setFocusCycleRoot(true);
		requestFocus();

		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(300, 160));
		panel.setBorder(new TitledBorder(null, "", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		add(panel, "cell 1 1,grow");
		panel.setLayout(new MigLayout("", "[grow]", "[grow]"));

		lblProgressMessage = new JLabel("-");
		lblProgressMessage.setFont(new Font("Tahoma", Font.BOLD, 14));
		panel.add(lblProgressMessage, "flowy,cell 0 0,alignx center");
		lblProgressMessage.setHorizontalAlignment(SwingConstants.CENTER);

		lblExtraInfo = new JLabel("-");
		lblExtraInfo.setFont(new Font("Tahoma", Font.PLAIN, 14));
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

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setColor(new Color(0, 0, 0, 150));
		g.fillRect(0, 0, getWidth(), getHeight());
	}

	public void showOverlay(String message, String extraInfo) {
		lblProgressMessage.setText(message);
		lblExtraInfo.setText(extraInfo);
		setFocusCycleRoot(true);
		setVisible(true);
		requestFocus();
		isShowing = true;
	}

	public void hideOverlay() {
		setFocusCycleRoot(false);
		setVisible(false);
		isShowing = false;
	}

	public boolean isOverlayShowing() {
		return isShowing;
	}

}
