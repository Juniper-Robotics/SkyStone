package org.firstinspires.ftc.teamcode.SkyStone.V2.Subsystems;

import android.support.annotation.NonNull;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.localization.ThreeTrackingWheelLocalizer;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.openftc.revextensions2.ExpansionHubEx;
import org.openftc.revextensions2.RevBulkData;

import java.util.Arrays;
import java.util.List;

public class Odometry extends ThreeTrackingWheelLocalizer {
    public static double TICKS_PER_REV = 4096;
    public static double WHEEL_RADIUS = 1.276; // in
    public static double GEAR_RATIO = 1; // output (wheel) speed / input (encoder) speed
    public static double LATERAL_DISTANCE= 13.85;
    private ExpansionHubEx hub;
    private DcMotor rightVertEncoder, horizontalEncoder, leftVertEncoder;

    public Odometry(HardwareMap hardwareMap) {
        super(Arrays.asList(
                new Pose2d(1.72, LATERAL_DISTANCE / 2, 0), // left
                new Pose2d(1.72, -LATERAL_DISTANCE / 2, 0), // right
                new Pose2d(-0.942, -7.33, Math.toRadians(90)) // front
        ));
        hub = hardwareMap.get(ExpansionHubEx.class, "Expansion Hub 2");
        leftVertEncoder = hardwareMap.dcMotor.get("LI");
        rightVertEncoder = hardwareMap.dcMotor.get("RI");
        horizontalEncoder = hardwareMap.dcMotor.get("L.L");
    }

    public static double encoderTicksToInches(int ticks) {
        return WHEEL_RADIUS * 2 * Math.PI * GEAR_RATIO * ticks / TICKS_PER_REV;
    }

    @NonNull
    @Override
    public List<Double> getWheelPositions() {
        RevBulkData bulkData = hub.getBulkInputData();

        if (bulkData == null) {
            return Arrays.asList(0.0, 0.0, 0.0, 0.0);
        }
        return Arrays.asList(
                encoderTicksToInches(-bulkData.getMotorCurrentPosition(leftVertEncoder)),
                encoderTicksToInches(bulkData.getMotorCurrentPosition(rightVertEncoder)),
                encoderTicksToInches(-bulkData.getMotorCurrentPosition(horizontalEncoder))
        );
    }
}