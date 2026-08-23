package ai.youvan.stereomultistability;

import org.junit.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

public class StimulusCatalogTest {
    @Test public void corpusHas289UniqueIds() {
        List<StimulusSpec> all=StimulusCatalog.all(); assertEquals(289,all.size());
        HashSet<String> ids=new HashSet<>(); for(StimulusSpec s:all) ids.add(s.stimulusId); assertEquals(289,ids.size());
    }

    @Test public void firstIsExactZeroControl() {
        StimulusSpec s=StimulusCatalog.all().get(0); assertEquals("S0001",s.stimulusId); assertEquals(0.0,s.globalScale,0.0); assertEquals(0.0,StereoMath.rmsRenderedDisparity(s),0.0);
    }

    @Test public void lastMatchesParentOrdering() {
        StimulusSpec s=StimulusCatalog.all().get(288); assertEquals("S0289",s.stimulusId); assertEquals(6.5,s.globalScale,0.0); assertEquals(-1,s.sign); assertEquals(-10.0,s.vertex4DxModelUnits,0.0); assertEquals(10.0,s.vertex6DxModelUnits,0.0);
    }

    @Test public void knownG1MetricsMatchParentAtlas() {
        StimulusSpec s=null; for(StimulusSpec x:StimulusCatalog.all()) if(x.sign==1 && x.globalScale==1.0 && x.vertex4DxModelUnits==0 && x.vertex6DxModelUnits==0){s=x;break;}
        assertNotNull(s); assertEquals(5.510951125589413,StereoMath.rmsRenderedDisparity(s),1e-9); assertEquals(7.218712709637384,StereoMath.maxAbsRenderedDisparity(s),1e-9);
    }

    /**
     * Strong regression test: execute the actual Android Java catalog and compare every
     * generated row against the parent-atlas fixture bundled at repository root.
     */
    @Test public void actualJavaCatalogMatchesAll289ParentFixtureRows() throws Exception {
        InputStream in=getClass().getResourceAsStream("/parent_manifest.csv");
        assertNotNull("fixtures/parent_manifest.csv must be available as a test resource",in);

        List<String[]> expected=new ArrayList<>();
        try(BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))) {
            String header=br.readLine();
            assertEquals("stimulus_id,global_scale,sign,vertex4_dx_model_units,vertex6_dx_model_units",header);
            String line;
            while((line=br.readLine())!=null) {
                if(line.trim().isEmpty()) continue;
                String[] parts=line.split(",",-1);
                assertEquals("Unexpected fixture column count in: "+line,5,parts.length);
                expected.add(parts);
            }
        }

        List<StimulusSpec> actual=StimulusCatalog.all();
        assertEquals(289,expected.size());
        assertEquals(expected.size(),actual.size());

        for(int i=0;i<expected.size();i++) {
            String[] e=expected.get(i);
            StimulusSpec a=actual.get(i);
            String where="row "+(i+2)+" / "+e[0];
            assertEquals(where,e[0],a.stimulusId);
            assertEquals(where,Double.parseDouble(e[1]),a.globalScale,0.0);
            assertEquals(where,Integer.parseInt(e[2]),a.sign);
            assertEquals(where,Double.parseDouble(e[3]),a.vertex4DxModelUnits,0.0);
            assertEquals(where,Double.parseDouble(e[4]),a.vertex6DxModelUnits,0.0);
        }
    }
}
