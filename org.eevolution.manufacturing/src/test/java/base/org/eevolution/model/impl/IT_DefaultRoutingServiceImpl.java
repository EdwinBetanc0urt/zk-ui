/**
 * 
 */
package org.eevolution.model.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.adempiere.core.domains.models.I_AD_WF_Node;
import org.adempiere.core.domains.models.I_AD_Workflow;
import org.adempiere.core.domains.models.I_S_Resource;
import org.adempiere.core.domains.models.X_C_UOM;
import org.adempiere.test.CommonGWSetup;
import org.compiere.model.MResource;
import org.compiere.model.MResourceType;
import org.compiere.model.MUOM;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWorkflow;
import org.eevolution.manufacturing.model.impl.DefaultRoutingServiceImpl;
import org.eevolution.model.RoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author teo_sarca
 *
 */
@Tag("Model")
@Tag("eevolution")
@Tag("Manufacturing")
class IT_DefaultRoutingServiceImpl extends CommonGWSetup {

    RoutingService routingService = null;

    @BeforeEach
    void localSetUp() {

        routingService = new DefaultRoutingServiceImpl();

    }

    @Test
    void test_estimateWorkingTime() {

        assertEstimateWorkingTime(10, 10, 0);
        assertEstimateWorkingTime(10.0 / 1.0, 10, 1);
        assertEstimateWorkingTime(10.0 / 6.0, 10, 6);

    }

    @Test
    void test_estimateWorkingTime_Qty() {

        assertEstimateWorkingTime(10, 10, 0);
        assertEstimateWorkingTime(10.0 / 1.0, 10, 1);
        assertEstimateWorkingTime(10.0 / 6.0, 10, 6);

    }

    @Test
    void test_calculateNodeDuration() {

        assertCalculateDuration(100.0 / 100.0 + 10.0,
                100, 100, 10, 0, 0);
        assertCalculateDuration(100.0 / 100.0 + 10.0 / 1.0,
                100, 100, 10, 1, 0);
        assertCalculateDuration(100.0 / 100.0 + 10.0 / 6.0,
                100, 100, 10, 6, 0);

    }

    @Test
    void test_calculateWorkflowDuration() {

        MWorkflow wf = createWorkflow(1);
        createNode(wf, "10", 0, 10, 0, 0);
        createNode(wf, "20", 0, 10, 0, 0);
        createNode(wf, "30", 0, 10, 0, 0);
        wf = new MWorkflow(getCtx(), wf.get_ID(), getTrxName());
        assertCalculateDuration(wf, 1000, 1000 * 10 + 1000 * 10 + 1000 * 10);

    }

    @Test
    void test_calculateWorkflowDuration_Overlap() {

        MWorkflow wf = createWorkflow(1);
        createNode(wf, "10", 0, 10, 0, 10);
        createNode(wf, "20", 0, 10, 0, 10);
        createNode(wf, "30", 0, 10, 0, 0);
        wf = new MWorkflow(getCtx(), wf.get_ID(), getTrxName());
        assertCalculateDuration(wf, 1000, 10 * 10 + 10 * 10 + 1000 * 10);

    }

    /**
     * @see RoutingService#estimateWorkingTime(I_AD_WF_Node)
     */
    private void assertEstimateWorkingTime(double expectedDuration,
            int duration, int unitsCycles) {

        int qtyBatchSize = 1; // not relevant
        int setupTime = 0; // not relevant
        int overlapUnits = 0; // not relevant
        MWorkflow wf = createWorkflow(qtyBatchSize);
        I_AD_WF_Node node = createNode(wf, "10", setupTime, duration,
                unitsCycles, overlapUnits);
        BigDecimal durationActual = routingService.estimateWorkingTime(node);
        assertEquals(expectedDuration, durationActual.doubleValue());

    }

    /**
     * @see RoutingService#calculateDuration(I_AD_WF_Node)
     */
    protected void assertCalculateDuration(final double expectedDuration,
            int qtyBatchSize, int setupTime, int duration,
            int unitsCycles, int overlapUnits) {

        MWorkflow wf = createWorkflow(qtyBatchSize);
        I_AD_WF_Node node = createNode(wf, "10", setupTime, duration,
                unitsCycles, overlapUnits);
        BigDecimal actualDuration = routingService.calculateDuration(node);
        assertEquals(expectedDuration, actualDuration.doubleValue());

    }

    /**
     * @see RoutingService#calculateDuration(I_AD_Workflow, I_S_Resource,
     *      BigDecimal)
     */
    protected void assertCalculateDuration(I_AD_Workflow wf, int qty,
            double durationExpected) {

        I_S_Resource plant = getCreatePlant();
        BigDecimal durationReal = routingService.calculateDuration(wf, plant,
                BigDecimal.valueOf(qty));
        assertEquals(durationExpected, durationReal.doubleValue());

    }

    private MWorkflow createWorkflow(int qtyBatchSize) {

        String value = "JUnit_Test_" + System.currentTimeMillis();
        MWorkflow wf = new MWorkflow(getCtx(), 0, getTrxName());
        wf.setWorkflowType(MWorkflow.WORKFLOWTYPE_Manufacturing);
        wf.setProcessType(MWorkflow.PROCESSTYPE_DedicateRepetititiveFlow);
        wf.setValue(value);
        wf.setName(value);
        wf.setDescription("Generated by " + getClass());
        wf.setAuthor("JUnit");
        wf.setQtyBatchSize(BigDecimal.valueOf(qtyBatchSize));
        //
        wf.saveEx();
        return wf;

    }

    private MWFNode createNode(MWorkflow wf, String value,
            int setupTime, int duration, int unitsCycles, int overlapUnits) {

        if (value == null) {
            value = "" + System.currentTimeMillis();
        }
        MWFNode node = new MWFNode(wf, value, value);
        node.setSetupTime(setupTime);
        node.setDuration(duration);
        node.setUnitsCycles(BigDecimal.valueOf(unitsCycles));
        node.setOverlapUnits(overlapUnits);
        //
        node.saveEx();
        return node;

    }

    private I_S_Resource getCreatePlant() {

        final String plantValue = "JUnit_Plant_24x7";
        MResource plant =
                new Query(getCtx(), MResource.Table_Name, "Value=?", null)
                        .setParameters(new Object[] { plantValue })
                        .setClient_ID()
                        .firstOnly();
        if (plant == null) {
            plant = new MResource(getCtx(), 0, null);
            plant.setValue(plantValue);
        }
        plant.setName(plantValue);
        plant.setIsManufacturingResource(true);
        plant.setManufacturingResourceType(
                MResource.MANUFACTURINGRESOURCETYPE_Plant);
        plant.setPlanningHorizon(365);
        plant.setM_Warehouse_ID(getM_Warehouse_ID());
        //
        MResourceType rt = null;
        if (plant.getS_ResourceType_ID() <= 0) {
            rt = new MResourceType(getCtx(), 0, null);
        } else {
            rt = new MResourceType(getCtx(), plant.getS_ResourceType_ID(),
                    null);
        }
        rt.setValue(plantValue);
        rt.setName(plantValue);
        rt.setAllowUoMFractions(true);
        rt.setC_TaxCategory_ID(getC_TaxCategory_ID());
        rt.setM_Product_Category_ID(getM_Product_Category_ID());
        rt.setC_UOM_ID(getC_UOM_ID("HR")); // Hour
        rt.setIsDateSlot(false);
        rt.setIsTimeSlot(false);
        rt.setIsSingleAssignment(false);
        rt.saveEx();
        plant.setS_ResourceType_ID(rt.getS_ResourceType_ID());
        //
        plant.saveEx();
        return plant;

    }

    public int getM_Warehouse_ID() {

        int M_Warehouse_ID = Env.getContextAsInt(getCtx(), "#M_Warehouse_ID");
        if (M_Warehouse_ID <= 0) {
            final String sql = "SELECT M_Warehouse_ID FROM M_Warehouse"
                    + " WHERE AD_Client_ID=0 OR AD_Client_ID=" + AD_CLIENT_ID
                    + " ORDER BY M_Warehouse_ID";
            M_Warehouse_ID = DB.getSQLValueEx(null, sql);
        }
        return M_Warehouse_ID;

    }

    public int getC_TaxCategory_ID() {

        int id = Env.getContextAsInt(getCtx(), "#C_TaxCategory_ID");
        if (id <= 0) {
            final String sql = "SELECT C_TaxCategory_ID FROM C_TaxCategory"
                    + " WHERE AD_Client_ID IN (0," + AD_CLIENT_ID + ")"
                    + " AND IsDefault='Y'"
                    + " ORDER BY C_TaxCategory_ID";
            id = DB.getSQLValueEx(null, sql);
        }
        return id;

    }

    public int getM_Product_Category_ID() {

        return DB.getSQLValueEx(null,
                "SELECT MIN(M_Product_Category_ID) FROM M_Product_Category"
                        + " WHERE AD_Client_ID IN (0," + AD_CLIENT_ID + ")"
                        + " AND IsDefault='Y'");

    }

    public int getC_UOM_ID(String x12de355) {

        String whereClause =
                X_C_UOM.COLUMNNAME_X12DE355 + "=? AND AD_Client_ID IN (0,?)";
        int[] ids = new Query(getCtx(), MUOM.Table_Name, whereClause, null)
                .setParameters(new Object[] { x12de355, AD_CLIENT_ID })
                .setOrderBy("AD_Client_ID DESC")
                .getIDs();
        return ids.length > 0 ? ids[0] : -1;

    }

}
