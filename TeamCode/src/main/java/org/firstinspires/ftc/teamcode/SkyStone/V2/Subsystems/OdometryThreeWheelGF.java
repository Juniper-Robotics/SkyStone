package org.firstinspires.ftc.teamcode.SkyStone.V2.Subsystems;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.localization.Localizer;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.jetbrains.annotations.NotNull;
import org.openftc.revextensions2.ExpansionHubEx;
import org.openftc.revextensions2.RevBulkData;

import java.util.Arrays;
@Config
public class OdometryThreeWheelGF implements Localizer {
    private ExpansionHubEx hub;
    private DcMotor rightVertEncoder, horizontalEncoder, leftVertEncoder;
    private Robot robot;
    public static double moveScalingFactor = 0;
    public static double turnScalingFactor = 35.694;
    public static double auxScalingFactor = 0;//12.6148;
    public static double auxPredictionScalingFactor = 0;

    public double wheelLeftLast = 0.0;
    public double wheelRightLast = 0.0;
    public double wheelAuxLast = 0.0;

    public double worldXPosition = 0.0;
    public double worldYPosition = 0.0;
    public double worldAngle_rad = 0.0;

    public double currPos_l = 0;
    public double currPos_r = 0;
    public double currPos_a = 0;

    //stuff for reading the angle in an absolute manner
    public double wheelLeftInitialReading = 0.0;
    public double wheelRightInitialReading = 0.0;
    public double lastResetAngle = 0.0;//this is set when you reset the position

    //use this to get how far we have traveled in the y dimension this update
    public double currentTravelYDistance = 0.0;


    public OdometryThreeWheelGF(HardwareMap hardwareMap) {
        robot = Robot.getInstance();
        hub = hardwareMap.get(ExpansionHubEx.class, "Expansion Hub 2");
        leftVertEncoder = robot.opMode.hardwareMap.dcMotor.get("LI");
        rightVertEncoder = robot.opMode.hardwareMap.dcMotor.get("RI");
        horizontalEncoder = robot.opMode.hardwareMap.dcMotor.get("L.L");
    }

    @NotNull
    @Override
    public Pose2d getPoseEstimate() {
        return new Pose2d(worldXPosition,worldYPosition,worldAngle_rad);
    }

    @Override
    public void setPoseEstimate(@NotNull Pose2d pose2d) {
        worldXPosition = pose2d.getX();
        worldYPosition = pose2d.getY();
        worldAngle_rad = pose2d.getHeading();
        //remember where we were at the time of the reset
        wheelLeftInitialReading = currPos_l;
        wheelRightInitialReading = currPos_r;
        lastResetAngle = pose2d.getHeading();
    }

    @Override
    public void update() {
        RevBulkData bulkData = hub.getBulkInputData();

        if (bulkData == null) {
            currPos_l=0;
            currPos_r=0;
            currPos_a=0;
        }
        currPos_l=bulkData.getMotorCurrentPosition(leftVertEncoder);
        currPos_r=bulkData.getMotorCurrentPosition(rightVertEncoder);
        currPos_a=bulkData.getMotorCurrentPosition(horizontalEncoder);
        PositioningCalculations();
    }
    private double AngleWrap(double angle){
        while (angle<-Math.PI){
            angle += 2.0*Math.PI;
        }
        while (angle>Math.PI){
            angle -= 2.0*Math.PI;
        }
        return angle;
    }








    /**
     * Updates our position on the field using the change from the encoders
     */
    public void PositioningCalculations(){
        double wheelLeftCurrent = -currPos_l;
        double wheelRightCurrent= currPos_r;
        double wheelAuxCurrent = currPos_a;

        //compute how much the wheel data has changed
        double wheelLeftDelta = wheelLeftCurrent - wheelLeftLast;
        double wheelRightDelta = wheelRightCurrent - wheelRightLast;
        double wheelAuxDelta = wheelAuxCurrent - wheelAuxLast;


        //get the real distance traveled using the movement scaling factors
        double wheelLeftDeltaScale = wheelLeftDelta*moveScalingFactor/1000.0;
        double wheelRightDeltaScale = wheelRightDelta*moveScalingFactor/1000.0;
        double wheelAuxDeltaScale = wheelAuxDelta*auxScalingFactor/1000.00;

        //get how much our angle has changed
        double angleIncrement = (wheelLeftDelta-wheelRightDelta)*turnScalingFactor/100000.0;
        robot.telemetry.addLine("Angle increment is " +
                (angleIncrement > 0 ? "POSITIVE" : "NEGATIVE"));


        //but use absolute for our actual angle
        double wheelRightTotal = currPos_r-wheelRightInitialReading;
        double wheelLeftTotal = -(currPos_l-wheelLeftInitialReading);

        double worldAngleLast = worldAngle_rad;
        worldAngle_rad = AngleWrap(((wheelLeftTotal-wheelRightTotal)*turnScalingFactor/100000.0) + lastResetAngle);

        //get the predicted amount the straif will go
        double tracker_a_prediction = Math.toDegrees(angleIncrement)*(auxPredictionScalingFactor/10.0);
        //now subtract that from the actual
        double r_xDistance = wheelAuxDeltaScale-tracker_a_prediction;


        //relativeY will by defa
        double relativeY = (wheelLeftDeltaScale + wheelRightDeltaScale)/2.0;
        double relativeX = r_xDistance;

        robot.telemetry.addLine("left wheel: " + (wheelLeftCurrent*moveScalingFactor/1000.0));
        robot.telemetry.addLine("right wheel: " + (wheelRightCurrent*moveScalingFactor/1000.0));
        robot.telemetry.addLine("aux wheel: " + (wheelAuxCurrent*auxScalingFactor/1000.0));


        //if angleIncrement is > 0 we can use steven's dumb stupid and stupid well you know the point
        //equations because he is dumb
        if(Math.abs(angleIncrement) > 0){
            //gets the radius of the turn we are in
            double radiusOfMovement = (wheelRightDeltaScale+wheelLeftDeltaScale)/(2*angleIncrement);
            //get the radius of our straifing circle
            double radiusOfStraif = r_xDistance/angleIncrement;





            relativeY = (radiusOfMovement * Math.sin(angleIncrement)) - (radiusOfStraif * (1 - Math.cos(angleIncrement)));

            relativeX = radiusOfMovement * (1 - Math.cos(angleIncrement)) + (radiusOfStraif * Math.sin(angleIncrement));

            robot.telemetry.addLine("radius of movement: " + radiusOfMovement);
//            myRobot.telemetry.addLine("radius of straif: " + radiusOfStraif);
            robot.telemetry.addLine("relative y: " + relativeY);
            robot.telemetry.addLine("relative x: " + relativeX);
        }



        worldXPosition += (Math.cos(worldAngleLast) * relativeY) + (Math.sin(worldAngleLast) *
                relativeX);
        worldYPosition += (Math.sin(worldAngleLast) * relativeY) - (Math.cos(worldAngleLast) *
                relativeX);



        //save the last positions for later
        wheelLeftLast = wheelLeftCurrent;
        wheelRightLast = wheelRightCurrent;
        wheelAuxLast = wheelAuxCurrent;


        //save how far we traveled in the y dimension this update for anyone that needs it
        //currently the absolute control of the collector radius uses it to compensate for
        //robot movement
        currentTravelYDistance = relativeY;
    }
}
