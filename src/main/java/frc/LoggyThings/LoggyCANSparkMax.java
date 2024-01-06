package frc.LoggyThings;

import java.util.EnumSet;
import java.util.HashMap;

import com.revrobotics.CANSparkMax;
import com.revrobotics.REVLibError;
import com.revrobotics.SparkMaxLimitSwitch.Type;

import edu.wpi.first.util.WPIUtilJNI;

/**
 * A {@link CANSparkMax} intizialized using {@link ILoggyMotor}
 */
public class LoggyCANSparkMax extends CANSparkMax implements ILoggyMotor {
    
    private EnumSet<ILoggyMotor.LogItem> mLogLevel = EnumSet.noneOf(ILoggyMotor.LogItem.class);
    private HashMap<LogItem, DataLogEntryWithHistory> mDataLogEntries = new HashMap<LogItem, DataLogEntryWithHistory>();
    private long mLogPeriod = 100000;// default to 100ms (unit is microseconds)
    private long lastLogTime = (long) Math.abs(Math.random() * 100000);
    private String mLogPath;

    /**
     * Constructs a new LoggyCANSparkMax and registers it with
     * {@link LoggyThingManager}
     * 
     * @param deviceNumber CAN id
     * @param motorType    brushed or brushless
     * @param logPath      String path of log file
     * @param logLevel     see {@link ILoggyMotor.LogItem}
     */
    public LoggyCANSparkMax(int deviceNumber, MotorType motorType, String logPath,
            EnumSet<ILoggyMotor.LogItem> logLevel) {
        super(deviceNumber, motorType);
        mLogPath = logPath;
        setLogLevel(logLevel);
        LoggyThingManager.getInstance().registerLoggyMotor(this);
    }

    /**
     * Constructs a new LoggyCANSparkMax and registers it with
     * {@link LoggyThingManager}
     * 
     * @param deviceNumber CAN id
     * @param motorType    brushed or brushless
     * @param logPath      String path of log file
     */
    public LoggyCANSparkMax(int deviceNumber, MotorType motorType, String logPath) {
        this(deviceNumber, motorType, logPath, ILoggyMotor.LogItem.LOGLEVEL_DEFAULT);
    }

    /**
     * Constructs a new LoggyCANSparkMax and registers it with
     * {@link LoggyThingManager}
     * 
     * @param deviceNumber CAN id
     * @param motorType    brushed or brushless
     */
    public LoggyCANSparkMax(int deviceNumber, MotorType motorType) {
        this(deviceNumber, motorType, "/loggyMotors/" + String.valueOf(deviceNumber) + "/",
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
        /* Slow down if commanded by manager */
        long logPeriod = Long.max(LoggyThingManager.getInstance().getMinGlobalLogPeriod(), mLogPeriod);
        long now = WPIUtilJNI.now();
        if ((now - logPeriod) > lastLogTime) {

            /* Only things allowed by the local log level are keys in the datalog entries */
            EnumSet<LogItem> potentialLogItems = EnumSet.copyOf(mDataLogEntries.keySet());

            // Only things allowed by the global log level
            potentialLogItems.retainAll(LoggyThingManager.getInstance().getGlobalMaxLogLevel());
            potentialLogItems.removeAll(LogItem.SET_FUNCTION_CALLS);

            for (LogItem thisLogItem : potentialLogItems) {
                DataLogEntryWithHistory thisEntry = mDataLogEntries.get(thisLogItem);
                switch (thisLogItem) {
                    case OUTPUT_PERCENT:
                        thisEntry.logDoubleIfChanged(getAppliedOutput(), now);
                        break;
                    case FORWARD_LIMIT_SWITCH:
                        thisEntry.logBooleanIfChanged(getForwardLimitSwitch(Type.kNormallyOpen).isPressed(), now);
                        break;
                    case REVERSE_LIMIT_SWITCH:
                        thisEntry.logBooleanIfChanged(getReverseLimitSwitch(Type.kNormallyOpen).isPressed(), now);
                        break;
                    case SELECTED_SENSOR_POSITION:
                        thisEntry.logDoubleIfChanged(getEncoder().getPosition(), now);
                        break;
                    case SELECTED_SENSOR_VELOCITY:
                        thisEntry.logDoubleIfChanged(getEncoder().getVelocity(), now);
                        break;
                    case STATOR_CURRENT:
                        thisEntry.logDoubleIfChanged(getOutputCurrent(), now);
                        break;
                    case SUPPLY_CURRENT:
                        // not supported
                        break;
                    case BUS_VOLTAGE:
                        thisEntry.logDoubleIfChanged(getBusVoltage(), now);
                        break;
                    case TEMPERATURE:
                        thisEntry.logDoubleIfChanged(getMotorTemperature(), now);
                        break;
                    case HAS_RESET:
                        /* not supported */
                        break;
                    case CLOSED_LOOP_ERROR:
                        break;
                    case CLOSED_LOOP_TARGET:
                        /* not supported */
                        break;
                    case OUTPUT_VOLTAGE:
                        /* not supported */
                        break;
                    case INTEGRATED_SENSOR_POSITION:
                        /* not supported */
                        break;
                    case INTEGRATED_SENSOR_VELOCITY:
                        /* not supported */
                        break;
                    default:
                        break;
                }
            }
            lastLogTime = WPIUtilJNI.now();
        }
    }

    // set function calls
    boolean justFailed = false;

    @Override
    public void set(double speed) {
        super.set(speed);
        try {// Don't jeopardize robot functionality
             // Filter the 4 potential log items down to the ones allowed here
            EnumSet<LogItem> potentialLogItems = EnumSet.of(LogItem.SET_FUNCTION_CONTROL_MODE,
                    LogItem.SET_FUNCTION_VALUE);
            potentialLogItems.retainAll(mDataLogEntries.keySet());
            potentialLogItems.retainAll(LoggyThingManager.getInstance().getGlobalMaxLogLevel());
            long now = WPIUtilJNI.now();
            for (LogItem thisLogItem : potentialLogItems) {
                DataLogEntryWithHistory thisEntry = mDataLogEntries.get(thisLogItem);
                switch (thisLogItem) {
                    case SET_FUNCTION_CONTROL_MODE:
                        thisEntry.logStringIfChanged("PercentOutput", now);
                        break;
                    case SET_FUNCTION_VALUE:
                        thisEntry.logDoubleIfChanged(speed, now);
                        break;
                    default:
                        break;
                }
            }
            justFailed = false;
        } catch (Exception e) {
            if (!justFailed) {
                e.printStackTrace();
                justFailed = true;
            }
        }

    }

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

    // TODO hook into setReference, set sensor position

    @Override
    public REVLibError setIdleMode(IdleMode mode) {
        try {
            if (mDataLogEntries.keySet().contains(LogItem.SET_NEUTRAL_MODE_IS_BRAKE)
                    && LoggyThingManager.getInstance().getGlobalMaxLogLevel()
                            .contains(LogItem.SET_NEUTRAL_MODE_IS_BRAKE)) {
                mDataLogEntries.get(LogItem.SET_NEUTRAL_MODE_IS_BRAKE)
                        .logBooleanIfChanged(mode == IdleMode.kBrake, WPIUtilJNI.now());
                justFailed = false;
            }
        } catch (Exception e) {
            if (!justFailed) {// don't spam log
                e.printStackTrace();
                justFailed = true;
            }
        }
        return super.setIdleMode(mode);
    }

    @Override
    public long getLastLogTime() {
        return lastLogTime;
    }
}