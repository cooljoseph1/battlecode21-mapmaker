package mapmaker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Arrays;
import java.util.LinkedList;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.google.flatbuffers.FlatBufferBuilder;

import battlecode.common.RobotType;
import battlecode.schema.SpawnedBodyTable;
import battlecode.schema.Vec;
import battlecode.schema.VecTable;
import battlecode.schema.BodyType;
import battlecode.schema.GameMap;

public class Display extends JPanel implements MouseMotionListener, MouseListener, KeyListener {

	private static final long serialVersionUID = 5268301898468656990L;

	private Window window;

	private int displayWidth;
	private int displayHeight;
	private int mapWidth;
	private int mapHeight;
	private double scaleSize;
	private int top;
	private int left;

	private Symmetry symmetry = Symmetry.HORIZONTAL;

	private int[] previousTileChanged = new int[] { -1, -1 };
	private double[][] passabilityMap;
	private Tool currentTool = Tool.PASSABILITY;
	private double currentPassability = 0.5;

	private LinkedList<EnlightenmentCenter> enlightenmentCenters = new LinkedList<EnlightenmentCenter>();

	private int[] startingPosition = null;
	private int[] cursorPosition = null;

	public Display(int width, int height, int mapWidth, int mapHeight) {
		super();

		this.mapWidth = mapWidth;
		this.mapHeight = mapHeight;

		passabilityMap = new double[mapHeight][mapWidth];
		// initialize passabilityMap to all 1.0, the highest possible passability
		for (int y = 0; y < mapHeight; y++) {
			for (int x = 0; x < mapWidth; x++) {
				passabilityMap[y][x] = 1.0;
			}
		}

		window = new Window(this);

		window.setPreferredSize(new Dimension(width, height));
		window.setLayout(new BorderLayout());
		window.add(this, BorderLayout.CENTER);
		window.pack();

		addMouseMotionListener(this);
		addMouseListener(this);
		addKeyListener(this);
		grabFocus();

	}

	public void reset() {
		passabilityMap = new double[mapHeight][mapWidth];
		// initialize passabilityMap to all 1.0, the highest possible passability
		for (int y = 0; y < mapHeight; y++) {
			for (int x = 0; x < mapWidth; x++) {
				passabilityMap[y][x] = 1.0;
			}
		}
		previousTileChanged = null;
		enlightenmentCenters = new LinkedList<EnlightenmentCenter>();
		repaint();
	}

	public void setWidth(int width) {
		mapWidth = width;
		reset();
	}
	
	public void setHeight(int height) {
		mapHeight = height;
		reset();
	}
	
	public void setCurrentPassability(double passability) {
		currentPassability = passability;
		previousTileChanged = null;
	}

	public double getCurrentPassability() {
		return currentPassability;
	}
	
	public int getMapWidth() {
		return mapWidth;
	}
	
	public double getMapHeight() {
		return mapHeight;
	}

	@Override
	protected void paintComponent(Graphics g) {
		displayWidth = getWidth();
		displayHeight = getHeight();
		setDefaultScale();

		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0, 0, getWidth(), getHeight());

		for (int y = 0; y < mapHeight; y++) {
			for (int x = 0; x < mapWidth; x++) {
				int brightness = (int) (255 * passabilityMap[y][x]);
				g2d.setColor(new Color(brightness, brightness, brightness));
				g2d.fillRect(calculateDrawX(x), calculateDrawY(y), (int) scaleSize + 1, (int) scaleSize + 1);
			}
		}

		for (EnlightenmentCenter ec : enlightenmentCenters) {
			int brightness = (int) (127 + 128 * passabilityMap[ec.y][ec.x]);
			switch (ec.team) {
			case RED:
				g2d.setColor(new Color(brightness, 0, 0));
				break;
			case BLUE:
				g2d.setColor(new Color(0, 0, brightness));
				break;
			case NEUTRAL:
				g2d.setColor(new Color(brightness, 0, brightness));
				break;
			default:
				break;
			}

			g2d.fillRect(calculateDrawX(ec.x), calculateDrawY(ec.y), (int) scaleSize + 1, (int) scaleSize + 1);
		}

		g2d.setColor(Color.BLACK);
		for (int y = 0; y < mapHeight + 1; y++) {
			g2d.drawLine(left, calculateDrawY(y), calculateDrawX(mapWidth), calculateDrawY(y));
		}
		for (int x = 0; x < mapWidth + 1; x++) {
			g2d.drawLine(calculateDrawX(x), top, calculateDrawX(x), calculateDrawY(mapHeight));
		}

		if (startingPosition != null) {
			g2d.setColor(Color.RED);
			g2d.drawRect(Math.min(startingPosition[0], cursorPosition[0]),
					Math.min(startingPosition[1], cursorPosition[1]), Math.abs(cursorPosition[0] - startingPosition[0]),
					Math.abs(cursorPosition[1] - startingPosition[1]));
		}
	}

	private void setDefaultScale() {
		scaleSize = Math.min(((double) displayWidth) / mapWidth, ((double) displayHeight) / mapHeight) - 1;
		top = (int) ((displayHeight - (scaleSize * mapHeight)) / 2);
		left = (int) ((displayWidth - (scaleSize * mapWidth)) / 2);
	}

	private int[] calculateDrawPosition(int x, int y) {
		return new int[] { left + (int) (x * scaleSize), top + (int) (y * scaleSize) };
	}

	private int calculateDrawX(int x) {
		return left + (int) (x * scaleSize);
	}

	private int calculateDrawY(int y) {
		return top + (int) (y * scaleSize);
	}

	private int[] calculateGridPosition(int x, int y) {
		return new int[] { (int) Math.floor((x - left) / scaleSize), (int) Math.floor((y - top) / scaleSize) };
	}

	private void useMyTool(int mouseX, int mouseY) {
		int[] gridPosition = calculateGridPosition(mouseX, mouseY);
		if (Arrays.equals(gridPosition, previousTileChanged)) {
			return;
		}
		if (gridPosition[0] < 0 || gridPosition[0] >= mapWidth || gridPosition[1] < 0 || gridPosition[1] >= mapHeight) {
			return;
		}

		window.setStatus(Status.UNSAVED);

		int x = gridPosition[0];
		int y = gridPosition[1];

		int sx; // symmetry x
		int sy; // symmetry y
		switch (symmetry) {
		case HORIZONTAL:
			sx = x;
			sy = mapHeight - y - 1;
			break;
		case VERTICAL:
			sx = mapWidth - x - 1;
			sy = y;
			break;
		case ROTATIONAL:
		default:
			sx = mapWidth - x - 1;
			sy = mapHeight - y - 1;
			break;
		}

		switch (currentTool) {
		case PASSABILITY:
			setPassability(x, y, currentPassability);
			setPassability(sx, sy, currentPassability);
			break;
		case RED_EC:
			deleteEC(x, y);
			deleteEC(sx, sy);
			makeEC(x, y, Team.RED);
			makeEC(sx, sy, Team.BLUE);
			break;
		case BLUE_EC:
			deleteEC(x, y);
			deleteEC(sx, sy);
			makeEC(x, y, Team.BLUE);
			makeEC(sx, sy, Team.RED);
			break;
		case NEUTRAL_EC:
			deleteEC(x, y);
			deleteEC(sx, sy);
			makeEC(x, y, Team.NEUTRAL);
			makeEC(sx, sy, Team.NEUTRAL);
			break;
		case DELETE_EC:
			deleteEC(x, y);
			deleteEC(sx, sy);
			break;
		default:
			break;
		}

		previousTileChanged = gridPosition;
	}

	private void setPassability(int x, int y, double passability) {
		passabilityMap[y][x] = passability;
		repaint();

	}

	private void setPassabilityRange(int startingX, int startingY, int endX, int endY, double passability) {
		window.setStatus(Status.UNSAVED);
		int[] startPosition = calculateGridPosition(startingX, startingY);
		int[] endPosition = calculateGridPosition(endX, endY);
		for (int x = startPosition[0]; x <= endPosition[0]; x++) {
			for (int y = startPosition[1]; y <= endPosition[1]; y++) {
				if (x < 0 || y < 0 || x >= mapWidth || y >= mapHeight) {
					continue;
				}
				passabilityMap[y][x] = passability;
				
				int sx; // symmetry x
				int sy; // symmetry y
				switch (symmetry) {
				case HORIZONTAL:
					sx = x;
					sy = mapHeight - y - 1;
					break;
				case VERTICAL:
					sx = mapWidth - x - 1;
					sy = y;
					break;
				case ROTATIONAL:
				default:
					sx = mapWidth - x - 1;
					sy = mapHeight - y - 1;
					break;
				}
				
				passabilityMap[sy][sx] = passability;
			}
		}
		repaint();

	}

	private void makeEC(int x, int y, Team team) {
		EnlightenmentCenter enlightenmentCenter = new EnlightenmentCenter(x, y, team);
		enlightenmentCenters.add(enlightenmentCenter);
		repaint();
	}

	private void deleteEC(int x, int y) {
		// Deletes all ECs at a given location
		enlightenmentCenters.removeIf((ec) -> ec.x == x && ec.y == y);
		repaint();
	}

	public void setTool(Tool tool) {
		currentTool = tool;
		this.previousTileChanged = null;
		window.setCurrentTool(tool);
	}

	public void setSymmetry(Symmetry symmetry) {
		this.symmetry = symmetry;
		window.setCurrentSymmetry(symmetry);
	}

	public byte[] generateMap() {
		FlatBufferBuilder builder = new FlatBufferBuilder();

		// Spawned body information
		// this.bodiesArray = {
		// robotIDs: [],
		// teamIDs: [],
		// types: [],
		// xs: [],
		// ys: [],
		// influences: []
		// };

		// Get header information from form
		String name = window.getFileName();
		int[] minCorner = new int[] { (int) (Math.random() * 20000) + 10000, (int) (Math.random() * 20000) + 10000 };
		int[] maxCorner = new int[] { minCorner[0] + mapWidth, minCorner[1] + mapHeight };
		int randomSeed = (int) (Math.random() * 1000);

		// Parse body information

		// First convert ids, etc. to the schema
		int[] robotIds = new int[enlightenmentCenters.size()];
		byte[] teamIds = new byte[enlightenmentCenters.size()];
		byte[] types = new byte[enlightenmentCenters.size()];
		int[] influences = new int[enlightenmentCenters.size()];
		int[] xs = new int[enlightenmentCenters.size()];
		int[] ys = new int[enlightenmentCenters.size()];

		int i = 0;
		for (EnlightenmentCenter ec : enlightenmentCenters) {
			robotIds[i] = ec.id;

			switch (ec.team) {
			case RED:
				teamIds[i] = 1;
				break;
			case BLUE:
				teamIds[i] = 2;
				break;
			case NEUTRAL:
			default:
				teamIds[i] = 0;
				break;
			}

			types[i] = BodyType.ENLIGHTENMENT_CENTER;
			influences[i] = Constants.INITIAL_INFLUENCE;
			xs[i] = ec.x;
			ys[i] = ec.y;

			i++;
		}

		// Now we create the vectors
		int robotIdsVector = SpawnedBodyTable.createRobotIDsVector(builder, robotIds);
		int teamIdsVector = SpawnedBodyTable.createTeamIDsVector(builder, teamIds);
		int typesVector = SpawnedBodyTable.createTypesVector(builder, types);

		// In the middle for some reason we add in the locations
		final int xsP = VecTable.createXsVector(builder, xs);
		final int ysP = VecTable.createYsVector(builder, ys);

		int locsVector = VecTable.createVecTable(builder, xsP, ysP);
		int influencesVector = SpawnedBodyTable.createInfluencesVector(builder, influences);

		// Now we add in the vectors
		SpawnedBodyTable.startSpawnedBodyTable(builder);
		SpawnedBodyTable.addRobotIDs(builder, robotIdsVector);
		SpawnedBodyTable.addTeamIDs(builder, teamIdsVector);
		SpawnedBodyTable.addTypes(builder, typesVector);
		SpawnedBodyTable.addLocs(builder, locsVector);
		SpawnedBodyTable.addInfluences(builder, influencesVector);

		final int bodies = SpawnedBodyTable.endSpawnedBodyTable(builder);

		// Whew! We finished adding in the bodies.

		// Now we add in the passabilities
		double[] passabilities = new double[mapWidth * mapHeight];
		i = 0;
		for (int y = 0; y < mapHeight; y++) {
			for (int x = 0; x < mapWidth; x++) {
				passabilities[i] = passabilityMap[y][x];
				i++;
			}
		}
		final int passability = GameMap.createPassabilityVector(builder, passabilities);

		// Tie everything together.
		int nameP = builder.createString(name);
		GameMap.startGameMap(builder);
		GameMap.addName(builder, nameP);
		GameMap.addMinCorner(builder, Vec.createVec(builder, minCorner[0], minCorner[1]));
		GameMap.addMaxCorner(builder, Vec.createVec(builder, maxCorner[0], maxCorner[1]));
		GameMap.addBodies(builder, bodies);
		GameMap.addPassability(builder, passability);
		GameMap.addRandomSeed(builder, randomSeed);

		final int gameMap = GameMap.endGameMap(builder);

		// Return the game map to write to a file
		builder.finish(gameMap);
		byte[] returnvalue = builder.sizedByteArray();
		return builder.sizedByteArray();
	}

	public void openMap(String fileLocation) {
		// TODO: MAKE THIS
		/*
		 * try { int height = 0; int width = 0;
		 * 
		 * BufferedReader reader = new BufferedReader(new FileReader(fileLocation)); for
		 * (String line = reader.readLine(); line != null; line = reader.readLine()) {
		 * height += 1; width = line.length(); } reader.close();
		 * 
		 * this.mapWidth = width; this.mapHeight = height;
		 * 
		 * tileMap = new Tile[height][width];
		 * 
		 * reader = new BufferedReader(new FileReader(fileLocation)); int y = 0; for
		 * (String line = reader.readLine(); line != null; line = reader.readLine(),
		 * y++) { for (int x = 0; x < line.length(); x++) { tileMap[y][x] =
		 * Tile.fromChar(line.charAt(x)); } } reader.close();
		 * 
		 * } catch (IOException ex) { throw new RuntimeException(ex); } finally {
		 * repaint(); }
		 */
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (e.isShiftDown()) {
			cursorPosition = new int[] { e.getX(), e.getY() };
			repaint();
		}
		if (e.isShiftDown()) {
			return;
		}
		if (!SwingUtilities.isLeftMouseButton(e)) {
			return;
		}
		useMyTool(e.getX(), e.getY());
	}

	@Override
	public void mouseMoved(MouseEvent e) {

	}

	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.isShiftDown()) {
			startingPosition = new int[] { e.getX(), e.getY() };
			cursorPosition = new int[] { e.getX(), e.getY() };
		} else {
			switch (e.getButton()) {
			case MouseEvent.BUTTON1:
				useMyTool(e.getX(), e.getY());
				break;
			case MouseEvent.BUTTON2:
				break;
			case MouseEvent.BUTTON3:
				break;
			default:
				break;
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.isShiftDown()) {
			if (startingPosition != null) {
				setPassabilityRange(Math.min(startingPosition[0], e.getX()), Math.min(startingPosition[1], e.getY()),
						Math.max(startingPosition[0], e.getX()), Math.max(startingPosition[1], e.getY()),
						currentPassability);
				startingPosition = null;
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
			startingPosition = null;
			repaint();
		}

	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}

}
