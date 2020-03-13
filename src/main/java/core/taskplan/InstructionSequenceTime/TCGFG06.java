package core.taskplan.InstructionSequenceTime;

public class TCGFG06 implements SequenceTime {
    @Override
    public String ExecutionTime(TimeVariable timeVariable, String TaskName) {
        if (TaskName == "TASK09") {
            double time=timeVariable.T0d;
//            float ftime=(float) time;
            int i_time= (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }else {
            return null;
        }
    }
}
