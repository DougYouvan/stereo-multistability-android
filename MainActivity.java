package ai.youvan.stereomultistability;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.window.OnBackInvokedDispatcher;
import java.io.*;
import java.util.*;

public final class MainActivity extends Activity {
    private static final String PARENT_MANIFEST_SHA256="51cf147a8c8cf8016795cfcccefa63cdb058612bb3fcb8f5166eface8ebaec52";

    private static final int MODE_HOME=0;
    private static final int MODE_ATLAS=1;
    private static final int MODE_EXPERIMENT_STIMULUS=2;
    private static final int MODE_OSCILLATION=3;
    private static final int MODE_CONTINUOUS=4;
    private static final int MODE_EXPERIMENT_FREE=5;
    private static final int MODE_EXPERIMENT_STRUCTURED=6;
    private static final int MODE_SETUP=7;

    private static final int REQ_EXPORT=500;

    private final List<StimulusSpec> catalog=StimulusCatalog.all();
    private SessionStore store;
    private int atlasIndex=0;
    private StereoView stereoView;
    private boolean crossed=false;
    private int mode=MODE_HOME;
    private File pendingExportFile;

    private TrialPlanner.Plan experimentPlan;
    private int experimentIndex=0;
    private long stimulusStartMs;
    private boolean stimulusClockReady=false;
    private String participantCode="";
    private double viewingDistanceMm=Double.NaN;
    private double screenPxPerMm=Double.NaN;
    private long experimentSeed=0L;
    private String experimentSessionId="";
    private boolean experimentActive=false;

    private long oscillationStartMs;
    private boolean oscillationClockReady=false;
    private String oscillationSessionId="";
    private StimulusSpec oscillationSpec;
    private int oscillationEventIndex=0;
    private boolean oscillationActive=false;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        store=new SessionStore(this);
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        loadCalibration();
        registerPredictiveBack();
        showHome();
    }

    private void registerPredictiveBack() {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                this::handleBackNavigation
            );
        }
    }

    @SuppressWarnings("deprecation")
    @Override public void onBackPressed() {
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.TIRAMISU) handleBackNavigation();
        else super.onBackPressed();
    }

    private void handleBackNavigation() {
        if(experimentActive && (mode==MODE_EXPERIMENT_STIMULUS || mode==MODE_EXPERIMENT_FREE || mode==MODE_EXPERIMENT_STRUCTURED)) {
            new AlertDialog.Builder(this)
                .setTitle("Abort experiment?")
                .setMessage("Completed trials are already checkpointed. Aborting will add an explicit aborted-status record.")
                .setNegativeButton("Continue experiment",null)
                .setPositiveButton("Abort",(d,w)->{ finalizeExperiment("aborted"); showHome(); })
                .show();
            return;
        }
        if(oscillationActive && mode==MODE_OSCILLATION) {
            finalizeOscillation("completed");
            toast("Oscillation run saved.");
            showHome();
            return;
        }
        if(mode!=MODE_HOME) { showHome(); return; }
        finish();
    }

    private TextView text(String s,int sp) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(Color.BLACK); t.setPadding(12,8,12,8); return t;
    }
    private Button button(String s) { Button b=new Button(this); b.setText(s); return b; }

    private void showHome() {
        mode=MODE_HOME; showSystemUi(); keepScreenAwake(false);
        ScrollView scroll=new ScrollView(this);
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(40,24,40,24); box.setGravity(Gravity.CENTER_HORIZONTAL); box.setBackgroundColor(Color.WHITE);
        TextView title=text("Stereo Multistability Android",28); title.setGravity(Gravity.CENTER); box.addView(title,new LinearLayout.LayoutParams(-1,-2));
        TextView sub=text("Landscape free-fusion research app — no headset or viewer required",17); sub.setGravity(Gravity.CENTER); box.addView(sub);
        TextView count=text("289 mathematically generated stereo pairs. Experimental stimulus screens contain only the pair.",15); count.setGravity(Gravity.CENTER); box.addView(count);

        Button atlas=button("Atlas — browse all 289 pairs"); atlas.setOnClickListener(v->startAtlas()); box.addView(atlas,wide());
        Button exp=button("Blind experiment — 40 trials / 8 hidden repeats"); exp.setOnClickListener(v->showExperimentSetup()); box.addView(exp,wide());
        Button osc=button("Oscillation logger — cube / top / front transitions"); osc.setOnClickListener(v->showOscillationSetup()); box.addView(osc,wide());
        Button cont=button("Continuous disparity explorer"); cont.setOnClickListener(v->showContinuous()); box.addView(cont,wide());
        Button cal=button("Physical screen calibration"); cal.setOnClickListener(v->showCalibration()); box.addView(cal,wide());
        Button export=button("Export a saved session CSV"); export.setOnClickListener(v->showSavedSessions()); box.addView(export,wide());
        Button exportAll=button("Export all saved sessions as ZIP"); exportAll.setOnClickListener(v->exportAllSessions()); box.addView(exportAll,wide());

        TextView privacy=text("Research data stay in private app storage and Android Auto Backup is disabled. Export is user-initiated.",13); privacy.setGravity(Gravity.CENTER); box.addView(privacy);
        TextView repo=text("Parent atlas: github.com/DougYouvan/stereo-multistability-atlas",13); repo.setGravity(Gravity.CENTER); box.addView(repo);
        scroll.addView(box); setContentView(scroll);
    }

    private LinearLayout.LayoutParams wide(){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(Math.min(getResources().getDisplayMetrics().widthPixels-120,900),-2); p.setMargins(0,7,0,7); return p; }

    private boolean isLandscapeNow() {
        android.util.DisplayMetrics dm=getResources().getDisplayMetrics();
        return dm.widthPixels>dm.heightPixels;
    }

    private void showLandscapeRequired(Runnable retry) {
        mode=MODE_SETUP; showSystemUi(); keepScreenAwake(false);
        AlertDialog.Builder b=new AlertDialog.Builder(this)
            .setTitle("Landscape required")
            .setMessage("Rotate the device so the screen is wider than it is tall before continuing. Android 16 may ignore app orientation locks on some tablets and unfolded foldables, so the experiment verifies orientation at runtime.")
            .setCancelable(false)
            .setPositiveButton("Retry",(d,w)->{
                if(isLandscapeNow()) retry.run();
                else showLandscapeRequired(retry);
            });
        if(experimentActive || oscillationActive) {
            b.setNegativeButton("Abort session",(d,w)->{
                if(experimentActive) finalizeExperiment("aborted");
                if(oscillationActive) finalizeOscillation("aborted");
                showHome();
            });
        } else {
            b.setNegativeButton("Home",(d,w)->showHome());
        }
        b.show();
    }

    private void startAtlas() {
        mode=MODE_SETUP; showSystemUi(); keepScreenAwake(false);
        new AlertDialog.Builder(this).setTitle("Atlas controls")
            .setMessage("Hold the phone sideways. The stimulus screen contains only the stereo pair. Use VOLUME UP for next, VOLUME DOWN for previous, and Android Back to leave the atlas.")
            .setPositiveButton("Start",(d,w)->showAtlasStimulus()).setNegativeButton("Cancel",(d,w)->showHome()).show();
    }

    private void showAtlasStimulus(){
        if(!isLandscapeNow()){ showLandscapeRequired(this::showAtlasStimulus); return; }
        mode=MODE_ATLAS; hideSystemUi(); keepScreenAwake(true);
        stereoView=new StereoView(this); stereoView.setStimulus(catalog.get(atlasIndex),crossed); setContentView(stereoView);
    }

    @Override public boolean onKeyDown(int keyCode,KeyEvent event) {
        if(mode==MODE_ATLAS) {
            if(keyCode==KeyEvent.KEYCODE_VOLUME_UP){ atlasIndex=(atlasIndex+1)%catalog.size(); stereoView.setStimulus(catalog.get(atlasIndex),crossed); return true; }
            if(keyCode==KeyEvent.KEYCODE_VOLUME_DOWN){ atlasIndex=(atlasIndex-1+catalog.size())%catalog.size(); stereoView.setStimulus(catalog.get(atlasIndex),crossed); return true; }
        }
        return super.onKeyDown(keyCode,event);
    }

    private void showExperimentSetup() {
        mode=MODE_SETUP; showSystemUi(); keepScreenAwake(false);
        LinearLayout box=formBox();
        box.addView(text("Blind experiment setup",24));
        EditText participant=new EditText(this); participant.setHint("Participant code (optional)"); box.addView(participant,wide());
        EditText distance=new EditText(this); distance.setHint("Viewing distance in mm (optional; e.g. 400)"); distance.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); box.addView(distance,wide());
        RadioGroup fusion=new RadioGroup(this); fusion.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton par=new RadioButton(this); par.setText("Parallel / wall-eyed"); par.setId(1001); par.setChecked(true);
        RadioButton cro=new RadioButton(this); cro.setText("Crossed"); cro.setId(1002); fusion.addView(par); fusion.addView(cro); box.addView(fusion);
        TextView cal=text(Double.isNaN(screenPxPerMm)?"No physical calibration stored. Pixel-space data will still be recorded.":String.format(Locale.US,"Calibration: %.3f screen px/mm",screenPxPerMm),14); box.addView(cal);
        Button start=button("Start 40-trial blind session");
        start.setOnClickListener(v->{
            if(!isLandscapeNow()){ showLandscapeRequired(this::showExperimentSetup); return; }
            participantCode=participant.getText().toString().trim();
            viewingDistanceMm=parseDouble(distance.getText().toString());
            crossed=fusion.getCheckedRadioButtonId()==1002;
            experimentPlan=TrialPlanner.standardPlan(); experimentSeed=experimentPlan.seed; experimentIndex=0; experimentSessionId=UUID.randomUUID().toString();
            try {
                store.begin("experiment","record_type,session_id,parent_manifest_sha256,session_status,planned_trials,completed_trials,seed,participant,trial_index,stimulus_id,repeat_instance,fusion_mode,time_to_report_ms,free_report,fusion,geometry,depth,divider,distortion,temporal,confidence,screen_width_px,screen_height_px,screen_px_per_mm,viewing_distance_mm,display_scale,rms_disparity_screen_px,max_disparity_screen_px,rms_disparity_arcmin,max_disparity_arcmin");
                experimentActive=true;
                store.append(experimentStatusRow("running"));
            } catch(IOException e){ error(e); return; }
            showExperimentStimulus();
        }); box.addView(start,wide());
        setContentView(wrap(box));
    }

    private void showExperimentStimulus() {
        if(!isLandscapeNow()){ showLandscapeRequired(this::showExperimentStimulus); return; }
        mode=MODE_EXPERIMENT_STIMULUS; hideSystemUi(); keepScreenAwake(true);
        TrialPlanner.Trial trial=experimentPlan.trials.get(experimentIndex);
        stereoView=new StereoView(this); stereoView.setStimulus(trial.spec,crossed);
        stimulusClockReady=false;
        stereoView.setOnClickListener(v->{
            if(!stimulusClockReady) return;
            long elapsed=SystemClock.elapsedRealtime()-stimulusStartMs;
            stimulusClockReady=false;
            showFreeReport(trial,elapsed);
        });
        setContentView(stereoView);
        stereoView.post(()->{ stimulusStartMs=SystemClock.elapsedRealtime(); stimulusClockReady=true; });
    }

    private void showFreeReport(TrialPlanner.Trial trial,long elapsedMs) {
        mode=MODE_EXPERIMENT_FREE; showSystemUi(); keepScreenAwake(false);
        LinearLayout box=formBox();
        box.addView(text("Free report — stimulus is hidden",22));
        box.addView(text("Describe what you saw before any experimenter categories are shown.",15));
        EditText free=new EditText(this); free.setHint("What did you see?"); free.setMinLines(3); box.addView(free,wide());
        Button next=button("Continue to structured scoring");
        next.setOnClickListener(v->{
            String report=free.getText().toString().trim();
            if(report.isEmpty()){ toast("Enter a short free report before continuing."); return; }
            showStructuredResponse(trial,elapsedMs,report);
        });
        box.addView(next,wide());
        setContentView(wrap(box));
    }

    private void showStructuredResponse(TrialPlanner.Trial trial,long elapsedMs,String freeReport) {
        mode=MODE_EXPERIMENT_STRUCTURED; showSystemUi(); keepScreenAwake(false);
        LinearLayout box=formBox(); box.addView(text("Structured scoring",22));

        RadioGroup fusion=radioRow("Fusion",new String[]{"Fused","Partial","Unable to fuse"},2000,box);
        TextView gt=text("Geometry (select all that occurred)",15); box.addView(gt);
        LinearLayout geom=new LinearLayout(this); geom.setOrientation(LinearLayout.HORIZONTAL);
        CheckBox cube=check("Cube"), top=check("Top stack"), front=check("Front stack"), other=check("Other"), uncertain=check("Uncertain");
        for(CheckBox c:new CheckBox[]{cube,top,front,other,uncertain}) geom.addView(c); box.addView(geom);
        uncertain.setOnCheckedChangeListener((b,on)->{ if(on){cube.setChecked(false);top.setChecked(false);front.setChecked(false);other.setChecked(false);} });
        for(CheckBox c:new CheckBox[]{cube,top,front,other}) c.setOnCheckedChangeListener((b,on)->{ if(on) uncertain.setChecked(false); });

        RadioGroup depth=radioRow("Depth orientation",new String[]{"Normal","Inverted","Uncertain"},3000,box);
        RadioGroup divider=radioRow("Divider",new String[]{"Present","Absent","Intermittent"},4000,box);
        RadioGroup distortion=radioRow("Distortion",new String[]{"None","Kink / notch","Other"},5000,box);
        RadioGroup temporal=radioRow("Temporal behavior",new String[]{"Stable","Alternated","Continuously unstable"},6000,box);
        TextView confText=text("Confidence: 3 / 5",15); box.addView(confText);
        SeekBar conf=new SeekBar(this); conf.setMax(4); conf.setProgress(2); conf.setOnSeekBarChangeListener(new SimpleSeek(){@Override public void onProgressChanged(SeekBar b,int p,boolean u){confText.setText("Confidence: "+(p+1)+" / 5");}}); box.addView(conf,wide());
        Button submit=button("Submit and continue");
        submit.setOnClickListener(v->{
            int fi=fusion.getCheckedRadioButtonId();
            if(fi==-1){ toast("Select fusion status."); return; }
            boolean unable=fi==2002;
            if(!unable) {
                if(!(cube.isChecked()||top.isChecked()||front.isChecked()||other.isChecked()||uncertain.isChecked())) { toast("Select at least one geometry response."); return; }
                if(depth.getCheckedRadioButtonId()==-1||divider.getCheckedRadioButtonId()==-1||distortion.getCheckedRadioButtonId()==-1||temporal.getCheckedRadioButtonId()==-1){ toast("Complete the structured response fields."); return; }
            }
            String geometry=unable?"not_applicable":joinChecked(cube,top,front,other,uncertain);
            String dep=unable?"not_applicable":radioValue(depth);
            String div=unable?"not_applicable":radioValue(divider);
            String dis=unable?"not_applicable":radioValue(distortion);
            String tem=unable?"not_applicable":radioValue(temporal);
            String fus=radioValue(fusion);
            double displayScale=stereoView==null?Double.NaN:stereoView.canonicalToScreenScale();
            double rms=StereoMath.rmsRenderedDisparity(trial.spec)*displayScale;
            double mx=StereoMath.maxAbsRenderedDisparity(trial.spec)*displayScale;
            double rmsArc=arcmin(rms), maxArc=arcmin(mx);
            String row=experimentTrialRow(trial,elapsedMs,freeReport,fus,geometry,dep,div,dis,tem,conf.getProgress()+1,displayScale,rms,mx,rmsArc,maxArc);
            try{store.append(row);}catch(IOException e){error(e);return;}
            experimentIndex++;
            if(experimentIndex>=experimentPlan.trials.size()) {
                finalizeExperiment("completed");
                new AlertDialog.Builder(this).setTitle("Session complete").setMessage("40 trials completed and checkpointed to private app storage. You can export this or any earlier saved session from the home screen.").setPositiveButton("Home",(d,w)->showHome()).setCancelable(false).show();
            } else showExperimentStimulus();
        }); box.addView(submit,wide());
        setContentView(wrap(box));
    }

    private String experimentTrialRow(TrialPlanner.Trial trial,long elapsedMs,String freeReport,String fus,String geometry,String dep,String div,String dis,String tem,int confidence,double displayScale,double rms,double mx,double rmsArc,double maxArc) {
        String[] f=new String[30]; Arrays.fill(f,"");
        f[0]="trial"; f[1]=experimentSessionId; f[2]=PARENT_MANIFEST_SHA256; f[3]="running";
        f[4]=Integer.toString(experimentPlan.trials.size()); f[5]=Integer.toString(experimentIndex+1); f[6]=Long.toString(experimentSeed); f[7]=participantCode;
        f[8]=Integer.toString(experimentIndex+1); f[9]=trial.spec.stimulusId; f[10]=Integer.toString(trial.repeatInstance); f[11]=crossed?"crossed":"parallel";
        f[12]=Long.toString(elapsedMs); f[13]=freeReport; f[14]=fus; f[15]=geometry; f[16]=dep; f[17]=div; f[18]=dis; f[19]=tem; f[20]=Integer.toString(confidence);
        f[21]=Integer.toString(getResources().getDisplayMetrics().widthPixels); f[22]=Integer.toString(getResources().getDisplayMetrics().heightPixels);
        f[23]=num(screenPxPerMm); f[24]=num(viewingDistanceMm); f[25]=num(displayScale); f[26]=num(rms); f[27]=num(mx); f[28]=num(rmsArc); f[29]=num(maxArc);
        return SessionStore.csvRow(f);
    }

    private String experimentStatusRow(String status) {
        int planned=experimentPlan==null?40:experimentPlan.trials.size();
        String[] f=new String[30]; Arrays.fill(f,"");
        f[0]="status"; f[1]=experimentSessionId; f[2]=PARENT_MANIFEST_SHA256; f[3]=status; f[4]=Integer.toString(planned); f[5]=Integer.toString(experimentIndex);
        f[6]=Long.toString(experimentSeed); f[7]=participantCode; f[11]=crossed?"crossed":"parallel";
        f[21]=Integer.toString(getResources().getDisplayMetrics().widthPixels); f[22]=Integer.toString(getResources().getDisplayMetrics().heightPixels); f[23]=num(screenPxPerMm); f[24]=num(viewingDistanceMm);
        return SessionStore.csvRow(f);
    }

    private void finalizeExperiment(String status) {
        if(!experimentActive) return;
        try { store.append(experimentStatusRow(status)); }
        catch(IOException e) { error(e); }
        experimentActive=false;
        stimulusClockReady=false;
    }

    private void showOscillationSetup() {
        mode=MODE_SETUP; showSystemUi(); keepScreenAwake(false); LinearLayout box=formBox(); box.addView(text("Oscillation logger setup",24));
        EditText sid=new EditText(this); sid.setHint("Stimulus number 1–289 (e.g. 145)"); sid.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); box.addView(sid,wide());
        EditText participant=new EditText(this); participant.setHint("Participant code (optional)"); box.addView(participant,wide());
        RadioGroup fusion=radioRow("Fusion",new String[]{"Parallel / wall-eyed","Crossed"},7000,box); fusion.check(7000);
        TextView instructions=text("During viewing the screen contains only the pair. Tap the LEFT third when CUBE dominates, CENTER third for TOP STACK, RIGHT third for FRONT STACK. Long-press anywhere for OTHER. Each change is timestamped. Back ends and saves the run.",15); box.addView(instructions);
        Button start=button("Start oscillation run"); start.setOnClickListener(v->{
            if(!isLandscapeNow()){ showLandscapeRequired(this::showOscillationSetup); return; }
            int n=parseInt(sid.getText().toString(),-1); if(n<1||n>289){toast("Enter a stimulus number from 1 to 289.");return;}
            participantCode=participant.getText().toString().trim(); crossed=fusion.getCheckedRadioButtonId()==7001; oscillationSpec=catalog.get(n-1); oscillationSessionId=UUID.randomUUID().toString(); oscillationEventIndex=0;
            try{
                store.begin("oscillation","record_type,oscillation_session_id,parent_manifest_sha256,session_status,participant,stimulus_id,fusion_mode,event_index,elapsed_ms,state");
                oscillationActive=true;
                store.append(oscillationStatusRow("running",0L));
            }catch(IOException e){error(e);return;}
            showOscillation();
        }); box.addView(start,wide()); setContentView(wrap(box));
    }

    private void showOscillation(){
        if(!isLandscapeNow()){ showLandscapeRequired(this::showOscillation); return; }
        mode=MODE_OSCILLATION; hideSystemUi(); keepScreenAwake(true); stereoView=new StereoView(this); stereoView.setStimulus(oscillationSpec,crossed);
        oscillationClockReady=false;
        GestureDetector gd=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e){return true;}
            @Override public boolean onSingleTapUp(MotionEvent e){
                if(!oscillationClockReady) return true;
                String state=e.getX()<stereoView.getWidth()/3f?"cube":(e.getX()<2*stereoView.getWidth()/3f?"top_stack":"front_stack"); logOsc(state); return true;
            }
            @Override public void onLongPress(MotionEvent e){ if(oscillationClockReady) logOsc("other"); }
        });
        stereoView.setOnTouchListener((v,e)->gd.onTouchEvent(e)); setContentView(stereoView);
        stereoView.post(()->{ oscillationStartMs=SystemClock.elapsedRealtime(); oscillationClockReady=true; });
    }

    private void logOsc(String state){
        if(!oscillationClockReady) return;
        long ms=SystemClock.elapsedRealtime()-oscillationStartMs; oscillationEventIndex++;
        try{store.append(SessionStore.csvRow("event",oscillationSessionId,PARENT_MANIFEST_SHA256,"running",participantCode,oscillationSpec.stimulusId,crossed?"crossed":"parallel",Integer.toString(oscillationEventIndex),Long.toString(ms),state));}
        catch(IOException e){error(e);}
    }

    private String oscillationStatusRow(String status,long elapsedMs) {
        return SessionStore.csvRow("status",oscillationSessionId,PARENT_MANIFEST_SHA256,status,participantCode,oscillationSpec==null?"":oscillationSpec.stimulusId,crossed?"crossed":"parallel",Integer.toString(oscillationEventIndex),Long.toString(elapsedMs),"");
    }

    private void finalizeOscillation(String status) {
        if(!oscillationActive) return;
        long elapsed=oscillationStartMs==0L?0L:SystemClock.elapsedRealtime()-oscillationStartMs;
        try { store.append(oscillationStatusRow(status,elapsed)); }
        catch(IOException e) { error(e); }
        oscillationActive=false;
        oscillationClockReady=false;
    }

    private void showContinuous(){
        if(!isLandscapeNow()){ showLandscapeRequired(this::showContinuous); return; }
        mode=MODE_CONTINUOUS; showSystemUi(); keepScreenAwake(true); crossed=false;
        FrameLayout frame=new FrameLayout(this); stereoView=new StereoView(this); frame.addView(stereoView,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout ctl=new LinearLayout(this); ctl.setOrientation(LinearLayout.VERTICAL); ctl.setBackgroundColor(0xDDFFFFFF); ctl.setPadding(16,5,16,5);
        TextView label=text("Continuous explorer: g = 0.00",14); ctl.addView(label);
        SeekBar gbar=new SeekBar(this); gbar.setMax(130); ctl.addView(gbar,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        Spinner pattern=new Spinner(this); String[] pats={"v4=0 v6=0","v4=5 v6=5","v4=10 v6=10","v4=15 v6=0","v4=20 v6=5","v4=-5 v6=-5","v4=5 v6=-5","v4=-10 v6=10"}; pattern.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,pats)); row.addView(pattern,new LinearLayout.LayoutParams(0,-2,1));
        Button sign=button("Sign +"); final int[] sgn={1}; sign.setOnClickListener(v->{sgn[0]*=-1;sign.setText("Sign "+(sgn[0]>0?"+":"−"));updateContinuous(gbar,pattern,sgn[0],label);}); row.addView(sign);
        Button swap=button("Parallel"); swap.setOnClickListener(v->{crossed=!crossed;swap.setText(crossed?"Crossed":"Parallel");updateContinuous(gbar,pattern,sgn[0],label);}); row.addView(swap); ctl.addView(row);
        gbar.setOnSeekBarChangeListener(new SimpleSeek(){@Override public void onProgressChanged(SeekBar b,int p,boolean u){updateContinuous(gbar,pattern,sgn[0],label);}});
        pattern.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){updateContinuous(gbar,pattern,sgn[0],label);}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM); frame.addView(ctl,cp); setContentView(frame); updateContinuous(gbar,pattern,sgn[0],label);
    }

    private void updateContinuous(SeekBar gbar,Spinner pat,int sign,TextView label){
        double g=gbar.getProgress()*0.05; double[][] ps={{0,0},{5,5},{10,10},{15,0},{20,5},{-5,-5},{5,-5},{-10,10}}; int p=Math.max(0,pat.getSelectedItemPosition()); StimulusSpec s=new StimulusSpec("LIVE",g,sign,ps[p][0],ps[p][1]); stereoView.setStimulus(s,crossed); label.setText(String.format(Locale.US,"Continuous explorer: g = %.2f   v4 = %.0f   v6 = %.0f   sign %s",g,ps[p][0],ps[p][1],sign>0?"+":"−"));
    }

    private void showCalibration(){
        mode=MODE_SETUP; showSystemUi(); keepScreenAwake(false); LinearLayout box=formBox(); box.addView(text("Physical screen calibration",24));
        box.addView(text("Place a standard ID-1 credit/debit card against the screen. Adjust the outlined rectangle until its width matches the card's 85.60 mm width, then save. A matching reported resolution does not prove that a calibration belongs to the same physical display; recalibrate after changing phones/monitors, display scaling, resolution, or presentation configuration.",15));
        FrameLayout holder=new FrameLayout(this); holder.setBackgroundColor(Color.WHITE); LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,260); box.addView(holder,hp);
        View rect=new View(this); rect.setBackground(makeBorder()); FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(500,(int)(500*53.98/85.60),Gravity.CENTER); holder.addView(rect,rp);
        TextView size=text("Width: 500 screen px",15); box.addView(size);
        SeekBar bar=new SeekBar(this); int sw=getResources().getDisplayMetrics().widthPixels; bar.setMax(Math.max(100,sw-100)); bar.setProgress(Math.min(500,bar.getMax())); box.addView(bar,wide());
        bar.setOnSeekBarChangeListener(new SimpleSeek(){@Override public void onProgressChanged(SeekBar b,int p,boolean u){int w=Math.max(100,p);FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)rect.getLayoutParams();lp.width=w;lp.height=(int)(w*53.98/85.60);rect.setLayoutParams(lp);size.setText("Width: "+w+" screen px");}});
        Button save=button("Save calibration"); save.setOnClickListener(v->{int w=Math.max(100,bar.getProgress());screenPxPerMm=w/85.60;getSharedPreferences("sma",MODE_PRIVATE).edit().putLong("pxmm",Double.doubleToLongBits(screenPxPerMm)).putInt("cal_w",getResources().getDisplayMetrics().widthPixels).putInt("cal_h",getResources().getDisplayMetrics().heightPixels).apply();toast(String.format(Locale.US,"Saved %.3f screen px/mm",screenPxPerMm));showHome();}); box.addView(save,wide()); setContentView(wrap(box));
    }

    private android.graphics.drawable.Drawable makeBorder(){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(Color.TRANSPARENT);d.setStroke(4,Color.BLACK);return d;}

    private void loadCalibration(){
        android.content.SharedPreferences p=getSharedPreferences("sma",MODE_PRIVATE); if(!p.contains("pxmm")){screenPxPerMm=Double.NaN;return;} int w=p.getInt("cal_w",-1),h=p.getInt("cal_h",-1); int cw=getResources().getDisplayMetrics().widthPixels,ch=getResources().getDisplayMetrics().heightPixels; if(w!=cw||h!=ch){screenPxPerMm=Double.NaN;return;} screenPxPerMm=Double.longBitsToDouble(p.getLong("pxmm",Double.doubleToLongBits(Double.NaN)));
    }

    private void showSavedSessions() {
        try {
            List<File> files=store.listSessions();
            if(files.isEmpty()){ toast("No saved sessions yet."); return; }
            String[] names=new String[files.size()]; for(int i=0;i<files.size();i++) names[i]=files.get(i).getName();
            new AlertDialog.Builder(this).setTitle("Export saved session").setItems(names,(d,which)->exportFile(files.get(which),"text/csv")).setNegativeButton("Cancel",null).show();
        } catch(IOException e) { error(e); }
    }

    private void exportAllSessions() {
        try { exportFile(store.createAllSessionsZip(),"application/zip"); }
        catch(IOException e) { error(e); }
    }

    private void exportFile(File f,String mime) {
        pendingExportFile=f;
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType(mime); i.putExtra(Intent.EXTRA_TITLE,f.getName()); startActivityForResult(i,REQ_EXPORT);
    }

    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);
        if(req==REQ_EXPORT&&result==RESULT_OK&&data!=null&&pendingExportFile!=null){
            Uri u=data.getData(); File src=pendingExportFile;
            try(InputStream in=new FileInputStream(src);OutputStream out=getContentResolver().openOutputStream(u)){
                byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);toast("Export complete.");
            }catch(Exception e){error(e);} finally { pendingExportFile=null; }
        }
    }

    private LinearLayout formBox(){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(35,18,35,18);b.setBackgroundColor(Color.WHITE);return b;}
    private ScrollView wrap(LinearLayout b){ScrollView s=new ScrollView(this);s.addView(b);return s;}
    private CheckBox check(String s){CheckBox c=new CheckBox(this);c.setText(s);return c;}
    private RadioGroup radioRow(String title,String[] labels,int base,LinearLayout parent){parent.addView(text(title,15));RadioGroup g=new RadioGroup(this);g.setOrientation(RadioGroup.HORIZONTAL);for(int i=0;i<labels.length;i++){RadioButton r=new RadioButton(this);r.setText(labels[i]);r.setId(base+i);g.addView(r);}parent.addView(g);return g;}
    private String radioValue(RadioGroup g){int id=g.getCheckedRadioButtonId();if(id==-1)return "";RadioButton r=g.findViewById(id);return r.getText().toString().toLowerCase(Locale.US).replace(" / ","_").replace(' ','_');}
    private String joinChecked(CheckBox... cs){ArrayList<String> a=new ArrayList<>();for(CheckBox c:cs)if(c.isChecked())a.add(c.getText().toString().toLowerCase(Locale.US).replace(' ','_'));return String.join(";",a);}
    private double parseDouble(String s){try{return Double.parseDouble(s.trim());}catch(Exception e){return Double.NaN;}}
    private int parseInt(String s,int def){try{return Integer.parseInt(s.trim());}catch(Exception e){return def;}}
    private String num(double x){return Double.isNaN(x)?"":String.format(Locale.US,"%.6f",x);}
    private double arcmin(double screenPx){if(Double.isNaN(screenPxPerMm)||Double.isNaN(viewingDistanceMm)||viewingDistanceMm<=0)return Double.NaN;double mm=screenPx/screenPxPerMm;return Math.toDegrees(Math.atan2(mm,viewingDistanceMm))*60.0;}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private void error(Exception e){new AlertDialog.Builder(this).setTitle("Error").setMessage(e.toString()).setPositiveButton("OK",null).show();}

    private void keepScreenAwake(boolean keep) {
        if(keep) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @SuppressWarnings("deprecation")
    private void hideSystemUi(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R) {
            WindowInsetsController c=getWindow().getInsetsController();
            if(c!=null) {
                c.hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @SuppressWarnings("deprecation")
    private void showSystemUi(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R) {
            WindowInsetsController c=getWindow().getInsetsController();
            if(c!=null) c.show(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());
        } else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    private abstract static class SimpleSeek implements SeekBar.OnSeekBarChangeListener {public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}}
}
