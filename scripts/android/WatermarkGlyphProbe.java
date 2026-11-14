// Test fixture only: fixed strings reproduce an existing reference capture.
// Never package this shell probe into the application or use its clock in production.
import android.graphics.*;
import java.io.*;
public class WatermarkGlyphProbe {
 public static void main(String[] args) throws Exception {
  String root=args[0]; new File(root).mkdirs();
  Typeface time=Typeface.createFromFile(root+"/BigShouldersText-Medium.ttf");
  Typeface text=Typeface.createFromFile(root+"/RobotoCondensed-Regular.ttf");
  String[] values={"15:32","14 Tháng 11,2026","Thứ Bảy","234 Example Street, Example District,"};
  for(int kind=0;kind<values.length;kind++) for(int quarter=kind==0?480:144;quarter<=(kind==0?544:184);quarter++) {
   Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);p.setColor(Color.WHITE);p.setTextSize(quarter/4f);p.setTypeface(kind==0?time:text);
   Bitmap bitmap=Bitmap.createBitmap(1100,220,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bitmap);c.drawColor(Color.BLACK);c.drawText(values[kind],10,170,p);
   try(FileOutputStream output=new FileOutputStream(root+"/"+kind+"-"+quarter+".png")){bitmap.compress(Bitmap.CompressFormat.PNG,100,output);}bitmap.recycle();
  }
 }
}
