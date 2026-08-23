package ai.youvan.stereomultistability;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class TrialPlannerTest {
    @Test public void standardPlansContainControlAndEightRepeatsWithLag() {
        for(long seed=0;seed<500;seed++) {
            TrialPlanner.Plan p=TrialPlanner.plan(StimulusCatalog.all(),40,8,seed,8);
            assertEquals(40,p.trials.size());
            HashMap<String,Integer> counts=new HashMap<>(); ArrayList<StimulusSpec> seq=new ArrayList<>();
            for(TrialPlanner.Trial t:p.trials){counts.put(t.spec.stimulusId,counts.getOrDefault(t.spec.stimulusId,0)+1);seq.add(t.spec);} 
            assertTrue(counts.containsKey("S0001"));
            int repeated=0; for(int n:counts.values()) if(n==2) repeated++;
            assertEquals(8,repeated); assertTrue(TrialPlanner.validRepeatLag(seq,8));
        }
    }
}
