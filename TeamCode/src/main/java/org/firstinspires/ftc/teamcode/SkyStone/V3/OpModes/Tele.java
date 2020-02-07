//V3

package org.firstinspires.ftc.teamcode.SkyStone.V3.OpModes;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.SkyStone.V3.Subsystems.Robot;

@TeleOp(name = "Tele")
public class Tele extends OpMode {
    Robot robot;
    ElapsedTime time;

    @Override
    public void init() {
        robot = new Robot(this);//constructs robot and gives access to opmode
        robot.mecanumDrive.runUsingEncoder(false);
        time = new ElapsedTime();
    }

    @Override
    public void loop() {
        telemetry.addData("TIME", time.milliseconds());
        robot.update();//updates all subsystems
    }

    @Override
    public void stop() {
        robot.opModeIsActive = false;
        super.stop();
    }
}