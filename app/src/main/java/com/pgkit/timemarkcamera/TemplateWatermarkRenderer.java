package com.pgkit.timemarkcamera;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Original Canvas implementation of the packaged template families, not decompiled application code.
 * Some templates are known only from packaged covers: their geometry is a reconstruction, not a
 * claim of pixel parity. Preview and output share design units, with output based on its short edge.
 */
final class TemplateWatermarkRenderer {
    private static final Typeface REGULAR=Typeface.create("sans-serif-condensed",Typeface.NORMAL);
    private static final Typeface BOLD=Typeface.create("sans-serif-condensed",Typeface.BOLD);
    private static final float BLOCK_WIDTH=326f;
    private TemplateWatermarkRenderer() {}

    static boolean drawLive(Canvas canvas,int width,int height,Date when,String address,float density,
                            WatermarkTemplate.Snapshot snapshot) {
        if (snapshot != null && "34".equals(snapshot.id) && isBasic34(snapshot)) {
            WatermarkRenderer.drawStyled34(canvas, width, height, when, address, density, snapshot);
            return true;
        }
        return draw(canvas,width,height,when,address,snapshot);
    }
    static boolean drawSaved(Canvas canvas,int width,int height,Date when,String address,
                             WatermarkTemplate.Snapshot snapshot) {
        return draw(canvas,width,height,when,address,snapshot);
    }

    private static boolean draw(Canvas canvas,int width,int height,Date when,String address,
                                WatermarkTemplate.Snapshot s) {
        if(s==null) return false;
        if("none".equals(s.id)) return true;
        if("34".equals(s.id) && isDefault34(s)) return false;
        if(width<=0 || height<=0) return true;
        Date time=when==null?new Date():when;
        List<Row> rows=rows(s,time,address);
        String title=title(s);
        boolean clock="21".equals(s.id), completed="80".equals(s.id), sheet="100".equals(s.id);
        boolean table=sheet || "110".equals(s.id) || "120".equals(s.id);
        boolean patrol="43".equals(s.id), clean="90".equals(s.id), outdoor="150".equals(s.id);
        boolean location="60".equals(s.id)||"130".equals(s.id), map="map01".equals(s.id);
        boolean delivery="dasher01".equals(s.id), report="20".equals(s.id);
        // A customized 34 uses the declarative path so every item switch is respected.
        float header=clock||completed?42:(!title.isEmpty()?34:0);
        if(outdoor) header=outdoorHeader(s);
        if("130".equals(s.id) && show(s,210)) header=72;
        if(map && show(s,210)) rows.add(new Row("Bản đồ chưa khả dụng",false));
        float rowHeight=table?25:20;
        float contentHeight=header+rows.size()*rowHeight+12;
        float scale=Math.min(width,height)/360f*s.sizeScale;
        scale=Math.min(scale,(width-16f)/BLOCK_WIDTH);
        scale=Math.min(scale,(height-16f)/Math.max(contentHeight,1f));
        if(scale<=0) return true;
        int save=canvas.save();
        canvas.translate(8f,height-8f-contentHeight*scale);
        canvas.scale(scale,scale);
        int accent=s.accentColor;
        int text=s.textColor;
        // Cover variants with dark text place it on the matching light panel.
        boolean light=Color.luminance(text)<.3;
        if(table || patrol || delivery || clean || report) {
            int panel=light?0xddfff7dc:0x99000000;
            if(patrol) panel=0xcc354d22;
            if(clean) panel=0xbb004b92;
            if(report) panel=0xeefefefe;
            rounded(canvas,0,0,BLOCK_WIDTH,contentHeight,table?0:3,panel);
        }
        float y=0;
        if(clock) {
            if(!title.isEmpty() || show(s,1)) {
                float titleWidth=title.isEmpty()?0:Math.min(210,measure(title,23,BOLD)+14);
                float total=titleWidth+(show(s,1)?96:0)+8;
                rounded(canvas,0,0,total,36,3,0xf2ffffff);
                // Thin diagonal texture is structural only; no original cover pixels are embedded.
                Paint hatch=paint(0x2254a8c5,1,REGULAR); hatch.setStrokeWidth(.6f);
                canvas.save(); canvas.clipRect(0,0,total,36);
                for(float x=-36;x<total;x+=6) canvas.drawLine(x,0,x+36,36,hatch);
                canvas.restore();
                if(titleWidth>0) { rounded(canvas,4,4,titleWidth,32,2,0xffffc233); text(canvas,title,8,27,23,Color.BLACK,true,titleWidth-9); }
                if(show(s,1)) text(canvas,time(s,time),titleWidth+7,29,31,0xff003b71,true,92);
                y=42;
            }
            line(canvas,1,y,1,contentHeight-3,0xffffc233,2);
        } else if(completed) {
            if(show(s,1)) {
                rounded(canvas,0,0,109,36,3,0xff00c93d);
                text(canvas,time(s,time),7,28,28,Color.WHITE,true,72);
                Paint p=paint(Color.WHITE,1,REGULAR); canvas.drawCircle(94,18,9,p);
                check(canvas,89,18,0xff00b939);
            }
            y=42; line(canvas,1,y,1,contentHeight-3,0xffffc233,2);
            // The Completed title is below the clock, matching the reference cover.
        } else if(outdoor) {
            float badgeX=4;
            for(int id:new int[]{6,5}) {
                String value=s.liveData.get(id);
                if(!show(s,id)||value==null||value.isEmpty()) continue;
                float w=Math.min(152,measure(value,18,BOLD)+18);
                rounded(canvas,badgeX,0,badgeX+w,28,3,id==6?0xffff641c:0x993b4148);
                text(canvas,value,badgeX+7,21,18,Color.WHITE,true,w-14); badgeX+=w+8;
            }
            float yy=badgeX>4?35:0;
            String coordinates=s.liveData.get(3);
            if(show(s,3)&&coordinates!=null&&!coordinates.isEmpty()) {
                rounded(canvas,4,yy,BLOCK_WIDTH-4,yy+27,3,0x993b4148);
                text(canvas,coordinates,11,yy+20,17,Color.WHITE,false,BLOCK_WIDTH-23); yy+=35;
            }
            y=yy;
            line(canvas,2,y+2,2,contentHeight-3,0xffff641c,2);
        } else if("130".equals(s.id) && show(s,210)) {
            rounded(canvas,0,0,BLOCK_WIDTH,64,3,0x773b4148);
            text(canvas,"Bản đồ chưa khả dụng",9,36,16,Color.WHITE,false,BLOCK_WIDTH-18); y=72;
        } else if(!title.isEmpty()) {
            if(report) {
                rounded(canvas,0,0,BLOCK_WIDTH,32,3,0xff146aff);
                text(canvas,title,9,24,23,Color.WHITE,true,BLOCK_WIDTH-18);
            } else if(table) {
                int band=sheet?0x88000000:accent;
                canvas.drawRect(0,0,BLOCK_WIDTH,31,paint(band,1,REGULAR));
                text(canvas,title,8,23,21,sheet?Color.WHITE:Color.BLACK,true,BLOCK_WIDTH-16);
            } else if(patrol) {
                shield(canvas,10,6,accent); text(canvas,title,35,25,23,accent,true,BLOCK_WIDTH-43);
                line(canvas,0,32,BLOCK_WIDTH,32,accent,1);
            } else if(clean) {
                check(canvas,10,18,Color.WHITE); text(canvas,title,29,25,24,Color.WHITE,true,BLOCK_WIDTH-35);
            } else {
                text(canvas,title,4,25,23,text,true,BLOCK_WIDTH-8);
            }
            y=34;
        }
        if((location||map) && show(s,6) && s.liveData.containsKey(6)) {
            rounded(canvas,4,y,BLOCK_WIDTH-4,y+21,2,0xbbee641e);
        }
        if(!table && !clock && !completed && !patrol && !clean && !outdoor)
            line(canvas,1,y+2,1,contentHeight-3,accent,2);
        for(Row row:rows) {
            if(table) {
                line(canvas,0,y,BLOCK_WIDTH,y,0x99ffffff,1);
                line(canvas,0,y,0,y+rowHeight,0x99ffffff,1);
                line(canvas,BLOCK_WIDTH,y,BLOCK_WIDTH,y+rowHeight,0x99ffffff,1);
            }
            text(canvas,row.text,table?8:9,y+(table?18:15),row.heading?16:15,
                    (patrol?0xffffec55:(clean?Color.WHITE:text)),row.heading,BLOCK_WIDTH-18);
            y+=rowHeight;
        }
        if(table) line(canvas,0,y,BLOCK_WIDTH,y,0x99ffffff,1);
        canvas.restoreToCount(save);
        return true;
    }

    private static float outdoorHeader(WatermarkTemplate.Snapshot s) {
        float height=0;
        if((show(s,6)&&s.liveData.containsKey(6))||(show(s,5)&&s.liveData.containsKey(5))) height+=35;
        if(show(s,3)&&s.liveData.containsKey(3)) height+=35;
        return height;
    }
    private static boolean isDefault34(WatermarkTemplate.Snapshot s) {
        if(s.sizeScale!=1f || s.accentColor!=0xffffc233 || s.textColor!=Color.WHITE) return false;
        if(!show(s,1) || !show(s,2)) return false;
        for(WatermarkTemplate.Field f:s.fields.values()) if(f.enabled && f.id!=1 && f.id!=2) return false;
        return true;
    }
    static boolean isBasic34(WatermarkTemplate.Snapshot s) {
        if (!show(s,1)) return false;
        for (WatermarkTemplate.Field field : s.fields.values())
            if (field.enabled && field.id != 1 && field.id != 2) return false;
        return true;
    }
    private static String title(WatermarkTemplate.Snapshot s) {
        int id="service01".equals(s.id)?11:("10".equals(s.id)?11:13);
        WatermarkTemplate.Field f=s.field(id);
        if(f==null || !f.enabled) return "";
        String title=f.content.trim();
        // Empty patrol title is resolved by the original runtime's localized resources.
        if(title.isEmpty() && "43".equals(s.id)) return "Tuần tra";
        return title;
    }
    private static List<Row> rows(WatermarkTemplate.Snapshot s,Date when,String address) {
        List<Row> result=new ArrayList<>();
        int titleId="service01".equals(s.id)||"10".equals(s.id)?11:13;
        if("80".equals(s.id) && show(s,13)) addWrapped(result,s.field(13).content,true);
        for(WatermarkTemplate.Field f:s.fields.values()) {
            if(!f.enabled || f.id==titleId || f.id==210) continue;
            if("150".equals(s.id) && (f.id==3||f.id==5||f.id==6)) continue;
            if(f.id==1 && ("21".equals(s.id)||"80".equals(s.id)||"150".equals(s.id))) {
                if("21".equals(s.id)) addWrapped(result,WatermarkRenderer.weekday(when)+", "+WatermarkRenderer.date(when),true);
                else if("150".equals(s.id)) addWrapped(result,WatermarkRenderer.date(when)+"  "+time(s,when),false);
                else addWrapped(result,WatermarkRenderer.date(when),false);
                continue;
            }
            String value;
            switch(f.id) {
                case 1: value=WatermarkRenderer.date(when)+"  "+time(s,when); break;
                case 2: value=address==null||address.trim().isEmpty()?"Vị trí chưa khả dụng":address; break;
                case 3: case 4: case 5: case 6:
                    value=s.liveData.get(f.id); if(value==null||value.isEmpty()) continue; break;
                case 640:
                    value=s.liveData.get(640); if(value==null||value.isEmpty()) continue;
                    value="Mã cục bộ: "+value; break;
                case 620:
                    // Business cards may contain image URIs/nested JSON; never print that JSON as text.
                    value=businessCard(f.content); break;
                default:
                    value=f.content;
                    if(value.isEmpty()) continue;
                    if(f.id!=88 && f.id!=630 && f.id!=1301 && f.id<1000000000)
                        value=f.title+": "+value;
            }
            addWrapped(result,value,false);
        }
        return result;
    }
    private static String businessCard(String content) {
        try {
            org.json.JSONArray fields=new org.json.JSONArray(content); StringBuilder text=new StringBuilder();
            for(int i=0;i<fields.length();i++) {
                org.json.JSONObject f=fields.getJSONObject(i); int id=f.optInt("id");
                if(f.optBoolean("switchStatus") && (id==60002||id==60003)) {
                    if(text.length()>0) text.append(" · "); text.append(f.optString("content"));
                }
            }
            return text.toString();
        } catch(org.json.JSONException ignored) { return content.startsWith("[")?"":content; }
    }
    private static void addWrapped(List<Row> result,String text,boolean heading) {
        if(text==null||text.trim().isEmpty()) return;
        Paint p=paint(Color.WHITE,heading?16:15,heading?BOLD:REGULAR);
        for(String paragraph:text.split("\\n")) {
            String rest=paragraph.trim();
            while(!rest.isEmpty()) {
                int end=p.breakText(rest,true,BLOCK_WIDTH-20,null);
                if(end<=0) end=Character.charCount(rest.codePointAt(0));
                if(end<rest.length()) {
                    int space=rest.lastIndexOf(' ',end); if(space>0) end=space;
                    if(end>0 && Character.isHighSurrogate(rest.charAt(end-1))) end--;
                }
                if(end<=0) end=Character.charCount(rest.codePointAt(0));
                result.add(new Row(rest.substring(0,end),heading)); rest=rest.substring(end).trim();
            }
        }
    }
    private static boolean show(WatermarkTemplate.Snapshot s,int id) { return s.field(id)!=null && s.field(id).enabled; }
    private static String time(WatermarkTemplate.Snapshot s,Date when) {
        WatermarkTemplate.Field f=s.field(1);
        return new SimpleDateFormat(f!=null&&f.hourFormat==1?"hh:mm a":"HH:mm",Locale.getDefault()).format(when);
    }
    private static float measure(String text,float size,Typeface face) {return paint(Color.WHITE,size,face).measureText(text);}
    private static Paint paint(int color,float size,Typeface face) {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG); p.setColor(color);p.setTextSize(size);p.setTypeface(face);return p;
    }
    private static void text(Canvas c,String value,float x,float baseline,float size,int color,boolean bold,float maxWidth) {
        Paint p=paint(color,size,bold?BOLD:REGULAR);
        float measured=p.measureText(value); if(measured>maxWidth) p.setTextSize(size*maxWidth/measured);
        if(Color.luminance(color)>.5) p.setShadowLayer(1.2f,0,.6f,0x77000000);
        c.drawText(value,x,baseline,p);
    }
    private static void rounded(Canvas c,float l,float t,float r,float b,float radius,int color) {c.drawRoundRect(new RectF(l,t,r,b),radius,radius,paint(color,1,REGULAR));}
    private static void line(Canvas c,float x,float y,float xx,float yy,int color,float stroke) {Paint p=paint(color,1,REGULAR);p.setStrokeWidth(stroke);c.drawLine(x,y,xx,yy,p);}
    private static void check(Canvas c,float x,float y,int color) {line(c,x,y,x+4,y+4,color,2);line(c,x+4,y+4,x+11,y-5,color,2);}
    private static void shield(Canvas c,float x,float y,int color) {
        Path path=new Path();path.moveTo(x,y+3);path.lineTo(x+9,y);path.lineTo(x+18,y+3);path.lineTo(x+16,y+16);path.lineTo(x+9,y+22);path.lineTo(x+2,y+16);path.close();
        Paint p=paint(color,1,REGULAR);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawPath(path,p);check(c,x+4,y+10,color);
    }
    private static final class Row {final String text;final boolean heading;Row(String text,boolean heading){this.text=text;this.heading=heading;}}
}
