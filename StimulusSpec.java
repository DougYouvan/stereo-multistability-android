package ai.youvan.stereomultistability;

public final class StimulusSpec {
    public final String stimulusId;
    public final double globalScale;
    public final int sign;
    public final double vertex4DxModelUnits;
    public final double vertex6DxModelUnits;

    public StimulusSpec(String stimulusId, double globalScale, int sign,
                        double vertex4DxModelUnits, double vertex6DxModelUnits) {
        this.stimulusId = stimulusId;
        this.globalScale = globalScale;
        this.sign = sign;
        this.vertex4DxModelUnits = vertex4DxModelUnits;
        this.vertex6DxModelUnits = vertex6DxModelUnits;
    }
}
