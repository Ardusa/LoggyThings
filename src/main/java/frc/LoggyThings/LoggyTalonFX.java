package frc.LoggyThings;

import java.util.EnumSet;
import java.util.HashMap;

import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.configs.Slot1Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.ControlModeValue;
import com.ctre.phoenix6.signals.NeutralModeValue;

import edu.wpi.first.util.WPIUtilJNI;

/**
 * A {@link TalonFX} intizialized using {@link ILoggyMotor}
 */
public class LoggyTalonFX extends TalonFX implements ILoggyMotor {
    private EnumSet<ILoggyMotor.LogItem> mLogLevel = EnumSet.noneOf(ILoggyMotor.LogItem.class);
    private HashMap<LogItem, DataLogEntryWithHistory> mDataLogEntries = new HashMap<LogItem, DataLogEntryWithHistory>();
    private long mLogPeriod = 100000;
    private long lastLogTime = (long) Math.abs(Math.random() * 100000);
    private String mLogPath;

    /**
     * Constructs a new LoggyTalonFX and registers it with
     * {@link LoggyThingManager}
     * 
     * @param deviceNumber CAN id
     * @param canbus       canbus path String
     * @param logPath      String path of log file
     * @param logLevel     see {@link ILoggyMotor.LogItem}
     */
    public LoggyTalonFX(int deviceNumber, String logPath, String canbus, EnumSet<ILoggyMotor.LogItem> logLevel) {
        super(deviceNumber, canbus);// actually make motor controller
        mLogPath = logPath;
        setLogLevel(logLevel);
        LoggyThingManager.getInstance().registerLoggyMotor(this);
        super.stopMotor();
    }

    /**
     * Constructs a new LoggyTalonFX and registers it with
     * {@link LoggyThingManager}
     * 
     * @param deviceNumber CAN id
     * @param logPath      String path of log file
     * @param logLevel     see {@link ILoggyMotor.LogItem}
     */
    public LoggyTalonFX(int deviceNumber, String logPath, EnumSet<ILoggyMotor.LogItem> logLevel) {
        this(deviceNumber, logPath, "", logLevel);
    }

    /**
     * Constructs a new LoggyTalonFX and registers it with
     * {@link LoggyThingManager}
     * 
     * @param deviceNumber CAN id
     * @param logPath      String path of log file
     */
    public LoggyTalonFX(int deviceNumber, String logPath) {
        this(deviceNumber, logPath, "", ILoggyMotor.LogItem.LOGLEVEL_DEFAULT);
    }

    /**
     * Constructs a new LoggyTalonFX and registers it with
     * {@link LoggyThingManager}
     * 
     * @param deviceNumber CAN id
     */
    public LoggyTalonFX(int deviceNumber) {
        this(deviceNumber, "/loggyMotors/" + String.valueOf(deviceNumber) + "/", "",
                ILoggyMotor.LogItem.LOGLEVEL_DEFAULT);
    }

    @Override
    public String getLogPath() {
        return mLogPath;
    }

    @Override
    public void setMinimumLogPeriod(double logPeriodSeconds) {
        mLogPeriod = (long) (logPeriodSeconds * 1e6);
    }

    @Override
    public long getMinimumLogPeriod() {
        return mLogPeriod;
    }

    @Override
    public void setLogLevel_internal(EnumSet<LogItem> logLevel) {
        mLogLevel = logLevel;
    }

    @Override
    public EnumSet<LogItem> getLogLevel() {
        return mLogLevel;
    }

    @Override
    public HashMap<LogItem, DataLogEntryWithHistory> getDataLogEntries() {
        return mDataLogEntries;
    }

    @Override
    public void writeToLog() {
        // Slow down if commanded by manager
        long logPeriod = Long.max(LoggyThingManager.getInstance().getMinGlobalLogPeriod(), mLogPeriod);
        long now = WPIUtilJNI.now();
        if ((now - logPeriod) > lastLogTime) {

            // Only things allowed by the local log level are keys in the datalog entries
            EnumSet<LogItem> potentialLogItems = EnumSet.copyOf(mDataLogEntries.keySet());

            // Only things allowed by the global log level
            potentialLogItems.retainAll(LoggyThingManager.getInstance().getGlobalMaxLogLevel());
            potentialLogItems.removeAll(LogItem.SET_FUNCTION_CALLS);// a set function call, not a periodic status value
            if ((getControlMode().getValue() == ControlModeValue.DutyCycleOut)
                    || (getControlMode().getValue() == ControlModeValue.Follower)
                    || (getControlMode().getValue() == ControlModeValue.MusicTone)
                    || (getControlMode().getValue() == ControlModeValue.TorqueCurrentFOC)) {
                potentialLogItems.removeAll(LogItem.PID_LOG_ADDITIONS);
            }
            for (LogItem thisLogItem : potentialLogItems) {
                DataLogEntryWithHistory thisEntry = mDataLogEntries.get(thisLogItem);
                switch (thisLogItem) {
                    case OUTPUT_PERCENT:
                        thisEntry.logDoubleIfChanged(getDutyCycle().getValue(), now);
                        break;
                    case FORWARD_LIMIT_SWITCH:
                        thisEntry.logBooleanIfChanged(getForwardLimit().getValue().value == 0, now);
                        break;
                    case REVERSE_LIMIT_SWITCH:
                        thisEntry.logBooleanIfChanged(getReverseLimit().getValue().value == 0, now);
                        break;
                    case SELECTED_SENSOR_POSITION:
                        thisEntry.logDoubleIfChanged(getPosition().getValue(), now);
                        break;
                    case SELECTED_SENSOR_VELOCITY:
                        thisEntry.logDoubleIfChanged(getVelocity().getValue(), now);
                        break;
                    case STATOR_CURRENT:
                        thisEntry.logDoubleIfChanged(getStatorCurrent().getValue(), now);
                        break;
                    case SUPPLY_CURRENT:
                        thisEntry.logDoubleIfChanged(getSupplyCurrent().getValue(), now);
                        break;
                    case BUS_VOLTAGE:
                        thisEntry.logDoubleIfChanged(getSupplyVoltage().getValue(), now);
                        break;
                    case TEMPERATURE:
                        thisEntry.logDoubleIfChanged(getDeviceTemp().getValue(), now);
                        break;
                    // case INTEGRATED_SENSOR_ABSOLUTE_POSITION:
                    // thisEntry.logDoubleIfChanged(getSensorCollection().getIntegratedSensorAbsolutePosition(),
                    // now);
                    // // thisEntry.logDoubleIfChanged(getForwardLimit().getValue().value, now);
                    // break;
                    case HAS_RESET:
                        thisEntry.logBooleanIfChanged(hasResetOccurred(), now);
                        break;
                    case CLOSED_LOOP_ERROR:
                        thisEntry.logDoubleIfChanged(getClosedLoopError().getValue(), now);
                        break;
                    // case INTEGRAL_ACCUMULATOR:
                    // thisEntry.logDoubleIfChanged(getIntegralAccumulator(), now);
                    // break;
                    case ERROR_DERIVATIVE:
                        thisEntry.logDoubleIfChanged(getDifferentialClosedLoopDerivativeOutput().getValue(), now);
                        break;
                    case CLOSED_LOOP_TARGET:
                        thisEntry.logDoubleIfChanged(getClosedLoopReference().getValue(), now);
                        break;
                    case OUTPUT_VOLTAGE:
                        thisEntry.logDoubleIfChanged(getMotorVoltage().getValue(), now);
                        break;
                    case INTEGRATED_SENSOR_POSITION:
                        thisEntry.logDoubleIfChanged(getRotorPosition().getValue(), now);
                        break;
                    case INTEGRATED_SENSOR_VELOCITY:
                        thisEntry.logDoubleIfChanged(getRotorVelocity().getValue(), now);
                        break;
                    default:
                        break;
                }
            }
            lastLogTime = WPIUtilJNI.now();
        }
    }

    // set function calls
    // @Override
    // public void set(double speed) {
    // }

    boolean justFailed = false;

    /**
     * PercentOut replacement.
     * Just enter a speed (double) and the motor will use
     * {@link ControlModeValue.DutyCycleOut}
     */
    @Override
    public void set(double speed) {

        /* Set speed using DutyCycleOut */
        super.set(speed);

        try {// Don't jeopardize robot functionality
             // Filter the 4 potential log items down to the ones allowed here
            EnumSet<LogItem> potentialLogItems = EnumSet.of(LogItem.SET_FUNCTION_CONTROL_MODE);
            potentialLogItems.retainAll(mDataLogEntries.keySet());
            potentialLogItems.retainAll(LoggyThingManager.getInstance().getGlobalMaxLogLevel());
            long now = WPIUtilJNI.now();
            for (LogItem thisLogItem : potentialLogItems) {
                DataLogEntryWithHistory thisEntry = mDataLogEntries.get(thisLogItem);

                switch (thisLogItem) {
                    case SET_FUNCTION_CONTROL_MODE:
                        thisEntry.logStringIfChanged(getControlMode().toString(), now);
                        break;
                    // case SET_FUNCTION_VALUE:
                    // thisEntry.logDoubleIfChanged(, now);
                    // break;
                    // case SET_FUNCTION_DEMAND_TYPE:
                    // thisEntry.logStringIfChanged(.toString(), now);
                    // break;
                    // case SET_FUNCTION_DEMAND:
                    // thisEntry.logDoubleIfChanged(demand1, now);
                    default:
                        break;
                }
            }
            justFailed = false;
        } catch (Exception e) {
            if (!justFailed) {// don't spam log
                e.printStackTrace();
                justFailed = true;
            }
        }

    }

    /**
     * configureSlot 0
     * 
     * @param config TalonFXConfiguration
     * @return StatusCode
     */
    public StatusCode setMotionProfile(TalonFXConfiguration config) {
        // if (slot == 0) {
        // Slot0Configs configSlot = config.Slot0;
        // } else if (slot == 1) {
        // Slot1Configs configSlot = config.Slot1;
        // } else if (slot == 2) {
        // Slot2Configs configSlot = config.Slot2;
        // } else {
        // return StatusCode.InvalidTask;
        // SlotConfigs configSlot = new SlotConfigs();
        // }
        Slot1Configs configSlot = config.Slot1;
        configSlot.kS = 0.25;
        configSlot.kV = 0.12;
        configSlot.kA = 0.01;
        configSlot.kP = 4.8;
        configSlot.kI = 0;
        configSlot.kD = 0.01;
        config.MotionMagic.MotionMagicCruiseVelocity = 80; // Target cruise velocity of 80 rps
        config.MotionMagic.MotionMagicAcceleration = 160; // Target acceleration of 160 rps/s (0.5 seconds)
        config.MotionMagic.MotionMagicJerk = 1600; // Target jerk of 1600 rps/s/s (0.1 seconds)

        return super.getConfigurator().apply(config);
    }

    // public SlotConfigs getSlot(int slot) {
    // if (slot == 0) {
    // return super.().getSlot0();
    // } else if (slot == 1) {
    // return super.getConfigurator().getSlot1();
    // } else if (slot == 2) {
    // return super.getConfigurator().getSlot2();
    // } else if (slot == 3) {
    // return super.getConfigurator().getSlot3();
    // } else {
    // return null;
    // }
    // }

    // // @Override
    // public StatusCode startMotionProfile(BufferedTrajectoryPointStream stream,
    // int minBufferedPts, ControlModeValue motionProfControlMode){
    // // StatusCode rc = super.
    // StatusCode rc = super.startMotionProfile(stream, minBufferedPts,
    // motionProfControlMode);
    // try {// Don't jeopardize robot functionality

    // if(mDataLogEntries.keySet().contains(LogItem.SET_FUNCTION_CONTROL_MODE) &&
    // LoggyThingManager.getInstance().getGlobalMaxLogLevel().contains(LogItem.SET_FUNCTION_CONTROL_MODE)){
    // long now = WPIUtilJNI.now();
    // mDataLogEntries.get(LogItem.SET_FUNCTION_CONTROL_MODE).logStringIfChanged(motionProfControlMode.toString(),
    // now);
    // justFailed = false;
    // }
    // } catch (Exception e) {
    // if (!justFailed) {// don't spam log
    // e.printStackTrace();
    // justFailed = true;
    // }
    // }
    // return rc;
    // }

    @Override
    public void setVoltage(double outputVolts) {
        super.setVoltage(outputVolts);
        try {
            if (mDataLogEntries.keySet().contains(LogItem.SET_VOLTAGE)
                    && LoggyThingManager.getInstance().getGlobalMaxLogLevel().contains(LogItem.SET_VOLTAGE)) {
                mDataLogEntries.get(LogItem.SET_VOLTAGE).logDoubleIfChanged(outputVolts, WPIUtilJNI.now());
                justFailed = false;
            }
        } catch (Exception e) {
            if (!justFailed) {// don;t spam log
                e.printStackTrace();
                justFailed = true;
            }
        }
    }

    @Override
    public StatusCode setPosition(double sensorPos) {
        return setPosition(sensorPos, 0);
    }

    @Override
    public StatusCode setPosition(double newValue, double timeoutSeconds) {
        try {
            if (mDataLogEntries.keySet().contains(LogItem.SET_SELECTED_SENSOR_POSITION)
                    && LoggyThingManager.getInstance().getGlobalMaxLogLevel()
                            .contains(LogItem.SET_SELECTED_SENSOR_POSITION)) {
                mDataLogEntries.get(LogItem.SET_SELECTED_SENSOR_POSITION).logDoubleIfChanged(newValue,
                        WPIUtilJNI.now());
                justFailed = false;
            }
        } catch (Exception e) {
            if (!justFailed) {// don't spam log
                e.printStackTrace();
                justFailed = true;
            }
        }
        return super.setPosition(newValue, timeoutSeconds);
        // return super.setSelectedSensorPosition(sensorPos, pidIdx, timeoutMs);
    }

    @Override
    public void setNeutralMode(NeutralModeValue neutralMode) {
        try {
            if (mDataLogEntries.keySet().contains(LogItem.SET_NEUTRAL_MODE_IS_BRAKE)
                    && LoggyThingManager.getInstance().getGlobalMaxLogLevel()
                            .contains(LogItem.SET_NEUTRAL_MODE_IS_BRAKE)) {
                mDataLogEntries.get(LogItem.SET_NEUTRAL_MODE_IS_BRAKE)
                        .logBooleanIfChanged(neutralMode == NeutralModeValue.Brake, WPIUtilJNI.now());
                justFailed = false;
            }
        } catch (Exception e) {
            if (!justFailed) {// don't spam log
                e.printStackTrace();
                justFailed = true;
            }
        }
        super.setNeutralMode(neutralMode);
    }

    @Override
    public long getLastLogTime() {
        return lastLogTime;
    }
}
