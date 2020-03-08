package core.taskplan.InstructionSequenceTime;

public class TCGFG07_AB implements SequenceTime {
    @Override
    public String ExecutionTime(TimeVariable timeVariable, String TaskName) {
        if (TaskName == "TASK10") {
            double time=timeVariable.T0-8;
            float f_time=(float) time;
            int i_time= (int) Math.ceil(f_time);

            return Integer.toHexString(i_time);
        }else {
            return null;
        }
    }
}
