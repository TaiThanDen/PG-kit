package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Color;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Declarative catalogue; each captured frame owns an immutable set of options and sensor values. */
final class WatermarkTemplate {
    final String id, baseId, name;
    final List<Field> items;
    private final int accentColor, textColor;
    private final float sizeScale;

    static final class Field {
        final int id, style, editType, showArea, hourFormat, tempUnits;
        final String title, content;
        final boolean enabled, userCustom;
        Field(JSONObject value) {
            id=value.optInt("id"); style=value.optInt("style"); editType=value.optInt("editType");
            showArea=value.optInt("showArea"); hourFormat=value.optInt("hourFormat"); tempUnits=value.optInt("tempUnits");
            title=value.optString("title"); content=value.optString("content");
            enabled=value.optBoolean("switchStatus"); userCustom=value.optBoolean("userCustom");
        }
        private Field(Field f, boolean show, String text) {
            id=f.id; style=f.style; editType=f.editType; showArea=f.showArea;
            hourFormat=f.hourFormat; tempUnits=f.tempUnits; title=f.title;
            content=text==null?"":text; enabled=show; userCustom=f.userCustom;
        }
        boolean isEditable() { return id!=1 && id!=2 && id!=3 && id!=4 && id!=5 && id!=6 && id!=210 && id!=640; }
    }

    private WatermarkTemplate(JSONObject value) {
        id=value.optString("id"); baseId=value.optString("baseId",id); name=value.optString("name",id);
        ArrayList<Field> parsed=new ArrayList<>(); JSONArray a=value.optJSONArray("items");
        if(a!=null) for(int i=0;i<a.length();i++) parsed.add(new Field(a.optJSONObject(i)));
        items=Collections.unmodifiableList(parsed);
        JSONObject theme=value.optJSONObject("theme");
        accentColor=parseColor(theme==null?"":theme.optString("color"),0xffffc233);
        textColor=parseColor(theme==null?"":theme.optString("textColor"),Color.WHITE);
        sizeScale=theme==null?1f:(float)theme.optDouble("sizeScale",1);
    }

    static List<WatermarkTemplate> defaults(Context context) {
        try(InputStream stream=context.getAssets().open("watermarks/templates.json")) {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream(); byte[] buffer=new byte[4096]; int n;
            while((n=stream.read(buffer))!=-1) bytes.write(buffer,0,n);
            JSONArray array=new JSONArray(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
            ArrayList<WatermarkTemplate> templates=new ArrayList<>();
            for(int i=0;i<array.length();i++) templates.add(new WatermarkTemplate(array.getJSONObject(i)));
            return Collections.unmodifiableList(templates);
        } catch(Exception error) { throw new IllegalStateException("Cannot load watermark catalogue",error); }
    }

    Snapshot defaultSnapshot() {
        LinkedHashMap<Integer,Field> fields=new LinkedHashMap<>();
        for(Field f:items) {
            Field value = f;
            if ("21".equals(id) && f.id == 13 && "CLOCK IN".equals(f.content))
                value = new Field(f, f.enabled, "Điểm danh");
            if ("80".equals(id) && f.id == 13 && "Completed".equals(f.content))
                value = new Field(f, f.enabled, "Đã hoàn thành");
            fields.put(f.id,value);
        }
        return new Snapshot(id,fields,accentColor,textColor,sizeScale,Collections.emptyMap());
    }
    static Snapshot noneSnapshot() {
        return new Snapshot("none", Collections.emptyMap(), Color.WHITE, Color.WHITE, 1f, Collections.emptyMap());
    }
    static Snapshot fromJson(Context context,String source) {
        List<WatermarkTemplate> templates=defaults(context);
        Snapshot fallback=null; for(WatermarkTemplate t:templates) if(t.id.equals("34")) fallback=t.defaultSnapshot();
        try {
            JSONObject data=new JSONObject(source);
            if("none".equals(data.optString("id"))) return noneSnapshot();
            Snapshot result=fallback;
            for(WatermarkTemplate t:templates) if(t.id.equals(data.optString("id"))) result=t.defaultSnapshot();
            JSONArray a=data.optJSONArray("fields");
            if(a!=null) for(int i=0;i<a.length();i++) { JSONObject f=a.getJSONObject(i);
                result=result.withField(f.getInt("id"),f.optBoolean("enabled"),f.optString("content")); }
            return result.withScale((float)data.optDouble("sizeScale",1))
                    .withTheme(data.optInt("accentColor",result.accentColor),data.optInt("textColor",result.textColor));
        } catch(Exception ignored) { return fallback; }
    }
    private static int parseColor(String text,int fallback) {
        try {return Color.parseColor(text);} catch(IllegalArgumentException ignored) {return fallback;}
    }

    static final class Snapshot {
        final String id;
        final Map<Integer,Field> fields;
        final int accentColor,textColor;
        final float sizeScale;
        final Map<Integer,String> liveData;
        private Snapshot(String id,Map<Integer,Field> fields,int accent,int text,float scale,Map<Integer,String> data) {
            this.id=id; this.fields=Collections.unmodifiableMap(new LinkedHashMap<>(fields));
            accentColor=accent; textColor=text; sizeScale=Float.isNaN(scale)?1:Math.max(.5f,Math.min(2f,scale));
            liveData=Collections.unmodifiableMap(new LinkedHashMap<>(data));
        }
        Field field(int id) { return fields.get(id); }
        Snapshot withField(int itemId,boolean enabled,String content) {
            Field f=fields.get(itemId); if(f==null) return this;
            Map<Integer,Field> copy=new LinkedHashMap<>(fields); copy.put(itemId,new Field(f,enabled,content));
            return new Snapshot(id,copy,accentColor,textColor,sizeScale,liveData);
        }
        Snapshot withScale(float scale) {return new Snapshot(id,fields,accentColor,textColor,scale,liveData);}
        Snapshot withTheme(int accent,int text) {return new Snapshot(id,fields,accent,text,sizeScale,liveData);}
        Snapshot withLiveData(Map<Integer,String> data) {return new Snapshot(id,fields,accentColor,textColor,sizeScale,data==null?Collections.emptyMap():data);}
        Snapshot withSensorData(Double latitude,Double longitude,Double altitude,Double compass) {
            Map<Integer,String> data=new LinkedHashMap<>(liveData);
            data.remove(3); data.remove(5); data.remove(6);
            if(latitude!=null && longitude!=null) data.put(3,String.format(Locale.US,"%.6f°, %.6f°",latitude,longitude));
            if(altitude!=null) data.put(5,String.format(Locale.getDefault(),"%.1f m",altitude));
            if(compass!=null) data.put(6,String.format(Locale.getDefault(),"%.0f°",compass));
            return withLiveData(data);
        }
        String toJson() {
            JSONObject data=new JSONObject(); JSONArray array=new JSONArray();
            try {
                data.put("id",id).put("accentColor",accentColor).put("textColor",textColor).put("sizeScale",sizeScale);
                for(Field f:fields.values()) array.put(new JSONObject().put("id",f.id).put("enabled",f.enabled).put("content",f.content));
                data.put("fields",array);
            } catch(JSONException ignored) { /* All values above have fixed primitive types. */ }
            return data.toString();
        }
    }
}
