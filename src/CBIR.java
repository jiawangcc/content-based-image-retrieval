
/* Project 1
*/

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.util.*;
import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.*;


public class CBIR extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private JLabel selectPicLabel = new JLabel(); // container to hold the query pic
	private JLabel pageDisplay = new JLabel(); //display current page in plain text
	private JButton[] button; // creates an array of JButtons
	private JCheckBox[] checkbox; //create an array of JCheckBoxes
	private JCheckBox relevance; //indicate whether to show all the RF checkboxes 
	private int[] buttonOrder = new int[101]; // creates an array to keep up with the image order
	private JPanel mainPanel; //panel for entire interface
	private JPanel topPanel;  //panel to hold query image on the left and the buttons on the right
	private JPanel buttonPanel; //panel to hold buttons "Retrieve by xx method", "reset"
	private JPanel pagePanel; //panel to hold page buttons
	private JPanel bottomPanel; //panel to hold picRow panel and boxRow panel
	private JPanel picRow; //panel to hold 5 pictures
	private JPanel boxRow; //panel to hold 5 checkbox
	
	private int[][] intensityMatrix = new int[100][26];//store image size and 25 intensity features of 100 images.
	private int[][] colorCodeMatrix = new int[100][65];//store image size and 64 colorcode features of 100 images.
	double featureMatrix[][] = new double [100][90]; ////store image size and 89 intensity + colorcode features of 100 images.
	int picNo = 0;      //keeps up with the query image
	int imageCount = 1; // keeps up with the number of images displayed
	int pageNo = 1;     // keeps up with the number of pages

	//main method
	public static void main(String args[]) {

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				CBIR app = new CBIR();
				app.setVisible(true);
			}
		});
	}

	//constructor to set up UI
	public CBIR() {		
		JFrame frame = new JFrame("CBIR: Please Select an Image");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

		topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		topPanel.setMaximumSize(new Dimension(700, 250));

		selectPicLabel = new JLabel();
		selectPicLabel.setLayout(new GridLayout(1, 1));
		selectPicLabel.setMaximumSize(new Dimension(340, 260));
		selectPicLabel.setHorizontalAlignment(JLabel.CENTER);
		selectPicLabel.setBorder(BorderFactory.createTitledBorder("Query Image"));

		buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(6, 1, 0, 5));
		buttonPanel.setMaximumSize(new Dimension(310, 260));
		JButton intensity = new JButton("Intensity");
		JButton colorCode = new JButton("Color-Code");
		JButton intensityAndColor = new JButton("Intensity + Color-Code");
		relevance = new JCheckBox("Relevance Feedback");
		relevance.setHorizontalAlignment(JLabel.CENTER);
		JButton reset = new JButton("Reset");

		buttonPanel.add(intensity);
		buttonPanel.add(colorCode);
		buttonPanel.add(intensityAndColor);
		buttonPanel.add(relevance);
		buttonPanel.add(reset);

		pagePanel = new JPanel();
		pagePanel.setLayout(new GridLayout(1, 3));
		JButton previous = new JButton("Previous");
		JButton next = new JButton("Next");
		pageDisplay = new JLabel("Page " + pageNo + " of 5");
		pageDisplay.setHorizontalTextPosition(JLabel.CENTER);
		pageDisplay.setHorizontalAlignment(JLabel.CENTER);
		pagePanel.add(previous);
		pagePanel.add(next);
		pagePanel.add(pageDisplay);


		bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
		bottomPanel.setMaximumSize(new Dimension(700, 540));
		picRow = new JPanel(); 
		boxRow = new JPanel(); 
		picRow.setMaximumSize(new Dimension(720, 110));
		picRow.setLayout(new GridLayout(1, 5));
		boxRow.setMaximumSize(new Dimension(720, 20));
		boxRow.setLayout(new GridLayout(1, 5));

		mainPanel.add(topPanel);
		mainPanel.add(bottomPanel);
		topPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		topPanel.add(selectPicLabel);
		topPanel.add(Box.createRigidArea(new Dimension(20, 0)));
		topPanel.add(buttonPanel);
		buttonPanel.add(pagePanel);
		
		intensity.addActionListener(new intensityHandler());
		colorCode.addActionListener(new colorCodeHandler());
		intensityAndColor.addActionListener(new intensityColorHandler());
		relevance.addActionListener(new RFcheckboxHandler());
		reset.addActionListener(new resetHandler());
		previous.addActionListener(new previousPageHandler());
		next.addActionListener(new nextPageHandler());
		
		frame.setContentPane(mainPanel);
		frame.setSize(720, 800);
		frame.setMinimumSize(new Dimension(720, 800));
		frame.setVisible(true);

		button = new JButton[101];
		/*
		 * This for loop goes through the images in the database and stores them
		 * as icons and adds the images to JButtons and then to the JButton
		 * array
		 */
		
		try {
			for (int i = 1; i < 101; i++) {
				final Image originalImage = ImageIO.read(getClass().getResource("images/" + i + ".jpg"));
				final ImageIcon icon = new ImageIcon(originalImage.getScaledInstance(-1, 80, Image.SCALE_SMOOTH));
	
				if (icon != null) {
					button[i] = new JButton(i + ".jpg", icon);
					button[i].setVerticalTextPosition(SwingConstants.BOTTOM);
					button[i].setHorizontalTextPosition(SwingConstants.CENTER);
					button[i].addActionListener(new IconButtonHandler(i, new ImageIcon(originalImage.getScaledInstance(-1, 180, Image.SCALE_SMOOTH))));
					buttonOrder[i] = i;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		//create 100 checkboxes
		checkbox = new JCheckBox[101];
		for (int i = 1; i < 101; i++){
			checkbox[i] = new JCheckBox("Relevant");
		}
		
		readIntensityFile();
		readColorCodeFile();
		readNormalizedFeatureFile();
		displayFirstPage();
	}

	/*
	 * This method opens the intensity text file containing the intensity matrix
	 * with the histogram bin values for each image. The contents of the matrix
	 * are processed and stored in a two dimensional array called
	 * intensityMatrix.
	 */
	public void readIntensityFile() {
		
		Scanner read;
		String line = "";
		int lineNumber = 0;
		try {
			read = new Scanner(new File("intensity.txt"));
			while (read.hasNextLine()) {
	            line = read.nextLine();
	            String[] s = line.split(",");
	            for (int i = 0; i < s.length; i++){
	            	intensityMatrix[lineNumber][i] = Integer.parseInt(s[i]);
	            }            	
	            lineNumber++;
			}
			
		} catch (FileNotFoundException EE) {
			System.out.println("The file intensity.txt does not exist");
		}

	}

	/*
	 * This method opens the color code text file containing the color code
	 * matrix with the histogram bin values for each image. The contents of the
	 * matrix are processed and stored in a two dimensional array called
	 * colorCodeMatrix.
	 */
	private void readColorCodeFile() {
		Scanner read;
		String line = "";
		int lineNumber = 0;
		try {
			read = new Scanner(new File("colorCodes.txt"));
			while (read.hasNextLine()) {
	            line = read.nextLine();
	            String[] s = line.split(",");
	            for (int i = 0; i < s.length; i++){
	            	colorCodeMatrix[lineNumber][i] = Integer.parseInt(s[i]);
	            }            	
	            lineNumber++;
			}
		} catch (FileNotFoundException EE) {
			System.out.println("The file colorCodes.txt does not exist");
		}

	}
	
	/*
	 * This method opens the normalized feature text file containing the normalized 
	 * feature matrix with the 89 bin values for each image. The contents of the
	 * matrix are processed and stored in a two dimensional array called
	 * featureMatrix.
	 */
	private void readNormalizedFeatureFile() {
		Scanner read;
		String line = "";
		int lineNumber = 0;
		try {
			read = new Scanner(new File("normalizedfeature.txt"));
			while (read.hasNextLine()) {
	            line = read.nextLine();
	            String[] s = line.split(",");
	            for (int i = 0; i < s.length; i++){
	            	featureMatrix[lineNumber][i] = Double.parseDouble(s[i]);
	            }            	
	            lineNumber++;
			}
		} catch (FileNotFoundException EE) {
			System.out.println("The file normalizedfeature.txt does not exist");
		}

	}
	
	/*
	 * This class implements an ActionListener for each iconButton. When an icon
	 * button is clicked, the image on the the button is added to the
	 * selectPicLabel and the picNo is set to the image number selected and
	 * being displayed.
	 */
	private class IconButtonHandler implements ActionListener {
		int pNo = 0;
		ImageIcon iconUsed;

		IconButtonHandler(int p, ImageIcon i) {
			pNo = p;
			iconUsed = i; // sets the icon to the one used in the button
		}

		public void actionPerformed(ActionEvent e) {
			selectPicLabel.setIcon(iconUsed);
			picNo = pNo;		
		}

	}

	/*
	 * This method displays the first twenty images in the bottomPanel. The for
	 * loop starts at number one and gets the image number stored in the
	 * buttonOrder array and assigns the value to imageButNo. The picRow
	 * associated with five image and boxRow associated with five checkboxes 
	 * are then added to bottomPanel. The for loop continues this process 
	 * until twenty images  and twenty checkboxes are displayed in the bottomPanel
	 */
	private void displayFirstPage() {
		
		int imageButNo = 0;
		bottomPanel.removeAll();
		
		for (int i = 1; i <= 20; i++){
			imageButNo = buttonOrder[i];
			picRow.add(button[imageButNo]);
			imageCount++;
			boxRow.add(checkbox[imageButNo]);
			boxRow.setVisible(relevance.isSelected());
			if (i%5 == 0){
				bottomPanel.add(picRow);
				bottomPanel.add(boxRow);
				picRow = new JPanel();
				boxRow = new JPanel();
				picRow.setMaximumSize(new Dimension(720, 110));
				picRow.setLayout(new GridLayout(1, 5));
				boxRow.setMaximumSize(new Dimension(720, 20));
				boxRow.setLayout(new GridLayout(1, 5));
				boxRow.setVisible(relevance.isSelected());
			}
		}
		
		bottomPanel.revalidate();
		bottomPanel.repaint();
	}
	
	/*
	 * This class implements an ActionListener for the nextPageButton. The last
	 * image number to be displayed is set to the current image count plus 20.
	 * If the endImage number equals 101, then the next page button does not
	 * display any new images because there are only 100 images to be displayed.
	 * The first picture on the next page is the image located in the
	 * buttonOrder array at the imageCount
	 */
	private class nextPageHandler implements ActionListener {

		public void actionPerformed(ActionEvent e) {

			int imageButNo = 0;
			int endImage = imageCount + 20;

			if (endImage <= 101) {
				pageNo = endImage / 20;
				pageDisplay.setText("Page " + pageNo + " of 5");
				bottomPanel.removeAll();
				for (int i = imageCount; i < endImage; i++) {
					imageButNo = buttonOrder[i];
					picRow.add(button[imageButNo]);
					imageCount++;
					boxRow.add(checkbox[imageButNo]);
					boxRow.setVisible(relevance.isSelected());
					if (i % 5 == 0) {
						bottomPanel.add(picRow);
						bottomPanel.add(boxRow);
						picRow = new JPanel();
						boxRow = new JPanel();
						picRow.setMaximumSize(new Dimension(720, 110));
						picRow.setLayout(new GridLayout(1, 5));
						boxRow.setMaximumSize(new Dimension(720, 20));
						boxRow.setLayout(new GridLayout(1, 5));
						boxRow.setVisible(relevance.isSelected());
					}

				}

				bottomPanel.revalidate();
				bottomPanel.repaint();
			}
		}

	}
    
	/*
	 * This class implements an ActionListener for the previousPageButton. The
	 * last image number to be displayed is set to the current image count minus
	 * 40. If the endImage number is less than 1, then the previous page button
	 * does not display any new images because the starting image is 1. The
	 * first picture on the next page is the image located in the buttonOrder
	 * array at the imageCount
	 */
	private class previousPageHandler implements ActionListener {

		public void actionPerformed(ActionEvent e) {

			int imageButNo = 0;
			int startImage = imageCount - 40;
			int endImage = imageCount - 20;

			if (startImage >= 1) {
				pageNo = startImage / 20 + 1;
				pageDisplay.setText("Page " + pageNo + " of 5");
				bottomPanel.removeAll();
				for (int i = startImage; i < endImage; i++) {
					imageButNo = buttonOrder[i];
					picRow.add(button[imageButNo]);
					imageCount--;
					boxRow.add(checkbox[imageButNo]);
					boxRow.setVisible(relevance.isSelected());
					if (i % 5 == 0) {
						bottomPanel.add(picRow);
						bottomPanel.add(boxRow);
						picRow = new JPanel();
						boxRow = new JPanel();
						picRow.setMaximumSize(new Dimension(720, 110));
						picRow.setLayout(new GridLayout(1, 5));
						boxRow.setMaximumSize(new Dimension(720, 20));
						boxRow.setLayout(new GridLayout(1, 5));
						boxRow.setVisible(relevance.isSelected());
					}
				}

				bottomPanel.revalidate();
				bottomPanel.repaint();
			}
		}

	}

	/*
	 * This class implements an ActionListener for relevance feedback checkbox.
	 * When it is checked, the 20 checkboxes on each page will be displayed and
	 * show the first page by default.
	 */
	private class RFcheckboxHandler implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			imageCount = 1;
			pageDisplay.setText("Page 1 of 5");
			displayFirstPage();
		}

	}
	
	/*
	 * This class implements an ActionListener for the reset button. When reset is
	 * clicked, all the images restore to the original order (from 1 to 100), clear 
	 * the query image, and also uncheck the relevance feedback.
	 */
	private class resetHandler implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			for (int i = 1; i < 101; i++){
				buttonOrder[i] = i;
			}
			imageCount = 1;
			picNo = 0;
			selectPicLabel.setIcon(null);
			for (int i = 1; i < 101; i++){
				checkbox[i].setSelected(false);
			}
			pageDisplay.setText("Page 1 of 5");
			relevance.setSelected(false);
			displayFirstPage();
		}

	}

	/*
	 * This class implements an ActionListener when the user selects the
	 * intensityHandler button. The image number that the user would like to
	 * find similar images for is stored in the variable picNo. The size of
	 * the image is retrieved from the intensityMatrix[*][0].In the computeDistance
	 * method selected image's intensity bin values are compared to all the other
	 * image's intensity bin values and a score is determined for how well the
	 * images compare. The images are then arranged from most similar to the least.
	 */
	private class intensityHandler implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			
			if( picNo == 0){
				return;
			}
			List<Integer> result = computeDistance(picNo);
			
			// Shift to right by one position
			result.add(0, -1);
			
			for (int i = 1; i < 101; i++) {
				buttonOrder[i] = result.get(i);		
			}
			imageCount = 1;
			pageDisplay.setText("Page 1 of 5");
			displayFirstPage();			
		}
		
		public List<Integer> computeDistance( int selectPicNum){
			
			int selectPicSize = intensityMatrix[selectPicNum - 1][0];
			List<PicDistance> arr = new ArrayList<>();
			
			for (int i = 0; i < 100; i++) {
				int currentPicSize = intensityMatrix[i][0];
				double d = 0;
				for (int j = 1; j < 26; j++){
					d = d + Math.abs(((double)intensityMatrix[selectPicNum-1][j] / (double)selectPicSize) - ((double)intensityMatrix[i][j] / (double)currentPicSize));
				}
				arr.add(new PicDistance(i,d));
			}
			
			Collections.sort(arr);
			
			return arr.stream().map(pd -> pd.imgNo + 1).collect(Collectors.toList());
		}
	}
	
	/*
	 * This class implements an ActionListener when the user selects the
	 * colorCode button. The image number that the user would like to find
	 * similar images for is stored in the variable picNo. The size of the image
	 * is retrieved from the colorCodeMatrix[*][0].In the computeDistance method
	 * selected image's colorCode bin values are compared to all the other
	 * image's colorCode bin values and a score is determined for how well the
	 * images compare. The images are then arranged from most similar to the
	 * least.
	 */
	private class colorCodeHandler implements ActionListener{

	      public void actionPerformed( ActionEvent e){
		
				if( picNo == 0){
					return;
				}
				List<Integer> result = computeDistance(picNo);
				
				// Shift to right by one position
				result.add(0, -1);
				
				for (int i = 1; i < 101; i++) {
					buttonOrder[i] = result.get(i);		
				}
				imageCount = 1;
				pageDisplay.setText("Page 1 of 5");
				displayFirstPage();			
			}
			
			public List<Integer> computeDistance( int selectPicNum){
				
				int selectPicSize = colorCodeMatrix[selectPicNum - 1][0];
				List<PicDistance> arr = new ArrayList<>();
				
				for (int i = 0; i < 100; i++) {
					int currentPicSize = colorCodeMatrix[i][0];
					double d = 0;
					for (int j = 1; j < 65; j++){
						d = d + Math.abs(((double)colorCodeMatrix[selectPicNum-1][j] / (double)selectPicSize) - ((double)colorCodeMatrix[i][j] / (double)currentPicSize));
					}
					arr.add(new PicDistance(i,d));
				}
				
				Collections.sort(arr);
				
				return arr.stream().map(pd -> pd.imgNo + 1).collect(Collectors.toList());
			} 
	}

	/*
	 * This class implements an ActionListener when the user selects the
	 * Intensity + Color-code button. The image number that the user would like
	 * to find similar images for is stored in the variable picNo. All the
	 * relevant image features are store in the relevantMatrix. Weights of 89
	 * features are stored in weigh[]. Use relevantMatrix to compute average and
	 * standard deviation. If initial iteration, set all weights to 1/89.
	 * Otherwise, set weights to 1/standard deviation and normalize each weight
	 * by dividing total weight. In the computeDistance method selected image's
	 * 89 normalized feature values are compared to all the other image's
	 * feature values and a score is determined for how well the images compare.
	 * The images are then arranged from most similar to the least.
	 */
	private class intensityColorHandler implements ActionListener {
		private boolean hasZeroStDev = false;
		double weight[] = new double[90];

		public void actionPerformed(ActionEvent e) {
			
			if( picNo == 0){
				return;
			}
			// find the relevant image number
			ArrayList<Integer> check = new ArrayList<Integer>();
			for (int i = 1; i < 101; i++) {
				if (checkbox[i].isSelected()) {
					check.add(i);
				}
			}

			// if no relevant image is selected(in the initial iteration), all weights = 1/89
			if (check.size() == 0) {
				for (int featureIndex = 1; featureIndex < 90; featureIndex++) {
					weight[featureIndex] = 1.0 / 89.0;
				}
			} else if (check.size() > 0) {

				double relevantMatrix[][] = new double[check.size()][90];

				// get relevant/positive matrix
				for (int i = 0; i < check.size(); i++) {
					for (int featureIndex = 1; featureIndex < 90; featureIndex++) {
						relevantMatrix[i][featureIndex] = featureMatrix[check.get(i)-1][featureIndex];
					}
				}

				double average[] = new double[90];
				double stdev[] = new double[90];
				weight = new double[90];
				// compute mean
				for (int featureIndex = 1; featureIndex < 90; featureIndex++) {
					double sum = 0.0;

					for (int i = 0; i < check.size(); i++) {
						sum += relevantMatrix[i][featureIndex];
					}
					average[featureIndex] = sum / (double)check.size();
				}

				// compute standard deviation
				for (int featureIndex = 1; featureIndex < 90; featureIndex++) {
					double sum = 0.0;

					for (int i = 0; i < check.size(); i++) {
						sum += Math.pow((relevantMatrix[i][featureIndex] - average[featureIndex]),2);
					}
					
					stdev[featureIndex] = Math.sqrt(sum/((double)check.size()-1.0));
					if (stdev[featureIndex] == 0) {
						hasZeroStDev = true;
					}
				}

				// if any feature standard deviation is 0, set it as half of
				// minimum non zero value
				ArrayList<Integer> meanZero = new ArrayList<Integer>();
				
				if (hasZeroStDev == true) {
					double[] copy = Arrays.copyOf(stdev, 90);
					Arrays.sort(copy);
					int i = 0;
					while (copy[i] == 0) {
						i++;
					}
					
					double minNonZero = copy[i];

					for (int j = 1; j < 90; j++) {
						if (stdev[j] == 0) {
							stdev[j] = minNonZero / 2.0;
							if (average[j] == 0) {
								meanZero.add(j);
							}
						}
					}
				}

				// compute normalized weight
				double weightSum = 0.0;
				double[] updateWeight = new double[90];
				
				for (int i = 1; i < 90; i++) {
					updateWeight[i] = 1.0 / stdev[i];
				}
				
				for (int i = 0; i < meanZero.size(); i++){
					updateWeight[meanZero.get(i)] = 0.0;
				}
				
				for (int i = 1; i < 90; i++) {
					weightSum += updateWeight[i];
				}

				for (int i = 1; i < 90; i++) {
					weight[i] = updateWeight[i] / weightSum;
				}
			}

			List<Integer> result = computeDistance(picNo, weight);
			// Shift to right by one position
			result.add(0, -1);
			for (int i = 1; i < 101; i++) {
				buttonOrder[i] = result.get(i);
			}
			imageCount = 1;
			pageDisplay.setText("Page 1 of 5");
			checkbox[picNo].setSelected(true);
			displayFirstPage();
		}

		public List<Integer> computeDistance(int selectPicNum, double weight[]) {

			List<PicDistance> arr = new ArrayList<>();

			for (int i = 0; i < 100; i++) {
				double d = 0.0;
				for (int j = 1; j < 90; j++) {
					d = d + weight[j] * Math.abs(featureMatrix[selectPicNum-1][j] - featureMatrix[i][j]);
				}
				arr.add(new PicDistance(i, d));
			}

			Collections.sort(arr);

			return arr.stream().map(pd -> pd.imgNo + 1).collect(Collectors.toList());
		}
	}

	/*
	 * This class implements the Comparable interface.This class facilitates Collections.sort()
	 * to achieve the sorting function.Each PicDistance instance has a imgNo variable for image 
	 * number and distance variable for the corresponding distance from query image. The 
	 * compareTo method compares distance and return the smaller distance and associated with 
	 * the image number.
	 */
	
	private class PicDistance implements Comparable<PicDistance> {
		
		private final int imgNo;
		private final double distance;
		
		public PicDistance(int imgNo, double distance) {
			this.imgNo = imgNo;
			this.distance = distance;
		}

		@Override
		public int compareTo(PicDistance o) {
			if (this.distance == o.distance) {
				return 0;
			} else {
				return (this.distance < o.distance) ? -1 : 1;
			}
		}
	}
}
	
	