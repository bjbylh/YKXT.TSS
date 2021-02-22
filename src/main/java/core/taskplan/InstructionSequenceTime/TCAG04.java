package core.taskplan.InstructionSequenceTime;

public class TCAG04 implements SequenceTime {
    @Override
    public String ExecutionTime(TimeVariable timeVariable, String TaskName) {
        if (TaskName == "TASK04") {
            double time = timeVariable.T1-timeVariable.TTCAG04-5;//三个值取最小值
            int i_time = (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }

        if (TaskName == "TASK04") {
            double time=timeVariable.T1-timeVariable.TTCAG04-5;
            float f_time=(float) time;
            int i_time= (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }else {
            return null;
        }
    }
}
