package mapmaker;

public class EnlightenmentCenter {
	public final int x;
	public final int y;
	public final int id;
	public final Team team;
	
	public static int currentId = 0;

	public EnlightenmentCenter(int x, int y, Team team) {
		this.x = x;
		this.y = y;
		this.team = team;

		id = ++currentId;
	}

}
