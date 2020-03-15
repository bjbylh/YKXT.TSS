package core.taskplan.InstructionSequenceTime;

public class TCDZG01 implements SequenceTime {

    @Override
    public String ExecutionTime(TimeVariable timeVariable, String TaskName) {
        if (TaskName == "TASK01" || TaskName == "TASK02") {
            double time1 = timeVariable.T0 - timeVariable.TGF;
            double time2 = timeVariable.T0 - timeVariable.TDG1 - timeVariable.TDG2;
            double time = time1 < time2 ? time1 : time2;//两个值取最小值
            time = time - 2;
            int i_time = (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        } else if (TaskName == "TASK10") {
            double time = timeVariable.T0 - 10;
//            float f_time = (float) time;
            int i_time = (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        } else {
            return null;
        }
    }
}
