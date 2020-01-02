package core.taskplan.InstructionSequenceTime;

public class TCA305_03 implements SequenceTime {
    @Override
    public String ExecutionTime(TimeVariable timeVariable, String TaskName) {
        if (TaskName == "TASK01" || TaskName == "TASK03") {
            double time=timeVariable.T1d;
            float f_time=(float) time;
            int i_time= (int) Math.ceil(f_time);

            return Integer.toHexString(i_time);
        }else if (TaskName == "TASK09") {
            double time=timeVariable.T1d;
            float f_time=(float) time;
            int i_time= (int) Math.ceil(f_time);

            return Integer.toHexString(i_time);
        }else {
            return null;
        }
    }
}
