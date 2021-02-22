package core.taskplan.InstructionSequenceTime;

public class NTCY132 implements SequenceTime {
    @Override
    public String ExecutionTime(TimeVariable timeVariable, String TaskName) {
        if (TaskName == "TASK10") {
            double time=timeVariable.T0;
//            float f_time=(float) time;
            int i_time= (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }

        if (TaskName == "TASK05") {
            double time=timeVariable.T-30;
//            float f_time=(float) time;
            int i_time= (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }else {
            return null;
        }
    }
}
