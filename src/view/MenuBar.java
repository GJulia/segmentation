package view;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.*;

public class MenuBar extends JMenuBar {

	private static final long serialVersionUID = 1L;

	private Controller ctrl;

	public MenuBar(Controller controller) {
		super();
		ctrl = controller;
		add(getFileMenu());
		add(getEditMenu());
	}

	public JMenu getFileMenu() {
		JMenu menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_A);
		menu.add(getMenuItem("Open", "open", KeyEvent.VK_A, 'O'));
		menu.add(getMenuItem("Save", "saveAs", KeyEvent.VK_G, 'S'));
		menu.add(getMenuItem("Quit", "quit", KeyEvent.VK_S, 'Q'));
		return menu;
	}

	public JMenu getEditMenu() {
		JMenu menu = new JMenu("Segmentation");
		menu.setMnemonic(KeyEvent.VK_E);
		menu.add(getMenuItem("Undo", "undoAll", KeyEvent.VK_D, 'Z'));
		menu.add(getMenuItem("Select method", "applySegmentation",
				KeyEvent.VK_S, 'G'));
		return menu;
	}
	public JMenuItem getMenuItem(String label, String callback, int mnemonic,
			char accel) {
		JMenuItem item = new JMenuItem(label, mnemonic);
		item.setAccelerator(KeyStroke
				.getKeyStroke(accel, ActionEvent.CTRL_MASK));
		item.addActionListener(new ActionListener(ctrl, callback));
		return item;
	}

	public JMenuItem getMenuItem(String label, String callback, int mnemonic,
			int accel) {
		JMenuItem item = new JMenuItem(label, mnemonic);
		item.setAccelerator(KeyStroke.getKeyStroke(accel, 0));
		item.addActionListener(new ActionListener(ctrl, callback));
		return item;
	}
}
