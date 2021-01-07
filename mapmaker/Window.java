package mapmaker;

import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FileUtils;

public class Window extends JFrame {

	private static final long serialVersionUID = -585839154376527986L;
	String saveLocation;
	Display display;
	private String fileName = "Untitiled";
	private Status status = Status.UNSAVED;

	JRadioButton horizontal;
	JRadioButton vertical;
	JRadioButton rotational;

	JRadioButton passability;
	JRadioButton redEC;
	JRadioButton blueEC;
	JRadioButton neutralEC;
	JRadioButton deleteEC;

	JTextField passabilityField;
	JTextField widthField;
	JTextField heightField;

	public Window(Display display) {
		super();

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			SwingUtilities.updateComponentTreeUI(this);
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.display = display;

		// ImageIcon img = new ImageIcon("resources/images/WarcodeIcon.png");
		// setIconImage(img.getImage());

		makeMenuBar();

		setTitle("*Untitiled* - Battlecode 2021 Map Maker");
		setCurrentTool(Tool.PASSABILITY);
		setCurrentSymmetry(Symmetry.HORIZONTAL);

		setVisible(true);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		Window window = this;
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				if (status == Status.UNSAVED) {
					String ObjButtons[] = { "Save", "No", "Cancel" };
					int PromptResult = JOptionPane.showOptionDialog(window, "Save before exiting?", "Exit",
							JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, ObjButtons, ObjButtons[1]);
					if (PromptResult == 2) {
						return;
					}
					if (PromptResult == 0) {
						if (saveLocation == null) {
							if (!chooseSaveLocation()) {
								return;
							}
						}
						saveFile();
					}
				}
				System.exit(0);
			}
		});
	
		addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
				window.requestFocusInWindow();			
			}

			@Override
			public void mouseReleased(MouseEvent e) {				
			}

			@Override
			public void mouseEntered(MouseEvent e) {				
			}

			@Override
			public void mouseExited(MouseEvent e) {				
			}
			
		});
		
		display.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
				window.requestFocusInWindow();			
			}

			@Override
			public void mouseReleased(MouseEvent e) {				
			}

			@Override
			public void mouseEntered(MouseEvent e) {				
			}

			@Override
			public void mouseExited(MouseEvent e) {				
			}
			
		});
	}

	/**
	 * Code put into a function to make more readable. Called on initializing.
	 */
	private void makeMenuBar() {
		JMenuBar menubar = new JMenuBar();

		{
			// make file drop down
			JMenu fileMenu = new JMenu("File");
			fileMenu.setMnemonic(KeyEvent.VK_F);
			
			JMenuItem save = new JMenuItem();
			save.setText("Save");
			save.setMnemonic(KeyEvent.VK_S);
			save.setAccelerator(KeyStroke.getKeyStroke('S', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			fileMenu.add(save);
			save.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (saveLocation == null) {
						if (!chooseSaveLocation()) { // User pressed cancel, so don't save it.
							return;
						}
					}
					saveFile();
				}
			});

			JMenuItem saveAs = new JMenuItem("Save As...");
			saveAs.setMnemonic(KeyEvent.VK_A);
			saveAs.setDisplayedMnemonicIndex(5);
			saveAs.setAccelerator(KeyStroke.getKeyStroke('A', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			fileMenu.add(saveAs);
			saveAs.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (!chooseSaveLocation()) { // User pressed cancel, so don't save it.
						return;
					}
					saveFile();
				}
			});
			
			menubar.add(fileMenu);
		}

		{// add symmetry drop down
			JMenu symmetryMenu = new JMenu("Symmetry");
			symmetryMenu.setMnemonic(KeyEvent.VK_M);
			ButtonGroup symmetryGroup = new ButtonGroup();

			horizontal = new JRadioButton();
			horizontal.setText("Horizontal");
			horizontal.setMnemonic(KeyEvent.VK_H);
			symmetryMenu.add(horizontal);
			symmetryGroup.add(horizontal);
			horizontal.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					display.setSymmetry(Symmetry.HORIZONTAL);
				}
			});

			symmetryMenu.addSeparator();

			vertical = new JRadioButton();
			vertical.setText("Vertical");
			vertical.setMnemonic(KeyEvent.VK_V);
			symmetryMenu.add(vertical);
			symmetryGroup.add(vertical);
			vertical.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					display.setSymmetry(Symmetry.VERTICAL);
				}
			});

			symmetryMenu.addSeparator();

			rotational = new JRadioButton();
			rotational.setText("Rotational");
			rotational.setMnemonic(KeyEvent.VK_L);
			symmetryMenu.add(rotational);
			symmetryGroup.add(rotational);
			rotational.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					display.setSymmetry(Symmetry.ROTATIONAL);
				}
			});

			menubar.add(symmetryMenu);
		}

		{// add tool drop down
			JMenu toolMenu = new JMenu("Tool");
			toolMenu.setMnemonic(KeyEvent.VK_T);
			ButtonGroup toolGroup = new ButtonGroup();

			passability = new JRadioButton();
			passability.setText("Passability");
			passability.setMnemonic(KeyEvent.VK_P);
			toolMenu.add(passability);
			toolGroup.add(passability);
			passability.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					display.setTool(Tool.PASSABILITY);
				}
			});

			toolMenu.addSeparator();

			redEC = new JRadioButton();
			redEC.setText("Red EC");
			redEC.setMnemonic(KeyEvent.VK_R);
			toolMenu.add(redEC);
			toolGroup.add(redEC);
			redEC.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					display.setTool(Tool.RED_EC);
				}
			});

			blueEC = new JRadioButton();
			blueEC.setText("Blue EC");
			blueEC.setMnemonic(KeyEvent.VK_B);
			toolMenu.add(blueEC);
			toolGroup.add(blueEC);
			blueEC.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					display.setTool(Tool.BLUE_EC);
				}
			});

			neutralEC = new JRadioButton();
			neutralEC.setText("Neutral EC");
			neutralEC.setMnemonic(KeyEvent.VK_E);
			toolMenu.add(neutralEC);
			toolGroup.add(neutralEC);
			neutralEC.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					display.setTool(Tool.NEUTRAL_EC);
				}
			});

			deleteEC = new JRadioButton();
			deleteEC.setText("Delete EC");
			deleteEC.setMnemonic(KeyEvent.VK_D);
			toolMenu.add(deleteEC);
			toolGroup.add(deleteEC);
			deleteEC.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					display.setTool(Tool.DELETE_EC);
				}
			});

			menubar.add(toolMenu);
		}

		// Make passability textbox
		{
			JLabel passabilityLabel = new JLabel("Passability: ");
			passabilityLabel.setFont(new Font(passabilityLabel.getFont().getName(), Font.PLAIN, 14));
			passabilityField = new JTextField("0.500");
			passabilityField.setMaximumSize(passabilityField.getPreferredSize());
			passabilityField.addFocusListener(new FocusListener() {
				@Override
				public void focusGained(FocusEvent e) {
				}

				@Override
				public void focusLost(FocusEvent e) {
					updatePassability();
					
				}
				
			});

			menubar.add(new JLabel(" | "));
			menubar.add(passabilityLabel);
			menubar.add(passabilityField);
		}

		// Make width + height textboxes
		{
			JLabel widthLabel = new JLabel("Width: ");
			widthLabel.setFont(new Font(widthLabel.getFont().getName(), Font.PLAIN, 14));
			widthField = new JTextField("50");
			widthField.setMaximumSize(passabilityField.getPreferredSize());
			widthField.addFocusListener(new FocusListener() {
				@Override
				public void focusGained(FocusEvent e) {
				}

				@Override
				public void focusLost(FocusEvent e) {
					updateWidth();
					
				}
				
			});

			menubar.add(new JLabel(" | "));
			menubar.add(widthLabel);
			menubar.add(widthField);
			
			
			JLabel heightLabel = new JLabel("Height: ");
			heightLabel.setFont(new Font(heightLabel.getFont().getName(), Font.PLAIN, 14));
			heightField = new JTextField("50");
			heightField.setMaximumSize(passabilityField.getPreferredSize());
			heightField.addFocusListener(new FocusListener() {
				@Override
				public void focusGained(FocusEvent e) {
				}

				@Override
				public void focusLost(FocusEvent e) {
					updateHeight();
					
				}
				
			});

			menubar.add(new JLabel(" | "));
			menubar.add(heightLabel);
			menubar.add(heightField);
		}
		
		{ // clear button
			JButton clearButton = new JButton("Clear");
			clearButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					display.reset();					
				}
			});
			menubar.add(clearButton);			
		}

		setJMenuBar(menubar);
	}

	public void setCurrentTool(Tool tool) {
		switch (tool) {
		case PASSABILITY:
			passability.setSelected(true);
			break;
		case RED_EC:
			redEC.setSelected(true);
			break;
		case BLUE_EC:
			blueEC.setSelected(true);
			break;
		case NEUTRAL_EC:
			neutralEC.setSelected(true);
			break;
		case DELETE_EC:
			deleteEC.setSelected(true);
			break;
		default:
			break;
		}
	}

	public void setCurrentSymmetry(Symmetry symmetry) {
		switch (symmetry) {
		case HORIZONTAL:
			horizontal.setSelected(true);
			break;
		case VERTICAL:
			vertical.setSelected(true);
			break;
		case ROTATIONAL:
			rotational.setSelected(true);
			break;
		default:
			break;
		}
	}

	private void updatePassability() {

		double newPassability;
		try {
			newPassability = Double.parseDouble(passabilityField.getText());
		} catch (NumberFormatException e) {
			passabilityField.setText("" + display.getCurrentPassability());
			return;
		}
		display.setCurrentPassability(newPassability);
	}
	
	private void updateWidth() {

		int newWidth;
		try {
			newWidth = Integer.parseInt(widthField.getText());
		} catch (NumberFormatException e) {
			widthField.setText("" + display.getMapWidth());
			return;
		}
		display.setWidth(newWidth);
	}
	
	private void updateHeight() {

		int newHeight;
		try {
			newHeight = Integer.parseInt(heightField.getText());
		} catch (NumberFormatException e) {
			heightField.setText("" + display.getMapHeight());
			return;
		}
		display.setHeight(newHeight);
	}

	public String getFileName() {
		return fileName;
	}

	/**
	 * 
	 * @return boolean. true means it chose successfully, false means the user
	 *         cancelled.
	 */
	private boolean chooseSaveLocation() {
		JFileChooser chooser = new JFileChooser("maps");
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Battlecode 2021 Map", "map21");
		chooser.setFileFilter(filter);
		int returnVal = chooser.showSaveDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			fileName = chooser.getSelectedFile().getName();
			if (fileName.endsWith(".map21")) {
				fileName = fileName.substring(0, fileName.length() - 6);
			}
			String file = chooser.getSelectedFile().getAbsolutePath();
			if (!file.endsWith(".map21")) {
				file = file + ".map21";
			}
			saveLocation = file;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Saves the map
	 */
	private void saveFile() {
		try {
			FileUtils.writeByteArrayToFile(new File(saveLocation), display.generateMap());
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} finally {
			setStatus(Status.SAVED);
		}
	}

	/**
	 * Opens a map
	 */
	private void openFile() {
		JFileChooser chooser = new JFileChooser("resources/maps");
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Battlecode 2021 Map", "map21");
		chooser.setFileFilter(filter);
		int returnVal = chooser.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			fileName = chooser.getSelectedFile().getName();
			if (fileName.endsWith(".map21")) {
				fileName = fileName.substring(0, fileName.length() - 6);
			}
			String file = chooser.getSelectedFile().getAbsolutePath();
			if (!file.endsWith(".map21")) {
				file = file + ".map21";
			}
			saveLocation = file;
		} else {
			return;
		}

		setStatus(Status.SAVED);
		display.openMap(saveLocation);
	}

	public void setStatus(Status status) {
		this.status = status;
		if (status == Status.UNSAVED) {
			setTitle("*" + fileName + "* - Battlecode 2021 Map Maker");
		} else if (status == Status.SAVED) {
			setTitle(fileName + " - Battlecode 2021 Map Maker");
		}
	}

}
