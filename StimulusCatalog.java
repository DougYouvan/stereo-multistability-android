package ai.youvan.stereomultistability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StimulusCatalog {
    private StimulusCatalog() {}

    private static final double[] SCALES = {
        0.10, 0.25, 0.50, 0.75, 1.0, 1.25, 1.50, 1.75, 2.0,
        2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0, 6.5
    };

    private static final double[][] LOCAL = {
        {0.0, 0.0}, {5.0, 5.0}, {10.0, 10.0}, {15.0, 0.0},
        {20.0, 5.0}, {-5.0, -5.0}, {5.0, -5.0}, {-10.0, 10.0}
    };

    public static List<StimulusSpec> all() {
        ArrayList<StimulusSpec> out = new ArrayList<>(289);
        int n = 1;
        out.add(new StimulusSpec(id(n++), 0.0, +1, 0.0, 0.0));
        for (int sign : new int[]{+1, -1}) {
            for (double g : SCALES) {
                for (double[] d : LOCAL) {
                    out.add(new StimulusSpec(id(n++), g, sign, d[0], d[1]));
                }
            }
        }
        return Collections.unmodifiableList(out);
    }

    public static StimulusSpec byId(String id) {
        int n = Integer.parseInt(id.substring(1));
        List<StimulusSpec> all = all();
        if (n < 1 || n > all.size()) throw new IllegalArgumentException("Unknown stimulus: " + id);
        return all.get(n - 1);
    }

    private static String id(int n) {
        return String.format(java.util.Locale.US, "S%04d", n);
    }
}
