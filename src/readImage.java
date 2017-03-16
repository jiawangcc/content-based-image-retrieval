
/*
 * Project 1
*/

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import javax.imageio.ImageIO;

public class readImage {
		
	int intensityBins[] = new int[26]; //stores 25 features of intensity for each image
	int intensityMatrix[][] = new int[100][26]; //stores 25 features of intensity for 100 images
	int colorCodeBins[] = new int[65]; //stores 64 features of colorCode for each image
	int colorCodeMatrix[][] = new int[100][65]; //stores 64 features of colorCode for 100 images
	double featureMatrix[][] = new double [100][90]; //stores 89 features of intensity and colorCode for 100 images

	/*
	 * Each image is retrieved from the file. The height and width are found for
	 * the image and the getIntensity and getColorCode methods are called.The
	 * intensity bin and colorCode bin values are filled into intensityMatrix
	 * and colorCodeMatrix. Call getNormalizedFeature method to compute the
	 * normalized feature based on the values of intensityMatrix and
	 * colorCodeMatrix. And then writeIntensity, writeColorCode and
	 * writeNormalizedFeatureare called.
	 */
	public readImage() {
		
		for (int imgNo = 0; imgNo < 100; imgNo++) {			
			try {
				// get the BufferedImage, using the ImageIO class
				BufferedImage image = ImageIO.read(this.getClass().getResource("images/" + (imgNo + 1) + ".jpg"));

				
				getIntensity(image, image.getHeight(), image.getWidth());
				for (int i = 0; i < intensityBins.length; i++) {
					intensityMatrix[imgNo][i] = intensityBins[i];
				}

				getColorCode(image, image.getHeight(), image.getWidth());
				for (int i = 0; i < colorCodeBins.length; i++) {
					colorCodeMatrix[imgNo][i] = colorCodeBins[i];
				}
				
			} catch (IOException e) {
				System.out.println("Error occurred when reading the file.");
			}
		}
	
		getNormalizedFeature();
		writeIntensity();
		writeColorCode();
		writeNormalizedFeature();
		
		System.out.println("Image features calculation done.");
	}

	/*
	* compute 25 intensity bin values for each image and store the result
	* in intensityBins. 
	*/
	private void getIntensity(BufferedImage image, int height, int width) {

		intensityBins = new int[26];
		intensityBins[0] = height * width; //use [0] to store img size

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				int pixel = image.getRGB(j, i);
				Color col = new Color(pixel, true);
				int r = col.getRed();
				int g = col.getGreen();
				int b = col.getBlue();
				
				//compute intensity of each pixel
				double intensity = 0.299 * r + 0.587 * g + 0.114 * b; 
				int binNum = (int) intensity / 10;

				if (binNum == 25) { // the last bin range is 240 - 255;
					binNum = 24;
				}

				intensityBins[binNum + 1]++;
			}
		}

	}

	/*
	* compute 64 color-code bin values for each image and store the result
	* in colorCodeBins. 
	*/
	public void getColorCode(BufferedImage image, int height, int width) {
		colorCodeBins = new int[65];
		colorCodeBins[0] = height * width; //use [0] to store img size

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				int pixel = image.getRGB(j, i);
				Color col = new Color(pixel, true);

				int r = col.getRed();
				byte br = (byte) (r & 0xFF); // turn 32bits int to an 8bits byte
				String sr = String.format("%8s", Integer.toBinaryString(br & 0xFF)).replace(' ', '0'); // binary
																										// string
				sr = sr.substring(0, 2); // take the first two bits

				int g = col.getGreen();
				byte bg = (byte) (g & 0xFF);
				String sg = String.format("%8s", Integer.toBinaryString(bg & 0xFF)).replace(' ', '0');
				sg = sg.substring(0, 2);

				int b = col.getBlue();
				byte bb = (byte) (b & 0xFF);
				String sb = String.format("%8s", Integer.toBinaryString(bb & 0xFF)).replace(' ', '0');
				sb = sb.substring(0, 2);

				String colorCode = sr + sg + sb; //combine the first two bits of r, g, b
				int binNum = Integer.parseInt(colorCode, 2);
				colorCodeBins[binNum + 1]++;
			}
		}
	}
	
	/*
	* Compute 89 intensity + color-code bin values for 100 images and store the values
	* in featureMatrix. Compute average and standard deviation of each feature. Normalize the 
	* entire featureMatrix using Gaussian normalization.
	*/
	public void getNormalizedFeature() {
		
		for (int i = 0; i < 100; i++){
			
			featureMatrix[i][0] = (double)intensityMatrix[i][0]; //first bin stores image size
			
			for (int j = 1; j <= 25; j++){
				featureMatrix[i][j] = (double)intensityMatrix[i][j]/featureMatrix[i][0] ;
			}
			for (int j = 1; j <= 64; j++){
				featureMatrix[i][j+25] = (double)colorCodeMatrix[i][j]/featureMatrix[i][0] ;
			}		
		}
		
		double average[] = new double[90];
		double stdev[] = new double[90];
		
		for(int i = 1; i < 90; i++){
			double sum = 0.0;
			
			for (int j = 0; j < 100; j++){			 
				 sum += featureMatrix[j][i];
			}
			average[i] = sum/100.0;
		}
		
		for(int i = 1; i < 90; i++){
			double sum = 0.0;
			
			for (int j = 0; j < 100; j++){
				sum  +=  Math.pow((featureMatrix[j][i] - average[i]),2);
			}
			stdev[i] = Math.sqrt(sum/99.0);
		}
		
		for (int i = 0; i < 100; i++){
			for (int j = 1; j < 90; j++){
				if (stdev[j] == 0){
					featureMatrix[i][j] = 0;
				}
				else{
				    featureMatrix[i][j] = (featureMatrix[i][j] - average[j])/stdev[j];
				}
			}
		}
	}

	// This method writes the contents of the colorCode matrix to a file named
	// colorCodes.txt.
	public void writeColorCode() {

		PrintWriter writer;
		try {
			writer = new PrintWriter("colorCodes.txt", "UTF-8");

			for (int i = 0; i < colorCodeMatrix.length; i++) { // 100
				for (int j = 0; j < colorCodeBins.length; j++) { // 65
					writer.print(colorCodeMatrix[i][j] + ",");
				}
				writer.println();
			}
			writer.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// This method writes the contents of the intensity matrix to a file called
	// intensity.txt
	public void writeIntensity() {

		PrintWriter writer;
		try {
			writer = new PrintWriter("intensity.txt", "UTF-8");

			for (int i = 0; i < intensityMatrix.length; i++) { // 100
				for (int j = 0; j < intensityBins.length; j++) { // 26
					writer.print(intensityMatrix[i][j] + ",");
				}
				writer.println();
			}
			writer.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	// This method writes the contents of the feature matrix to a file called
	// normalizedfeature.txt
	public void writeNormalizedFeature() {

		PrintWriter writer;
		try {
			writer = new PrintWriter("normalizedfeature.txt", "UTF-8");

			for (int i = 0; i < 100; i++) { // 100
				for (int j = 0; j < 90; j++) { // 90
					writer.print(featureMatrix[i][j] + ",");
				}
				writer.println();
			}
			writer.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	//main method 
	public static void main(String[] args) {
		new readImage();
	}

}
