package frc.LoggyThings;

import edu.wpi.first.wpilibj.DataLogManager;
import edu.wpi.first.wpilibj.command.PrintCommand;

public class LoggyPrintCommand extends PrintCommand{
    String m_text;
    LoggyPrintCommand(String text){
        super(text);
        m_text=text;
    }
    @Override
    protected void initialize() {
        DataLogManager.log(m_text);
    }
}
