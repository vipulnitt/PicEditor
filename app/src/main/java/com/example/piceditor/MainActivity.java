package com.example.piceditor;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    String str;
    boolean doodle = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permission();
       init();
    }
    public void permission()
    {
        Dexter.withContext(this)
                .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();

    }
    private static final int REQUEST_PICK_IMAGE=12;
    private static final int REQUEST_IMAGE_CAPUTRE = 11;
    private static final String appid="photoEditor";
    private Uri imageuri;
    public  boolean editMode = false;
    private Bitmap bitmap;
    private int height =0;
    private int width = 0;
    private static final int MAX_PIX = 2048;
    private int[]  pixels;
    private int pixelcount =0;
    private ImageView imageView;
    @SuppressLint("UnsupportedChromeOsCameraSystemFeature")
    private  void init() {
        final Button selectpic = findViewById(R.id.selectpic);
        final Button takepic = findViewById(R.id.takepic);
        imageView = findViewById(R.id.imageView);
        final Button grayscalebtn = findViewById(R.id.grayscale);
        final Button addtext = findViewById(R.id.addtext);
        final EditText editText = findViewById(R.id.edittext);
        final Button buttondoodle = findViewById(R.id.doodle);
        final Button savbtn = findViewById(R.id.save);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
        {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }
      if(!MainActivity.this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
        {
            findViewById(R.id.takepic).setVisibility(View.GONE);
        }
        selectpic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT );
                intent.setType("image/*");
                final Intent pickintent = new Intent(Intent.ACTION_PICK);
                pickintent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"images/*");
                final Intent intentchooser = Intent.createChooser(intent,"Select Image");
                startActivityForResult(intentchooser,REQUEST_PICK_IMAGE);
            }
        });
        takepic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent takepicintent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(takepicintent.resolveActivity(getPackageManager())!=null)
                {
                    final File photofile = createImagefile();
                    imageuri = Uri.fromFile(photofile);
                    final SharedPreferences mypref = getSharedPreferences(appid,0);
                    mypref.edit().putString("path",photofile.getAbsolutePath()).apply();
                    takepicintent.putExtra(MediaStore.EXTRA_OUTPUT,imageuri);
                    startActivityForResult(takepicintent,REQUEST_IMAGE_CAPUTRE);
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Camera Not supported", Toast.LENGTH_SHORT).show();
                }

            }
        });
        grayscalebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(){
                    public void run()
                    {
                        bitmap = convertToBlackWhite(bitmap);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageBitmap(bitmap);
                        }
                    });
                    }
                }.start();
            }
        });
        addtext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(){
                    public void run()
                    {
                        str = editText.getText().toString().trim();
                        bitmap =drawTextToBitmap(bitmap,str);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                }.start();
            }
        });
        buttondoodle.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                if(doodle==false)
                {
                    buttondoodle.setText("OFF Doodle");
                    doodle = true;
                } else if(doodle==true)
                {
                    buttondoodle.setText("ON Doodle");
                    doodle = false;
                }

            }
        });
        savbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                final DialogInterface.OnClickListener dialogclicklistner = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(which==DialogInterface.BUTTON_POSITIVE){
                            final File outFile = createImagefile();
                            try(FileOutputStream out = new FileOutputStream(outFile)){
                             bitmap.compress(Bitmap.CompressFormat.JPEG,90,out);
                             imageuri = Uri.parse("file://"+outFile.getAbsolutePath());
                             sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,imageuri));
                             Toast.makeText(MainActivity.this,"The Image is Saved",Toast.LENGTH_SHORT).show();
                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                builder.setMessage("Saved Current Photo To Gallery").setPositiveButton("yes",dialogclicklistner).setNegativeButton("no",dialogclicklistner ).show();

            }
        });

    }
    private File createImagefile()
    {
        final String timestamp = new SimpleDateFormat("yyyyMMDD_HHmmss").format(new Date());
        final String imagefilename = "/JPEG"+timestamp+".jpg";
        final File storage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(storage+imagefilename);
    }

    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent data)
    {
     super.onActivityResult(requestCode,resultCode,data);
     if(resultCode!=RESULT_OK)
     {
         return;
     }
     if(requestCode==REQUEST_IMAGE_CAPUTRE)
     {
         if(imageuri==null)
         {
             final SharedPreferences sharedPreferences = getSharedPreferences(appid,0);
             final String path = sharedPreferences.getString("path","");
            if (path.length()<1)
            {
                recreate();
                return;
            }
            imageuri = Uri.parse("file://"+path);
         }
         sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE));
     }
     else if(data==null)
     {
         recreate();
         return;
     } else if(requestCode == REQUEST_PICK_IMAGE)
     {
         imageuri = data.getData();
     }
     final ProgressDialog dialog = ProgressDialog.show(MainActivity.this,"Loading!","Please Wait..",true);
     editMode = true;
     findViewById(R.id.welcome).setVisibility(View.GONE);
     findViewById(R.id.editscreen).setVisibility(View.VISIBLE);
     new Thread()
     {
         public void run()
         {
bitmap =null;
final BitmapFactory.Options bmpOptions = new BitmapFactory.Options();
bmpOptions.inBitmap = bitmap;
bmpOptions.inJustDecodeBounds = true;
try(InputStream inputStream = getContentResolver().openInputStream(imageuri))
{
    bitmap = BitmapFactory.decodeStream(inputStream,null,bmpOptions);
}
catch (IOException e){
    e.printStackTrace();
}
bmpOptions.inJustDecodeBounds = false;
width = bmpOptions.outWidth;
height = bmpOptions.outHeight;
int resizeScale =1,check=0;
if(width>MAX_PIX)
{
resizeScale = width/MAX_PIX;
check=1;
}
else if(height>MAX_PIX)
{
resizeScale = height/MAX_PIX;
check=1;
}
if(check==1)
{
    resizeScale++;
}
             bmpOptions.inSampleSize = resizeScale;
              InputStream inputStream = null;
              try {
                  inputStream = getContentResolver().openInputStream(imageuri);
              }
              catch (FileNotFoundException e)
              {
                e.printStackTrace();
                recreate();
                return;
              }
              bitmap = BitmapFactory.decodeStream(inputStream,null,bmpOptions);
              runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                      imageView.setImageBitmap(bitmap);
                      dialog.cancel();
                  }
              });
              width = bitmap.getWidth();
              height = bitmap.getHeight();
              bitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
              pixelcount = width*height;
              pixels = new int[pixelcount];
              bitmap.getPixels(pixels,0,width,0,0,width,height);
         }
     }.start();

    }
    public static Bitmap convertToBlackWhite(Bitmap bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int[] pixels = new int[width * height];
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);

        int alpha = 0xFF << 24; // ?bitmap?24?
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];

                int red = ((grey & 0x00FF0000) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);

                grey = (int) (red * 0.3 + green * 0.59 + blue * 0.11);
                grey = alpha | (grey << 16) | (grey << 8) | grey;
                pixels[width * i + j] = grey;
            }
        }
        Bitmap newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        newBmp.setPixels(pixels, 0, width, 0, 0, width, height);
        return newBmp;
    }
    public static Bitmap drawTextToBitmap(Bitmap bitmap,String gText) {

        android.graphics.Bitmap.Config bitmapConfig =
                bitmap.getConfig();
        if(bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        bitmap = bitmap.copy(bitmapConfig, true);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(61, 61, 61));
        paint.setTextSize((int) (bitmap.getHeight()/10));
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);
        Rect bounds = new Rect();
        paint.getTextBounds(gText, 0, gText.length(), bounds);
        int x,y;
        x = (bitmap.getWidth() - bounds.width())/2;
        y =2*(bounds.height());
        canvas.drawText(gText, x, y, paint);
        return bitmap;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean value =super.onTouchEvent(event);
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            {
                return true;
            }
            case MotionEvent.ACTION_MOVE:{
                float X = event.getX()%bitmap.getWidth();
                float Y = event.getY()% bitmap.getHeight();

                if(X<bitmap.getWidth()&&Y<bitmap.getHeight())
                 if(doodle==true) {
                     bitmap = drawDoodle(bitmap, X ,Y);
                     imageView.setImageBitmap(bitmap);
                 }
                return true;
                    }

        }
        return value;
    }
    public static Bitmap drawDoodle(Bitmap bitmap,Float x,Float y) {

        android.graphics.Bitmap.Config bitmapConfig =
                bitmap.getConfig();
        if(bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        bitmap = bitmap.copy(bitmapConfig, true);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(61, 0, 0));
        canvas.drawCircle(x,y,10,paint);
        return bitmap;
    }
}