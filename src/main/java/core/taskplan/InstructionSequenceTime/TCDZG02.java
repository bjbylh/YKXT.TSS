package core.taskplan.InstructionSequenceTime;

public class TCDZG02 implements SequenceTime {

    @Override
    public String ExecutionTime(TimeVariable timeVariable, String TaskName) {
        if (TaskName == "TASK01" || TaskName == "TASK03" || TaskName == "TASK09") {
            double time = timeVariable.T0d + 16;
            int i_time = (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        } else {
            return null;
        }
    }
}
