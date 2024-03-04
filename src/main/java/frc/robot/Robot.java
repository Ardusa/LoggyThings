// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import com.ctre.phoenix6.controls.PositionDutyCycle;
import com.revrobotics.CANSparkLowLevel.MotorType;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import frc.LoggyThings.ILoggyMotor;
import frc.LoggyThings.LoggyCANSparkMax;
import frc.LoggyThings.LoggyTalonFX;
import frc.LoggyThings.LoggyThingManager;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the
 * name of this class or
 * the package after creating this project, you must also update the
 * build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {
  private LoggyTalonFX falcon = new LoggyTalonFX(1, "/ExampleSubsystem/falcon/",
      ILoggyMotor.LogItem.LOGLEVEL_EVERYTHING, false);
  private LoggyTalonFX falcon3 = new LoggyTalonFX(4, "/ExampleSubsystem/falcon3/",
      ILoggyMotor.LogItem.LOGLEVEL_EVERYTHING, false);

  private LoggyTalonFX falcon2 = new LoggyTalonFX(2, "/falcon2/", ILoggyMotor.LogItem.LOGLEVEL_MINIMAL, true);
  private LoggyCANSparkMax spark = new LoggyCANSparkMax(3, MotorType.kBrushless);

  @Override
  public void robotPeriodic() {
    LoggyThingManager.getInstance().periodic();
  }

  /** This function is called periodically during operator control. */
  @Override
  public void teleopPeriodic() {
    Joystick stick = new Joystick(0);
    if (stick.getX() > 0.1) {
      falcon.set(stick.getX());
    } else
      /*
       * Will run the motor in a clockwise motion because the time slowly increases
       * per each period
       */
      falcon.setControl(new PositionDutyCycle(Timer.getFPGATimestamp()));
    spark.set(stick.getY());
    falcon2.set(Math.sin(Timer.getFPGATimestamp()));
    falcon3.set(Math.cos(Timer.getFPGATimestamp()));
  }
}
