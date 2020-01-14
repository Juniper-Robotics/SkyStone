package org.firstinspires.ftc.teamcode.SkyStone.V2.Subsystems;

import android.support.annotation.NonNull;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.canvas.Canvas;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.control.PIDCoefficients;
import com.acmerobotics.roadrunner.drive.MecanumDrive;
import com.acmerobotics.roadrunner.followers.HolonomicPIDVAFollower;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.trajectory.constraints.DriveConstraints;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

import org.firstinspires.ftc.teamcode.RobotLibs.JMotor;
import org.firstinspires.ftc.teamcode.RobotLibs.JServo;
import org.firstinspires.ftc.teamcode.RobotLibs.StickyGamepad;
import org.firstinspires.ftc.teamcode.RobotLibs.Subsystem.Subsystem;
import org.openftc.revextensions2.ExpansionHubEx;
import org.openftc.revextensions2.RevBulkData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static org.firstinspires.ftc.teamcode.SkyStone.V2.Subsystems.DriveConstants.BASE_CONSTRAINTS;
import static org.firstinspires.ftc.teamcode.SkyStone.V2.Subsystems.DriveConstants.MOTOR_VELO_PID;
import static org.firstinspires.ftc.teamcode.SkyStone.V2.Subsystems.DriveConstants.RUN_USING_ENCODER;
import static org.firstinspires.ftc.teamcode.SkyStone.V2.Subsystems.DriveConstants.TRACK_WIDTH;
import static org.firstinspires.ftc.teamcode.SkyStone.V2.Subsystems.DriveConstants.kStatic;
import static org.firstinspires.ftc.teamcode.SkyStone.V2.Subsystems.DriveConstants.kV;
import static org.firstinspires.ftc.teamcode.SkyStone.V2.Subsystems.DriveConstants.kA;
import static org.firstinspires.ftc.teamcode.SkyStone.V2.Subsystems.DriveConstants.encoderTicksToInches;

@Config
public class MecanumDriveBase extends MecanumDrive implements Subsystem {

    /*
     *    front
     * 0         3
     *
     *
     * 1         2
     * */

    private ExpansionHubEx hub;
    public OpMode opMode;
    JMotor leftFront;
    JMotor leftBack;
    JMotor rightBack;
    JMotor rightFront;
    JServo grabServoRight;
    JServo grabServoLeft;
    JServo capStone;
    List<JMotor> driveMotors;
    private FtcDashboard dashboard = FtcDashboard.getInstance();
    public boolean thirdPersonDrive = false;
    //Road Runner
    DriveConstraints constraints = BASE_CONSTRAINTS;
    public static PIDCoefficients TRANSLATIONAL_PID = new PIDCoefficients(6, 0, 0);
    public static PIDCoefficients HEADING_PID = new PIDCoefficients(4, 0, 0.3);
    public HolonomicPIDVAFollower follower = new HolonomicPIDVAFollower(TRANSLATIONAL_PID, TRANSLATIONAL_PID, HEADING_PID);
    private Robot robot;
    public MecanumDriveBase(OpMode mode) {
        super(kV, kA, kStatic, TRACK_WIDTH);
        opMode = mode;
        hub = opMode.hardwareMap.get(ExpansionHubEx.class, "Expansion Hub 1");
        hub.setPhoneChargeEnabled(true);
        leftFront = new JMotor(mode.hardwareMap, "LF");
        leftBack = new JMotor(mode.hardwareMap, "LB");
        rightBack = new JMotor(mode.hardwareMap, "RB");
        rightFront = new JMotor(mode.hardwareMap, "RF");
        driveMotors = Arrays.asList(leftFront, leftBack, rightBack, rightFront);
        for (JMotor motor : driveMotors) {

            if (RUN_USING_ENCODER) {
                motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            }
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        if (RUN_USING_ENCODER && MOTOR_VELO_PID != null) {
            setPIDCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, MOTOR_VELO_PID);
        }
        grabServoRight = new JServo(mode.hardwareMap, "P.G.R");
        grabServoLeft = new JServo(mode.hardwareMap, "P.G.L");
        capStone = new JServo(mode.hardwareMap, "C");
        setLocalizer(new OdometryThreeWheel(opMode.hardwareMap));
        setPoseEstimate(new Pose2d(0, 0, 0));
        robot = Robot.getInstance();
    }

    @Override
    public void update() {
//        if (opMode.gamepad1.a) {
//            setPoseEstimate(new Pose2d(-robot.depositLift.getAbsExtend(), 0, Math.PI));
//        }

        //TODO MAKE PID FOR ROTATION

        capStone.setPosition((opMode.gamepad2.b ? 1 : 0.4));

        if (opMode.gamepad1.right_stick_button) {
            thirdPersonDrive = true;
            //TODO SET DIRECTION CALB
        } else if (opMode.gamepad1.left_stick_button) {
            thirdPersonDrive = false;
        }

        if (thirdPersonDrive) {
            updateMecanumFieldCentric(opMode.gamepad1, (opMode.gamepad1.right_bumper ? 0.25 : 1));
        } else {
            updateMecanum(opMode.gamepad1, (opMode.gamepad1.right_bumper ? 0.25 : 1));
        }

        setFoundationGrab(robot.stickyGamepad1.b ? FoundationGrabState.GRAB : FoundationGrabState.RELEASED);
        updatePoseEstimate();
        robot.telemetry.addData("POSE", getPoseEstimate());
        TelemetryPacket packet = new TelemetryPacket();
        Canvas fieldOverlay = packet.fieldOverlay();
        fieldOverlay.setStroke("#3F51B5");
        fieldOverlay.fillCircle(getPoseEstimate().getX(), getPoseEstimate().getY(), 3);
        dashboard.sendTelemetryPacket(packet);

    }

    public double angleToStone() {
        return Math.atan2(getPoseEstimate().getY(), getPoseEstimate().getX());
    }

    public void setFoundationGrab(FoundationGrabState state) {
        switch (state) {
            case GRAB:
                grabServoLeft.setPosition(0.2);
                grabServoRight.setPosition(0.6);
                break;
            case RELEASED:
                grabServoLeft.setPosition(0);
                grabServoRight.setPosition(0.8);
                break;
            case GRABSET:
                grabServoLeft.setPosition(0.6);//TODO FIND THESE POSES
                grabServoRight.setPosition(0.7);
                break;
        }

    }


    public void setMecanum(double angle, double speed, double rotation) {
        angle += 3 * Math.PI / 4;
        speed *= Math.sqrt(2);

        double motorPowers[] = new double[4];
        motorPowers[0] = (speed * sin(angle)) + rotation;
        motorPowers[1] = (speed * -cos(angle)) + rotation;
        motorPowers[2] = (speed * -sin(angle)) + rotation;
        motorPowers[3] = (speed * cos(angle)) + rotation;

        double max = Collections.max(Arrays.asList(1.0, Math.abs(motorPowers[0]),
                Math.abs(motorPowers[1]), Math.abs(motorPowers[2]), Math.abs(motorPowers[3])));
        if (max > 1.0) {
            for (int i = 0; i < 4; i++) {
                motorPowers[i] /= max;
            }
        }
        int i = 0;
        for (JMotor motor : driveMotors) {
            motor.setPower(motorPowers[i]);
            i++;
        }
    }

    public void updateMecanum(Gamepad gamepad, double scaling) {
        double angle = Math.atan2(gamepad.left_stick_x, gamepad.left_stick_y);
        double speed = Math.hypot(gamepad.left_stick_x, gamepad.left_stick_y) * scaling;
        double rotation = -gamepad.right_stick_x * scaling;

        speed = scalePower(speed);
        setMecanum(angle, speed, rotation);
    }


    public void updateMecanumFieldCentric(Gamepad gamepad, double scaling) {
        double angle = Math.atan2(gamepad.left_stick_x, gamepad.left_stick_y) + getPoseEstimate().getHeading();
        double speed = Math.hypot(gamepad.left_stick_x, gamepad.left_stick_y) * scaling;
        double rotation = -gamepad.right_stick_x * .8 * scaling;
        speed = scalePower(speed);
        setMecanum(angle, speed, rotation);
    }


    private static double scalePower(double speed) {
        return .5 * Math.pow(2 * (speed - .5), 3) + .5;
    }

    @NonNull
    @Override
    public List<Double> getWheelPositions() {
        RevBulkData bulkData = hub.getBulkInputData();

        if (bulkData == null) {
            return Arrays.asList(0.0, 0.0, 0.0, 0.0);
        }

        List<Double> wheelPositions = new ArrayList<>();
        for (JMotor motor : driveMotors) {
//            wheelPositions.add(encoderTicksToInches(motor.getCurrentPosition()));
            wheelPositions.add(encoderTicksToInches(bulkData.getMotorCurrentPosition(motor.motor)));
        }
        return wheelPositions;
    }

    @Override
    public void setMotorPowers(double v, double v1, double v2, double v3) {
        leftFront.setPower(-v);
        leftBack.setPower(-v1);
        rightBack.setPower(v2);
        rightFront.setPower(v3);
    }

    public void updateFollowingDrive() {
        setDriveSignal(follower.update(getPoseEstimate()));
    }

    public void setPIDCoefficients(DcMotor.RunMode runMode, PIDCoefficients coefficients) {
        for (JMotor motor : driveMotors) {
            motor.setPIDFCoefficients(runMode, new PIDFCoefficients(
                    coefficients.kP, coefficients.kI, coefficients.kD, 1
            ));
        }
    }

    @Override
    protected double getRawExternalHeading() {
        return 0;
    }


    public DriveConstraints getConstraints() {
        return constraints;
    }


    public void runUsingEncoder(boolean runwEnc) {
        for (JMotor motor : driveMotors) {
            if (runwEnc) {
                motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            } else {
                motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            }
        }
    }

    public void stopDriveMotors() {
        setMotorPowers(0, 0, 0, 0);
    }

    public double getDistanceToStone() {
        return Math.hypot(getPoseEstimate().getX(),getPoseEstimate().getY());
    }

    public double  getDistanceToStone(double distanceToStone) {
        return Math.hypot(getPoseEstimate().getX(),getPoseEstimate().getY());
    }


    public enum FoundationGrabState {
        GRAB, RELEASED, GRABSET
    }
}
