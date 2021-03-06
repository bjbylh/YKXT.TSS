package core.taskplan.InstructionSequenceTime;

public class TCDGG01 implements SequenceTime {

    @Override
    public String ExecutionTime(TimeVariable timeVariable, String TaskName) {
        if (TaskName == "TASK01") {
            double time=timeVariable.T0-timeVariable.TDG1;
            float f_time=(float) time;
            int i_time= (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }

        if (TaskName == "TASK01" || TaskName == "TASK02") {
            double time=timeVariable.T0-timeVariable.TDG1;
            float f_time=(float) time;
            int i_time= (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }else {
            return null;
        }
    }
}
