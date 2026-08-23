package ai.youvan.stereomultistability;

public final class StereoMath {
    private StereoMath() {}

    public static final double IMAGE_WIDTH = 900.0;
    public static final double IMAGE_HEIGHT = 450.0;
    public static final double MODEL_TO_IMAGE_SCALE = 0.52;
    public static final double LEFT_PANEL_CENTER_X = 285.0;
    public static final double RIGHT_PANEL_CENTER_X = 615.0;
    public static final double BASE_THETA_DEG = 6.0;
    public static final double BASE_ROTATION_ORIGIN_X = 0.0;

    public static final double[][] LEFT = {
        {266.0,102.0},{626.0,102.0},{266.0,366.0},{626.0,366.0},
        {357.0,176.0},{718.0,176.0},{357.0,440.0},{718.0,440.0}
    };

    public static final double[] BASE_W = {-100,-100,-100,-100,100,100,100,100};

    public static final int[][] EDGES = {
        {0,1},{1,3},{3,2},{2,0},
        {4,5},{5,7},{7,6},{6,4},
        {0,4},{1,5},{2,6},{3,7}
    };

    public static double[] baseDisparity() {
        double a = Math.toRadians(BASE_THETA_DEG);
        double[] d = new double[8];
        for (int i=0;i<8;i++) {
            double x = LEFT[i][0] - BASE_ROTATION_ORIGIN_X;
            double xr = BASE_ROTATION_ORIGIN_X + x*Math.cos(a) + BASE_W[i]*Math.sin(a);
            d[i] = xr - LEFT[i][0];
        }
        return d;
    }

    public static double[][] rightVertices(StimulusSpec spec) {
        double[][] r = copy(LEFT);
        double[] d = baseDisparity();
        for (int i=0;i<8;i++) r[i][0] = LEFT[i][0] + spec.sign*spec.globalScale*d[i];
        r[4][0] += spec.vertex4DxModelUnits;
        r[6][0] += spec.vertex6DxModelUnits;
        return r;
    }

    public static double[][] canonicalProject(double[][] vertices, double centerX) {
        final double xmin=240.0, xmax=740.0, ymin=80.0, ymax=460.0;
        final double s=MODEL_TO_IMAGE_SCALE;
        double pw=(xmax-xmin)*s;
        double ph=(ymax-ymin)*s;
        double ox=centerX-pw/2.0-xmin*s;
        double oy=IMAGE_HEIGHT/2.0-ph/2.0-ymin*s;
        double[][] pts=new double[vertices.length][2];
        for(int i=0;i<vertices.length;i++) {
            pts[i][0]=ox+vertices[i][0]*s;
            pts[i][1]=oy+vertices[i][1]*s;
        }
        return pts;
    }

    public static double[] renderedDisparities(StimulusSpec spec) {
        double[][] r=rightVertices(spec);
        double[] d=new double[8];
        for(int i=0;i<8;i++) d[i]=(r[i][0]-LEFT[i][0])*MODEL_TO_IMAGE_SCALE;
        return d;
    }

    public static double rmsRenderedDisparity(StimulusSpec spec) {
        double s=0.0; for(double x: renderedDisparities(spec)) s += x*x;
        return Math.sqrt(s/8.0);
    }

    public static double maxAbsRenderedDisparity(StimulusSpec spec) {
        double m=0.0; for(double x: renderedDisparities(spec)) m=Math.max(m,Math.abs(x));
        return m;
    }

    private static double[][] copy(double[][] src) {
        double[][] out=new double[src.length][2];
        for(int i=0;i<src.length;i++) { out[i][0]=src[i][0]; out[i][1]=src[i][1]; }
        return out;
    }
}
