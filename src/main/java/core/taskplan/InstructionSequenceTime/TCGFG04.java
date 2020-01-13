package core.taskplan.InstructionSequenceTime;

public class TCGFG04 implements SequenceTime {
    @Override
    public String ExecutionTime(TimeVariable timeVariable, String TaskName) {
        if (TaskName == "TASK06") {
            double time=timeVariable.T0d;
            float f_time=(float) time;
            int i_time= (int) Math.ceil(f_time);

            return Integer.toHexString(i_time);
        }else {
            return null;
        }
    }
}
