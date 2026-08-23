package ai.youvan.stereomultistability;

import java.security.SecureRandom;
import java.util.*;

public final class TrialPlanner {
    private TrialPlanner() {}

    public static final class Trial {
        public final StimulusSpec spec;
        public final int repeatInstance;
        Trial(StimulusSpec spec,int repeatInstance){this.spec=spec;this.repeatInstance=repeatInstance;}
    }

    public static final class Plan {
        public final List<Trial> trials;
        public final long seed;
        Plan(List<Trial> trials,long seed){this.trials=trials;this.seed=seed;}
    }

    public static Plan standardPlan() {
        return plan(StimulusCatalog.all(),40,8,new SecureRandom().nextLong(),8);
    }

    public static Plan plan(List<StimulusSpec> rows,int count,int repeatCount,long seed,int minRepeatLag) {
        Random rng=new Random(seed);
        repeatCount=Math.max(0,Math.min(repeatCount,count/2));
        int uniqueCount=Math.min(rows.size(),count-repeatCount);
        ArrayList<StimulusSpec> selected=new ArrayList<>();
        StimulusSpec control=rows.get(0);
        selected.add(control);
        int remaining=uniqueCount-1;
        ArrayList<StimulusSpec> pos=new ArrayList<>(), neg=new ArrayList<>();
        for(int i=1;i<rows.size();i++) {
            if(rows.get(i).sign>0) pos.add(rows.get(i)); else neg.add(rows.get(i));
        }
        Collections.shuffle(pos,rng); Collections.shuffle(neg,rng);
        int npos=(remaining+1)/2, nneg=remaining/2;
        selected.addAll(pos.subList(0,Math.min(npos,pos.size())));
        selected.addAll(neg.subList(0,Math.min(nneg,neg.size())));
        Collections.shuffle(selected,rng);

        ArrayList<StimulusSpec> repeatPool=new ArrayList<>();
        for(StimulusSpec s:selected) if(!s.stimulusId.equals("S0001")) repeatPool.add(s);
        Collections.shuffle(repeatPool,rng);
        ArrayList<StimulusSpec> items=new ArrayList<>(selected);
        items.addAll(repeatPool.subList(0,Math.min(repeatCount,repeatPool.size())));

        ArrayList<StimulusSpec> shuffled=null;
        for(int attempt=0;attempt<20000;attempt++) {
            ArrayList<StimulusSpec> cand=new ArrayList<>(items);
            Collections.shuffle(cand,rng);
            if(validRepeatLag(cand,minRepeatLag)) { shuffled=cand; break; }
        }
        if(shuffled==null) throw new IllegalStateException("Could not satisfy repeat lag");

        HashMap<String,Integer> seen=new HashMap<>();
        ArrayList<Trial> trials=new ArrayList<>();
        for(StimulusSpec s:shuffled) {
            int n=seen.getOrDefault(s.stimulusId,0)+1; seen.put(s.stimulusId,n);
            trials.add(new Trial(s,n));
        }
        return new Plan(Collections.unmodifiableList(trials),seed);
    }

    public static boolean validRepeatLag(List<StimulusSpec> trials,int minLag) {
        HashMap<String,Integer> first=new HashMap<>();
        for(int i=0;i<trials.size();i++) {
            String id=trials.get(i).stimulusId;
            if(first.containsKey(id)) {
                if(i-first.get(id)<minLag) return false;
            } else first.put(id,i);
        }
        return true;
    }
}
