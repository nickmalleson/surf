package simburglar;

/**
 * Used to define information about GIS data so that methods can be tested. To
 * add a new data set add it to the enum and change the CHOSEN_DATA field to
 * point to the new settings.
 */
public enum GIS_FIELDS {

    LEEDS("leeds", 211107, 152502, 152503);
    public String DATA_NAME;
    public Integer HOME_ID;
    public Integer BUILDING_A;
    public Integer BUILDING_B;

    /**
     *
     * @param dataName Name of the directory containing GIS data
     * @param homeID Home location for the agent
     * @param buildingA The ID of the closest building to <code>buildingB</code>
     * @param buildingB The ID of the closest building to <code>buildingA</code>
     */
    GIS_FIELDS(String dataName, Integer homeID, Integer buildingA,
            Integer buildingB) {
        this.DATA_NAME = dataName;
        this.HOME_ID = homeID;
        this.BUILDING_A = buildingA;
        this.BUILDING_B = buildingB;
    }

    public static GIS_FIELDS CHOSEN_DATA = GIS_FIELDS.LEEDS;
}