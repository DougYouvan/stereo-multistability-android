package ai.youvan.stereomultistability;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class SessionStore {
    private final Context context;
    private File current;

    public SessionStore(Context context){ this.context=context; }

    public File begin(String prefix,String header) throws IOException {
        File dir=sessionsDir();
        String ts=new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US).format(new Date());
        current=new File(dir,prefix+"-"+ts+"-"+UUID.randomUUID()+".csv");
        try(FileOutputStream fos=new FileOutputStream(current,false)) {
            fos.write((header+"\n").getBytes(StandardCharsets.UTF_8));
            fos.getFD().sync();
        }
        rememberLatest(current);
        return current;
    }

    public void append(String row) throws IOException {
        if(current==null) throw new IOException("No session started");
        try(FileOutputStream fos=new FileOutputStream(current,true)) {
            fos.write((row+"\n").getBytes(StandardCharsets.UTF_8));
            fos.getFD().sync();
        }
        current.setLastModified(System.currentTimeMillis());
    }

    public File current(){ return current; }

    public File latest() {
        String p=context.getSharedPreferences("sma",Context.MODE_PRIVATE).getString("latest_session",null);
        return p==null?null:new File(p);
    }

    public List<File> listSessions() throws IOException {
        File[] files=sessionsDir().listFiles((dir,name)->name.toLowerCase(Locale.US).endsWith(".csv"));
        ArrayList<File> out=new ArrayList<>();
        if(files!=null) Collections.addAll(out,files);
        out.sort((a,b)->Long.compare(b.lastModified(),a.lastModified()));
        return Collections.unmodifiableList(out);
    }

    public File createAllSessionsZip() throws IOException {
        List<File> files=listSessions();
        if(files.isEmpty()) throw new IOException("No saved sessions");
        File out=new File(context.getCacheDir(),"stereo-multistability-sessions.zip");
        try(ZipOutputStream zos=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(out,false)))) {
            byte[] buf=new byte[8192];
            for(File f:files) {
                ZipEntry entry=new ZipEntry(f.getName());
                entry.setTime(0L);
                zos.putNextEntry(entry);
                try(InputStream in=new BufferedInputStream(new FileInputStream(f))) {
                    int n; while((n=in.read(buf))!=-1) zos.write(buf,0,n);
                }
                zos.closeEntry();
            }
        }
        return out;
    }

    private File sessionsDir() throws IOException {
        File dir=new File(context.getFilesDir(),"sessions");
        if(!dir.exists() && !dir.mkdirs()) throw new IOException("Cannot create sessions directory");
        return dir;
    }

    private void rememberLatest(File f) {
        context.getSharedPreferences("sma",Context.MODE_PRIVATE).edit().putString("latest_session",f.getAbsolutePath()).apply();
    }

    public static String csv(String s) {
        if(s==null) s="";
        return "\""+s.replace("\"","\"\"").replace("\r"," ").replace("\n"," ")+"\"";
    }

    public static String csvRow(String... fields) {
        StringBuilder b=new StringBuilder();
        for(int i=0;i<fields.length;i++) {
            if(i>0) b.append(',');
            b.append(csv(fields[i]));
        }
        return b.toString();
    }
}
