import java.lang.Math;
import java.io.File;
import java.util.Calendar;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;

/**
 * @author Administrator
 *
 */
class User{
	private int[] userVector;
	private int userID;
	
	/**
	 * @param id a user's id, i.e., the directory's name of a user in GeolifeDA.DATASET_PATH.
	 * @param vec a user's feature vector
	 */
	User(int id, int[] vec){
		userID = id;
		userVector = vec;
	}
	/**
	 * @return a user's id
	 */
	public int getUserID(){
		return userID;
	}
	/**
	 * @returna user's feature vector
	 */
	public int[] getUserVector(){
		return this.userVector;
	}
	/**
	 * @param anotherUser
	 * @return the Manhattan distance or l1 distance between this user and another user.
	 */
	public int getSimilarity(User anotherUser){
		int sim = 0;
		int[] anotherUserVec = anotherUser.getUserVector();
		for(int i = 0; i < Math.min(userVector.length, anotherUserVec.length) ; i++){
			sim -= Math.abs(userVector[i] - anotherUserVec[i]);
		}
		return sim;
	}
}

class Dataset{
	private double[] locationRange;
	private double[] locationDelta;
	private double timeInterval;
	private int gridColNum;
	private int gridNum;
	private int userNum;
	
	/**
	 * @param delta delta[0] is the length of a grid, delta[1] is the width of a grid
	 * @param interval time sampling interval
	 */
	Dataset(double[] delta, double interval){
		locationRange = getLocationRange();
		initDataset(delta, interval);
	}
	/**
	 * @param delta delta[0] is the length of a grid, delta[1] is the width of a grid
	 * @param interval time sampling interval
	 * @param range Initialize the dataset object with a specified location range. 
	 * range[0] is the max latitude, range[1] is the min latitude; range[2] is the max longitude and range[3] is the min longitude.
	 */
	Dataset(double[] delta, double interval, double[] range){
		locationRange = range;
		initDataset(delta, interval);
	}
	private void initDataset(double[] delta, double interval){
		locationDelta = delta;
		timeInterval = interval;
		gridColNum = (int) Math.ceil((locationRange[2] - locationRange[3]) / locationDelta[1] + GeolifeDA.EPSILON);
		gridNum = getGridID(locationRange[0], locationRange[2]) + 1;
		
		userNum = 0;
		File datasetDir = new File(GeolifeDA.DATASET_PATH);
		try {
			if(datasetDir.exists()){
				File[] userDirs = datasetDir.listFiles();
				for(File file : userDirs){
					if(file.isDirectory()){
						userNum++;
					}
				}
			}
			else {
				throw new FileNotFoundException("Can not find Geolife Dataset in the specified path: " + GeolifeDA.DATASET_PATH);
			}
		} catch (FileNotFoundException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	/**
	 * @param latitude the latitude of a position
	 * @param longitude the longitude of a position
	 * @return id of the grid which a position fell in.
	 */
	private int getGridID(double latitude, double longitude){
		int gridID = -1;
		gridID = (int) ((longitude - locationRange[3]) / locationDelta[1]) +
				gridColNum * (int) ((latitude - locationRange[1]) / locationDelta[0]);
		return gridID;
	}
	/**
	 * @return the range of positions in current dataset.
	 * range[0] is the max latitude, range[1] is the min latitude; range[2] is the max longitude and range[3] is the min longitude.
	 */
	private double[] getLocationRange(){
		double maxLatitude = Double.NEGATIVE_INFINITY;
		double minLatitude = Double.POSITIVE_INFINITY;
		double maxLongitude = Double.NEGATIVE_INFINITY;
		double minLongitude = Double.POSITIVE_INFINITY;
		File datasetDir = new File(GeolifeDA.DATASET_PATH);
		RandomAccessFile plt;
		String pltLine;
		String[] elementStrings;
		double latitude, longitude;
		File[] userDirs = datasetDir.listFiles();
		for(File file : userDirs){
			if(file.isDirectory()){
				File pltDir = new File(file, "Trajectory");
				File[] pltFiles = pltDir.listFiles();
				for(File pltFile : pltFiles){
					try {
						plt = new RandomAccessFile(pltFile, "r");
						plt.seek(96);
						while ((pltLine = plt.readLine()) != null) {
							elementStrings = pltLine.split(",");
							latitude = Double.parseDouble(elementStrings[0]);
							longitude = Double.parseDouble(elementStrings[1]);
							if (latitude < minLatitude) {
								minLatitude = latitude;
							}
							else if (latitude > maxLatitude) {
								maxLatitude = latitude;
							}
							if (longitude < minLongitude) {
								minLongitude = longitude;
							}
							else if (longitude > maxLongitude) {
								maxLongitude = longitude;
							}
						}
						try {
							plt.close();
						} catch (IOException e) {
							// TODO: handle exception while trying to close a *.plt
							e.printStackTrace();
						}
					} catch (IOException e) {
						// TODO: handle exception while trying to open a *.plt
						e.printStackTrace();
					}
				}
			}
		}
		double[] range = new double[4];
		range[0] = maxLatitude;
		range[1] = minLatitude;
		range[2] = maxLongitude;
		range[3] = minLongitude;
		return range;
	}
	/**
	 * @return all of initialized user objects( with their corresponding feature vectors)
	 */
	public User[][] getAllUsers(){
		File datasetDir = new File(GeolifeDA.DATASET_PATH);
		File[] userDirs = datasetDir.listFiles();
		String pltLine;
		int userCnt = 0, userID = 0, gridID = 0, preGridID = 0, weekday = 0;
		int[] firstVec, lastVec;
		double dayNum = 0, preDayNum = 0;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-mm-dd");
		Calendar calendar = Calendar.getInstance();
		User[][] users = new User[2][];
		users[0] = new User[userNum];
		users[1] = new User[userNum];
		
		for(File file : userDirs){
			if(file.isDirectory()){
				firstVec = new int[gridNum];
				lastVec = new int[gridNum];
				String dirName = file.getName();
				userID = Integer.parseInt(dirName.substring(dirName.length() - 3, dirName.length()));
				File pltDir = new File(file, "Trajectory");
				File[] pltFiles = pltDir.listFiles();
				for(File pltFile : pltFiles){
					try {
						RandomAccessFile plt = new RandomAccessFile(pltFile, "r");
						plt.seek(96);
						while ((pltLine = plt.readLine()) != null) {
							String[] elementStrings = pltLine.split(",");
							gridID = getGridID(Double.parseDouble(elementStrings[0]), Double.parseDouble(elementStrings[1]));
							dayNum = Double.parseDouble(elementStrings[4]);
							if(preGridID != gridID){
								preGridID = gridID;
								preDayNum = dayNum;
							}
							else if (dayNum >= preDayNum + timeInterval) {
								preDayNum = dayNum;
								try {
									calendar.setTime(simpleDateFormat.parse(elementStrings[5]));
								} catch (ParseException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								weekday = calendar.get(Calendar.DAY_OF_WEEK);
								if(1 <= weekday && weekday <= 3){
									// if the day id between Monday and Wednesday
									firstVec[gridID]++;
								}
								else if (4 <= weekday && weekday <= 5) {
									lastVec[gridID]++;
								}
							}
						}
						try {
							plt.close();
						} catch (IOException e) {
							// TODO: handle exception while trying to close a *.plt
							e.printStackTrace();
						}
					} catch (IOException e) {
						// TODO: handle exception while trying to open a *.plt
						e.printStackTrace();
					}
				}
				users[0][userCnt] = new User(userID, firstVec);
				users[1][userCnt] = new User(userID, lastVec);
				userCnt++;
			}
		}
		return users;
	}
}

class BipartiteGraph{
	private int singlePartVertexNum;
	private int lx[], ly[];
	private boolean sx[], sy[];
	private int match[];
	private int weightMatrix[][];
	
	/**
	 * @param part1 a part of vertex of a bipartitegraph
	 * @param part2 the other part of vertex of a bipartitegraph
	 */
	BipartiteGraph(User[] part1, User[] part2) {
		singlePartVertexNum = Math.max(part1.length, part2.length);
		lx = new int[singlePartVertexNum];
		ly = new int[singlePartVertexNum];
		sx = new boolean[singlePartVertexNum];
		sy = new boolean[singlePartVertexNum];
		match = new int[singlePartVertexNum];
		initWeightMatrix(part1, part2);
	}
	private boolean searchPath(int u){
		sx[u] = true;
		for (int i = 0; i < singlePartVertexNum; i++) {
			if (!sy[i] && (lx[u] + ly[i] == weightMatrix[u][i])) {
				sy[i] = true;
				if (match[i] == -1 || searchPath(match[i])) {
					match[i] = u;
					return true;
				}
			}
		}
		return false;
	}
	/**
	 * @return a perfect match of a bipartite graph using Kuhn¨CMunkres algorithm
	 */
	public int km(){
		for (int i = 0; i < singlePartVertexNum; i++) {
			lx[i] = Integer.MIN_VALUE;
//			ly[i] = 0;
			match[i] = -1;
		}
		for (int i = 0, j; i < singlePartVertexNum; i++) {
			for (j = 0; j < singlePartVertexNum; j++) {
				if (lx[i] < weightMatrix[i][j]) {
					lx[i] = weightMatrix[i][j];
				}
			}
		}
		for (int k = 0, i, j; k < singlePartVertexNum; k++) {
			while (true) {
				for (i = 0; i < singlePartVertexNum; i++) {
					sx[i] = false;
					sy[i] = false;
				}
				if (searchPath(k)) {
					break;
				}
				int inc = Integer.MAX_VALUE;
				for (i = 0; i < singlePartVertexNum; i++) {
					if (sx[i]) {
						for (j = 0; j < singlePartVertexNum; j++) {
							if (!sy[j] && (lx[i] + ly[j] - weightMatrix[i][j] < inc)) {
								inc = lx[i] + ly[j] - weightMatrix[i][j];
							}
						}
					}
				}
				if(inc == 0)
					System.out.println("error");
				for (i = 0; i < singlePartVertexNum; i++) {
					if (sx[i]) {
						lx[i] -=inc;
					}
					if (sy[i]) {
						ly[i] += inc;
					}
				}
			}
		}
		int weightSum = 0;
		for (int i = 0; i < singlePartVertexNum; i++) {
			if (match[i] >= 0) {
				weightSum += weightMatrix[match[i]][i];
			}
		}
		return weightSum;
	}
	private void initWeightMatrix(User[] part1, User[] part2){
		weightMatrix = new int[singlePartVertexNum][];
		for (int i = 0; i < part1.length; i++) {
			weightMatrix[i] = new int[singlePartVertexNum];
			for (int j = 0; j < part2.length; j++) {
				weightMatrix[i][j] = part1[i].getSimilarity(part2[j]);
			}
		}
		for (int i = part1.length; i < singlePartVertexNum; i++) {
			// Make sure the weightMatrix is a square matrix.
			// All of the rest elements (if exist) are assigned to zero.
			weightMatrix[i] = new int[singlePartVertexNum];
		}
	}
	public int[] getPerfectMatch(){
		return match;
	}
	public double getHitRate(){
		double hitNum = 0;
		for (int i = 0; i < match.length; i++) {
			if (i == match[i]) {
				hitNum++;
			}
		}
		return hitNum / singlePartVertexNum;
	}
}

public class GeolifeDA {
	public static final double EPSILON = 0.00000001;
	public static final String DATASET_PATH = "Z://Data";
	private static void printIntArray(int[] intArray){
		for (int i = 0; i < intArray.length - 1; i++) {
			System.out.print(intArray[i]);
			System.out.print(", ");
		}
		System.out.println(intArray[intArray.length - 1]);
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		double[] locationDelta = {0.01, 0.01};
		double timeInterval = 0.0001;
		double[] locationRange = {40.6649833333333, 39.0857, 117.290333333333, 115.771263}; 
		Dataset dataset = new Dataset(locationDelta, timeInterval, locationRange);
		User[][] users = dataset.getAllUsers();
		BipartiteGraph bipartiteGraph = new BipartiteGraph(users[0], users[1]);
		bipartiteGraph.km();
		int[] biGraphPerfectMatch = bipartiteGraph.getPerfectMatch();
		System.out.print("The perfect match for bipartite graph is: ");
		printIntArray(biGraphPerfectMatch);
		System.out.print("The hit rate of de-anonymizing Geolife Dataset is: ");
		System.out.println(bipartiteGraph.getHitRate());
	}
}
