package net.sourceforge.meid;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Arrays;

import net.sourceforge.meid.R;

import android.R.string;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

//import com.androidplot.series.XYSeries;
//import com.androidplot.xy.LineAndPointFormatter;
//import com.androidplot.xy.SimpleXYSeries;
//import com.androidplot.xy.XYPlot;
//import com.androidplot.xy.XYStepMode;
//import com.example.tryone.R;
//import com.example.tryone.MainActivity.MyHistogram;
//import com.example.tryone.R;
//import com.example.tryone.MainActivity.MyHistogram;


public class histogram extends Activity {
	
	
	
	  boolean isColored= false;
    // Red, Green, Blue
    private int NUMBER_OF_COLOURS = 3;
    Bitmap   greyBmp=null;
    public final int RED = 0;
    public final int GREEN = 1;
    public final int BLUE = 2;
    private int[][] colourBins;
    private int maxY;
    float offset = 1f;
	private int SIZE = 256;
	LinearLayout view;
	LinearLayout view_color;
	//private XYPlot xyPlot;
	

	 /*
	 * Method for Converting Image into GrayScale
	 */
	     
	public Bitmap toGrayscale(Bitmap bmpOriginal)
	{       
	            int width, height;
	            height = bmpOriginal.getHeight();
	            width = bmpOriginal.getWidth();   
	            Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
	            Canvas canvas = new Canvas(bmpGrayscale);
	            Paint paint = new Paint();
	            ColorMatrix colorMatrix = new ColorMatrix();
	            colorMatrix.setSaturation(0);
	            ColorMatrixColorFilter f = new ColorMatrixColorFilter(colorMatrix);
	            paint.setColorFilter(f);
	            canvas.drawBitmap(bmpOriginal, 0, 0, paint);
	            return bmpGrayscale;
	}
	
	
    /*
    * Method for Getting image from Assets
    */

public static Bitmap getBitmapFromAsset(Context context, String image_path)
{
         AssetManager assetManager = context.getAssets();
         InputStream inputStream = null;
         Bitmap bitmap = null;
         try
         {
               inputStream = assetManager.open(image_path);
               bitmap = BitmapFactory.decodeStream(inputStream);
         }
         catch(Exception exception)
         {
               exception.printStackTrace();
               bitmap = null;
         }
         return bitmap;
}
   Button button;    ImageView image;
	//private String preview_image_name = null;
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.histogram);
 
        // initialize our XYPlot reference:
     //	        image = (ImageView) findViewById(R.id.image);
        	 
        	        
       //	        xyPlot = (XYPlot) findViewById(R.id.xyplot);
       	     Bitmap d =null;

    String    preview_image_name  = getIntent().getExtras().getString("names");
    if(preview_image_name!= null){
        File file = new File(preview_image_name);
      
        
        String im=file.getAbsolutePath();
        
         d = BitmapFactory.decodeFile(im);
        
      
        }
    

	//Bitmap greyBmp=null;
	
	
	   greyBmp = toGrayscale(d);
       //  image.setImageBitmap(greyBmp);
        int h = greyBmp.getHeight(); int w = greyBmp.getWidth();
        
        
  //      int []histogram= new int [256];
        // initialize all intensity values to 0
    //    for(int i = 0; i < 255; i++)
     //   {
      //      histogram[i] = 0;
      //  }
    
        colourBins = new int[NUMBER_OF_COLOURS][];

        for (int i = 0; i < NUMBER_OF_COLOURS; i++) {
            colourBins[i] = new int[SIZE];
        }
        
        for (int i = 0; i < NUMBER_OF_COLOURS; i++) {
            for (int j = 0; j < SIZE; j++) {
                colourBins[i][j] = 0;
            }
        }
        
        for (int x = 0; x < d.getWidth(); x++) {
            for (int y = 0; y < d.getHeight(); y++) {

                int pixel = d.getPixel(x, y);

                colourBins[RED][Color.red(pixel)]++;
                colourBins[GREEN][Color.green(pixel)]++;
                colourBins[BLUE][Color.blue(pixel)]++;
            }
        }

        maxY = 0;

        for (int i = 0; i < NUMBER_OF_COLOURS; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (maxY < colourBins[i][j]) {
                    maxY = colourBins[i][j];
                }
            }
        }
        
        view = (LinearLayout) findViewById(R.id.lyt);
        view.addView(new MyHistogram(getApplicationContext(),d));
        
        isColored = true;
        //view_color = (LinearLayout) findViewById(R.id.lyt_color);
        //view_color.addView(new MyHistogram(getApplicationContext(),
        	//	greyBmp));
        // calculate the no of pixels for each intensity values
      // for(int y = 0; y < w; y++)
      //     for(int x = 0; x < h; x++)
             //   histogram[Color.greyBmp.getPixel(w, h)]++;

        
        // find the maximum intensity element from histogram
     //  int max = histogram[0];
       // for(int i = 1; i < 255; i++){
         //   if(max < histogram[i]){
           //     max = histogram[i];
            //}
        //}
        
        // normalize the histogram between 0 and histImage.rows

      //  for(int i = 0; i < 255; i++){
         //   histogram[i] = ((int)histogram[i]/max)*(256);
        //}
              

    }

	
	/////////////////////////////////////////////////////////////
	   class MyHistogram extends View {

	        public MyHistogram(Context context, Bitmap bi) {
	            super(context);

	        }

	        @Override
	        protected void onDraw(Canvas canvas) {
	            // TODO Auto-generated method stub
	            super.onDraw(canvas);

	           
	                canvas.drawColor(Color.GRAY);

	                Log.e("NIRAV", "Height : " + getHeight() + ", Width : "
	                        + getWidth());

	                int xInterval = (int) ((double) getWidth() / ((double) SIZE + 1));

	                for (int i = 0; i < NUMBER_OF_COLOURS; i++) {

	                    Paint wallpaint;

	                    wallpaint = new Paint();
	                    if (isColored) {
	                        if (i == RED) {
	                            wallpaint.setColor(Color.RED);
	                        } else if (i == GREEN) {
	                            wallpaint.setColor(Color.GREEN);
	                        } else if (i == BLUE) {
	                            wallpaint.setColor(Color.BLUE);
	                        }
	                    } else {
	                        wallpaint.setColor(Color.YELLOW);
	                    }
	                    
	                   

	                    wallpaint.setStyle(Style.FILL);

	                    Path wallpath = new Path();
	                    wallpath.reset();
	                    wallpath.moveTo(0, getHeight());
	                    for (int j = 0; j < SIZE - 1; j++) {
	                        int value = (int) (((double) colourBins[i][j] / (double) maxY) * (getHeight()+100));


	                         //if(j==0) { 
	                        //   wallpath.moveTo(j * xInterval* offset, getHeight() - value); 
	                         //} 
	                        // else {
	                             wallpath.lineTo(j * xInterval * offset, getHeight() - value);
	                        // }
	                    }
	                    wallpath.lineTo(SIZE * offset, getHeight());
	                    canvas.drawPath(wallpath, wallpaint);
	                }

	            }

	        
	    }
	   
	    @Override
	     protected Dialog onCreateDialog(int id) {
	            ProgressDialog dataLoadProgress = new ProgressDialog(this);
	            dataLoadProgress.setMessage("Loading...");
	            dataLoadProgress.setIndeterminate(true);
	            dataLoadProgress.setCancelable(false);
	            dataLoadProgress.setProgressStyle(android.R.attr.progressBarStyleLarge);
	            return dataLoadProgress;

	        }
   
}