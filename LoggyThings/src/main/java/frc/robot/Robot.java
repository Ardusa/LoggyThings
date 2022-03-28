// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.LoggyThings.ILoggyMotor;
import frc.LoggyThings.LoggyCANSparkMax;
import frc.LoggyThings.LoggyThingManager;
import frc.LoggyThings.LoggyWPI_TalonFX;

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
  private LoggyWPI_TalonFX falcon = new LoggyWPI_TalonFX(1, "/ExampleSubsystem/falcon/",
      ILoggyMotor.LogItem.LOGLEVEL_EVERYTHING);
  private LoggyWPI_TalonFX falcon2 = new LoggyWPI_TalonFX(1, "/falcon2/", ILoggyMotor.LogItem.LOGLEVEL_MINIMAL);
  private LoggyCANSparkMax spark = new LoggyCANSparkMax(3, MotorType.kBrushless);

  @Override
  public void robotPeriodic() {
    LoggyThingManager.getInstance().periodic();
  }

  /** This function is called periodically during operator control. */
  @Override
  public void teleopPeriodic() {
    Joystick stick = new Joystick(0);
    if(stick.getX() > 0.1){
      falcon.set(ControlMode.PercentOutput, stick.getX());
    }else
      falcon.set(ControlMode.Position,Timer.getFPGATimestamp());
    spark.set(stick.getY());
    falcon2.set(Math.sin(Timer.getFPGATimestamp()));
  }
}
